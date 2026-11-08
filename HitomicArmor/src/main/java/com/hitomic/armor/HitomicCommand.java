package com.hitomic.armor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Implementa /armadura e /hitomic. Todos exigem a permissão hitomicarmor.admin (compatível com LuckPerms). */
public final class HitomicCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "hitomicarmor.admin";

    private static final List<HitomicItem> ARMOR = List.of(
            HitomicItem.CAPACETE, HitomicItem.PEITORAL, HitomicItem.CALCAS, HitomicItem.BOTAS);
    private static final List<HitomicItem> TOOLS = List.of(
            HitomicItem.PICARETA, HitomicItem.MACHADO, HitomicItem.PA, HitomicItem.ENXADA);
    private static final List<HitomicItem> KIT = List.of(
            HitomicItem.CAPACETE, HitomicItem.PEITORAL, HitomicItem.CALCAS, HitomicItem.BOTAS,
            HitomicItem.ASA, HitomicItem.ESPADA, HitomicItem.PICARETA, HitomicItem.MACHADO,
            HitomicItem.PA, HitomicItem.ENXADA, HitomicItem.ARCO, HitomicItem.ESCUDO);

    private final HitomicArmorPlugin plugin;

    public HitomicCommand(HitomicArmorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings settings = plugin.settings();
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(settings.message("no-permission"));
            return true;
        }

        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("armadura")) {
            if (args.length < 1 || !args[0].equalsIgnoreCase("hitomic")) {
                sender.sendMessage(settings.message("usage-armadura"));
                return true;
            }
            give(sender, args.length >= 2 ? args[1] : null, ARMOR, settings.rawMessage("group-armor"));
            return true;
        }

        // /hitomic <sub> [jogador]
        if (args.length < 1) {
            sender.sendMessage(settings.message("usage-hitomic"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String target = args.length >= 2 ? args[1] : null;
        switch (sub) {
            case "espada" -> give(sender, target, List.of(HitomicItem.ESPADA), null);
            case "ferramentas" -> give(sender, target, TOOLS, settings.rawMessage("group-tools"));
            case "arco" -> give(sender, target, List.of(HitomicItem.ARCO), null);
            case "asa" -> give(sender, target, List.of(HitomicItem.ASA), null);
            case "escudo" -> give(sender, target, List.of(HitomicItem.ESCUDO), null);
            case "armadura" -> give(sender, target, ARMOR, settings.rawMessage("group-armor"));
            case "kit" -> give(sender, target, KIT, settings.rawMessage("group-kit"));
            case "recarregar", "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(plugin.settings().message("reloaded"));
            }
            default -> sender.sendMessage(settings.message("usage-hitomic"));
        }
        return true;
    }

    private void give(CommandSender sender, String targetName, List<HitomicItem> types, String groupLabel) {
        Settings settings = plugin.settings();
        Player target;
        if (targetName == null) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(settings.message("player-only"));
                return;
            }
            target = self;
        } else {
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage(settings.message("player-not-found"));
                return;
            }
        }

        boolean dropped = false;
        for (HitomicItem type : types) {
            ItemStack stack = plugin.items().create(type);
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(stack);
            for (ItemStack rest : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), rest);
                dropped = true;
            }
        }

        String label = groupLabel != null ? groupLabel : settings.plainName(types.get(0));
        if (target.equals(sender)) {
            target.sendMessage(settings.message("given", "%item%", label));
        } else {
            target.sendMessage(settings.message("given", "%item%", label));
            sender.sendMessage(settings.message("given-other", "%player%", target.getName(), "%item%", label));
        }
        if (dropped) {
            target.sendMessage(settings.message("inventory-full"));
        }
        plugin.effects().scheduleUpdate(target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(PERMISSION)) {
            return out;
        }
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            List<String> options = name.equals("armadura")
                    ? List.of("hitomic")
                    : List.of("espada", "ferramentas", "arco", "asa", "escudo", "armadura", "kit", "recarregar");
            for (String option : options) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(option);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("recarregar") && !args[0].equalsIgnoreCase("reload")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(player.getName());
                }
            }
        }
        return out;
    }
}
