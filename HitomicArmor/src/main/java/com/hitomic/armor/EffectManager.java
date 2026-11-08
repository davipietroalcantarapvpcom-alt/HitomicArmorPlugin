package com.hitomic.armor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calcula o bônus de cada jogador (armadura / asa / combo) e aplica UM único conjunto de efeitos.
 *
 * Regras:
 *  - Os efeitos do combo SUBSTITUEM os da armadura e da asa (nunca se somam nem duplicam).
 *  - O plugin só remove efeitos que ele mesmo aplicou (assinatura: tipo + nível + duração dentro do que ele aplica).
 *  - Se o jogador já tem um efeito externo mais forte do mesmo tipo, o plugin não interfere.
 *  - Se um efeito externo mais fraco do mesmo tipo é sobrescrito, ele é guardado e restaurado ao remover o nosso.
 */
public final class EffectManager {

    private static final long INFINITE = -1L;

    private static final class Shadow {
        final int amplifier;
        final boolean ambient;
        final boolean particles;
        final boolean icon;
        final long expiresAtMillis; // INFINITE = sem fim

        Shadow(PotionEffect effect) {
            this.amplifier = effect.getAmplifier();
            this.ambient = effect.isAmbient();
            this.particles = effect.hasParticles();
            this.icon = effect.hasIcon();
            this.expiresAtMillis = effect.getDuration() < 0 ? INFINITE : System.currentTimeMillis() + effect.getDuration() * 50L;
        }
    }

    private static final class PlayerState {
        final Map<PotionEffectType, Integer> applied = new HashMap<>();
        final Map<PotionEffectType, Shadow> shadows = new HashMap<>();
    }

    private final JavaPlugin plugin;
    private final ItemFactory items;
    private volatile Settings settings;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private BukkitTask task;

    public EffectManager(JavaPlugin plugin, ItemFactory items, Settings settings) {
        this.plugin = plugin;
        this.items = items;
        this.settings = settings;
    }

    // ------------------------------------------------------------------ ciclo de vida

    public void start() {
        stop();
        long period = settings.checkIntervalTicks();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Recarrega a configuração: limpa os efeitos aplicados com os níveis antigos e reinicia a verificação. */
    public void reload(Settings newSettings) {
        stop();
        clearAll();
        this.settings = newSettings;
        start();
        updateAll();
    }

    /** Remove (só os nossos) os efeitos de todos os jogadores. Usado ao desligar o plugin. */
    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        states.clear();
    }

    // ------------------------------------------------------------------ avaliação de equipamento

