package com.hitomic.armor;

import org.bukkit.potion.PotionEffectType;

/** Efeitos gerenciados pelo plugin (somente estes são aplicados/removidos por ele). */
public enum EffectKind {
    STRENGTH(PotionEffectType.STRENGTH, "strength"),
    SPEED(PotionEffectType.SPEED, "speed"),
    REGENERATION(PotionEffectType.REGENERATION, "regeneration");

    private final PotionEffectType type;
    private final String configKey;

    EffectKind(PotionEffectType type, String configKey) {
        this.type = type;
        this.configKey = configKey;
    }

    public PotionEffectType type() {
        return type;
    }

    public String configKey() {
        return configKey;
    }
}
