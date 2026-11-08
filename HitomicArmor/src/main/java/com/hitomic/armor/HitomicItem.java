package com.hitomic.armor;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Todos os itens personalizados do plugin. O "id" é o identificador interno (gravado no
 * PersistentDataContainer) e também o nome do item model no resource pack (hitomic:&lt;id&gt;).
 */
public enum HitomicItem {
    CAPACETE("capacete", Material.NETHERITE_HELMET, EquipmentSlot.HEAD),
    PEITORAL("peitoral", Material.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST),
    CALCAS("calcas", Material.NETHERITE_LEGGINGS, EquipmentSlot.LEGS),
    BOTAS("botas", Material.NETHERITE_BOOTS, EquipmentSlot.FEET),
    ASA("asa", Material.PAPER, null),
    ESPADA("espada", Material.NETHERITE_SWORD, null),
    PICARETA("picareta", Material.NETHERITE_PICKAXE, null),
    MACHADO("machado", Material.NETHERITE_AXE, null),
    PA("pa", Material.NETHERITE_SHOVEL, null),
    ENXADA("enxada", Material.NETHERITE_HOE, null),
    ARCO("arco", Material.BOW, null),
    ESCUDO("escudo", Material.SHIELD, null);

    private static final Set<Material> BASE_MATERIALS = EnumSet.noneOf(Material.class);

    static {
        for (HitomicItem item : values()) {
            BASE_MATERIALS.add(item.material);
        }
    }

    private final String id;
    private final Material material;
    private final EquipmentSlot armorSlot;

    HitomicItem(String id, Material material, EquipmentSlot armorSlot) {
        this.id = id;
        this.material = material;
        this.armorSlot = armorSlot;
    }

    public String id() {
        return id;
    }

    public Material material() {
        return material;
    }

    /** Slot de armadura, ou null se não for uma peça de armadura. */
    public EquipmentSlot armorSlot() {
        return armorSlot;
    }

    public boolean isArmor() {
        return armorSlot != null;
    }

    public static boolean isBaseMaterial(Material material) {
        return BASE_MATERIALS.contains(material);
    }

    public static HitomicItem byId(String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.toLowerCase(Locale.ROOT);
        for (HitomicItem item : values()) {
            if (item.id.equals(wanted)) {
                return item;
            }
        }
        return null;
    }
}