    public boolean hasFullArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        return items.is(inv.getHelmet(), HitomicItem.CAPACETE)
                && items.is(inv.getChestplate(), HitomicItem.PEITORAL)
                && items.is(inv.getLeggings(), HitomicItem.CALCAS)
                && items.is(inv.getBoots(), HitomicItem.BOTAS);
    }

    public boolean hasWingInOffhand(Player player) {
        return items.is(player.getInventory().getItemInOffHand(), HitomicItem.ASA);
    }

    public Tier tierOf(Player player) {
        boolean armor = hasFullArmor(player);
        boolean wing = hasWingInOffhand(player);
        if (armor && wing) {
            return Tier.COMBO;
        }
        if (armor) {
            return Tier.ARMOR;
        }
        if (wing) {
            return Tier.WING;
        }
        return Tier.NONE;
    }

    private Map<PotionEffectType, Integer> desiredEffects(Player player) {
        Tier tier = tierOf(player);
        Map<PotionEffectType, Integer> wanted = new LinkedHashMap<>();
        if (tier == Tier.NONE) {
            return wanted;
        }
        for (EffectKind kind : EffectKind.values()) {
            int level = settings.level(tier, kind);
            if (level > 0) {
                wanted.put(kind.type(), level - 1); // amplificador = nível - 1
            }
        }
        return wanted;
    }

    // ------------------------------------------------------------------ atualização

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
        // limpa estados de quem saiu
        states.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    /** Agenda uma verificação para o próximo tick (depois que o evento de inventário terminar). */
    public void scheduleUpdate(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                update(player);
            }
        });
    }

    public void scheduleUpdate(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                update(player);
            }
        }, delayTicks);
    }

    public void update(Player player) {
        if (!player.isOnline() || player.isDead()) {
            states.remove(player.getUniqueId());
            return;
        }
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
        Map<PotionEffectType, Integer> wanted = desiredEffects(player);

        // 1) remove o que não é mais desejado (ou mudou de nível)
        Iterator<Map.Entry<PotionEffectType, Integer>> it = state.applied.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<PotionEffectType, Integer> entry = it.next();
            Integer wantedAmp = wanted.get(entry.getKey());
            if (wantedAmp != null && wantedAmp.intValue() == entry.getValue().intValue()) {
                continue;
            }
            release(player, state, entry.getKey(), entry.getValue());
            it.remove();
        }

        // 2) aplica / renova
        for (Map.Entry<PotionEffectType, Integer> entry : wanted.entrySet()) {
            apply(player, state, entry.getKey(), entry.getValue());
        }

        if (state.applied.isEmpty() && state.shadows.isEmpty()) {
            states.remove(player.getUniqueId());
        }
    }

    private boolean looksLikeOurs(PotionEffect current, int amplifier) {
        return current != null
                && current.getAmplifier() == amplifier
                && current.getDuration() >= 0
                && current.getDuration() <= settings.durationTicks();
    }

    private void apply(Player player, PlayerState state, PotionEffectType type, int amplifier) {
        int duration = settings.durationTicks();
        PotionEffect current = player.getPotionEffect(type);
        Integer mine = state.applied.get(type);
        boolean ours = mine != null && mine.intValue() == amplifier && looksLikeOurs(current, amplifier);

        if (ours) {
            if (current.getDuration() > duration / 2) {
                return; // ainda tem bastante duração, nada a fazer (evita reaplicar sem necessidade)
            }
        } else if (current != null) {
            if (current.getAmplifier() > amplifier) {
                return; // efeito externo mais forte: não interferimos
            }
            if (current.getAmplifier() == amplifier && (current.getDuration() < 0 || current.getDuration() > duration)) {
                return; // efeito externo equivalente e mais longo
            }
            // efeito externo mais fraco (ou igual e curto) vai ser sobrescrito: guardamos para restaurar depois
            if (!state.shadows.containsKey(type)) {
                state.shadows.put(type, new Shadow(current));
            }
        }

        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
        state.applied.put(type, amplifier);
    }

    /** Remove o nosso efeito (somente se ainda for nosso) e restaura um eventual efeito externo que ele escondeu. */
    private void release(Player player, PlayerState state, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        Shadow shadow = state.shadows.remove(type);
        if (!looksLikeOurs(current, amplifier)) {
            return; // não é mais o nosso efeito (expirou, foi trocado por outro): não mexemos
        }
        player.removePotionEffect(type);
        if (shadow == null) {
            return;
        }
        int remaining;
        if (shadow.expiresAtMillis == INFINITE) {
            remaining = -1;
        } else {
            long ticks = (shadow.expiresAtMillis - System.currentTimeMillis()) / 50L;
            if (ticks <= 0) {
                return;
            }
            remaining = (int) Math.min(ticks, Integer.MAX_VALUE);
        }
        player.addPotionEffect(new PotionEffect(type, remaining, shadow.amplifier, shadow.ambient, shadow.particles, shadow.icon));
    }

    /** Remove tudo que o plugin aplicou neste jogador (saída do servidor, desligamento, recarga). */
    public void clear(Player player) {
        PlayerState state = states.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        for (Map.Entry<PotionEffectType, Integer> entry : state.applied.entrySet()) {
            release(player, state, entry.getKey(), entry.getValue());
        }
    }

    /** Esquece o estado sem tocar nos efeitos (morte: o jogo já limpa os efeitos). */
    public void forget(Player player) {
        states.remove(player.getUniqueId());
    }

    /** Resumo para depuração: o que o jogador tem agora. */
    public Map<EffectKind, Integer> activeLevels(Player player) {
        Map<EffectKind, Integer> result = new EnumMap<>(EffectKind.class);
        Tier tier = tierOf(player);
        for (EffectKind kind : EffectKind.values()) {
            int level = settings.level(tier, kind);
            if (level > 0) {
                result.put(kind, level);
            }
        }
        return result;
    }
}
