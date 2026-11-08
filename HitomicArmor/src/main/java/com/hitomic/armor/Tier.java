package com.hitomic.armor;

/** Situação de bônus em que o jogador se encontra. */
public enum Tier {
    NONE("none"),
    ARMOR("armor"),
    WING("wing"),
    COMBO("combo");

    private final String configKey;

    Tier(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
