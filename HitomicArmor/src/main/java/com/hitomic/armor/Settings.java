package com.hitomic.armor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Leitura (e cache) do config.yml. */
public final class Settings {

    /** Namespace do resource pack (assets/hitomic/...). */
    public static final String PACK_NAMESPACE = "hitomic";
    /** Nome do asset de equipamento vestido (assets/hitomic/equipment/hitomic.json). */
    public static final String ARMOR_ASSET = "hitomic";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final FileConfiguration config;
    private final Map<HitomicItem, Component> names = new EnumMap<>(HitomicItem.class);
    private final Map<HitomicItem, List<Component>> lores = new EnumMap<>(HitomicItem.class);
    private final Map<HitomicItem, Integer> durabilities = new EnumMap<>(HitomicItem.class);
    private final Map<Tier, Map<EffectKind, Integer>> levels = new EnumMap<>(Tier.class);
    private final int checkIntervalTicks;
    private final int durationTicks;

    public Settings(FileConfiguration config) {
        this.config = config;

        for (HitomicItem item : HitomicItem.values()) {
            String base = "items." + item.id();
            String rawName = config.getString(base + ".name", "&5&l" + item.id());
            names.put(item, noItalic(LEGACY.deserialize(rawName)));
            List<Component> lore = new ArrayList<>();
            for (String line : config.getStringList(base + ".lore")) {
                lore.add(noItalic(LEGACY.deserialize(line)));
            }
            lores.put(item, lore);
            durabilities.put(item, Math.max(0, config.getInt(base + ".durability", 0)));
        }

        for (Tier tier : Tier.values()) {
            Map<EffectKind, Integer> map = new EnumMap<>(EffectKind.class);
            for (EffectKind kind : EffectKind.values()) {
                int level = tier == Tier.NONE ? 0 : config.getInt("effects.levels." + tier.configKey() + "." + kind.configKey(), 0);
                map.put(kind, Math.max(0, Math.min(level, 255)));
            }
            levels.put(tier, map);
        }

        this.checkIntervalTicks = Math.max(1, config.getInt("effects.check-interval-ticks", 10));
        // a duração precisa ser maior que o intervalo de verificação, senão o efeito piscaria
        this.durationTicks = Math.max(this.checkIntervalTicks * 4, config.getInt("effects.duration-ticks", 400));
    }

    private static Component noItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public Component name(HitomicItem item) {
        return names.get(item);
    }

    public List<Component> lore(HitomicItem item) {
        return lores.get(item);
    }

    public int durability(HitomicItem item) {
        return durabilities.get(item);
    }

    /** Nível (1-based) do efeito na situação; 0 = não aplicar. */
    public int level(Tier tier, EffectKind kind) {
        return levels.get(tier).get(kind);
    }

    public int checkIntervalTicks() {
        return checkIntervalTicks;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public String rawMessage(String key) {
        return config.getString("messages." + key, "");
    }

    public Component message(String key) {
        return LEGACY.deserialize(config.getString("messages.prefix", "") + rawMessage(key));
    }

    public Component message(String key, String placeholder, String value) {
        String text = config.getString("messages.prefix", "") + rawMessage(key).replace(placeholder, value);
        return LEGACY.deserialize(text);
    }

    public Component message(String key, String p1, String v1, String p2, String v2) {
        String text = config.getString("messages.prefix", "") + rawMessage(key).replace(p1, v1).replace(p2, v2);
        return LEGACY.deserialize(text);
    }

    /** Texto simples (sem cor) de um item, para mensagens. */
    public String plainName(HitomicItem item) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(names.get(item));
    }
}
