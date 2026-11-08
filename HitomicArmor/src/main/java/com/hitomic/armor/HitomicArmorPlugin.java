package com.hitomic.armor;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Plugin principal do HitomicArmor (Hitomic MC). */
public final class HitomicArmorPlugin extends JavaPlugin {

    private Settings settings;
    private ItemFactory items;
    private EffectManager effects;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(getConfig());
        this.items = new ItemFactory(this, settings);
        this.effects = new EffectManager(this, items, settings);

        getServer().getPluginManager().registerEvents(new EquipmentListener(effects), this);

        HitomicCommand handler = new HitomicCommand(this);
        register("armadura", handler);
        register("hitomic", handler);

        effects.start();
        getLogger().info("HitomicArmor ativado. Itens usam o resource pack (namespace '" + Settings.PACK_NAMESPACE + "').");
    }

    @Override
    public void onDisable() {
        if (effects != null) {
            effects.stop();
            effects.clearAll();
        }
    }

    private void register(String name, HitomicCommand handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Comando '" + name + "' não encontrado no plugin.yml!");
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    /** Recarrega config.yml e reaplica os efeitos com os novos valores. */
    public void reloadAll() {
        reloadConfig();
        this.settings = new Settings(getConfig());
        items.setSettings(settings);
        effects.reload(settings);
    }

    public Settings settings() {
        return settings;
    }

    public ItemFactory items() {
        return items;
    }

    public EffectManager effects() {
        return effects;
    }
}
