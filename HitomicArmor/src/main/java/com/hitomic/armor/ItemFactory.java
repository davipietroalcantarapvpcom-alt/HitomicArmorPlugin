package com.hitomic.armor;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Cria e identifica os itens Hitomic. A identificação usa EXCLUSIVAMENTE o
 * PersistentDataContainer (chave hitomicarmor:item_id) — nunca o nome do item.
 */
public final class ItemFactory {

    private final NamespacedKey idKey;
    private volatile Settings settings;

    public ItemFactory(JavaPlugin plugin, Settings settings) {
        this.idKey = new NamespacedKey(plugin, "item_id");
        this.settings = settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /** Cria o item personalizado: base Netherite (atributos idênticos), item model do pack e id interno. */
    public ItemStack create(HitomicItem type) {
        Settings s = this.settings;
        ItemStack stack = new ItemStack(type.material(), 1);

        ItemMeta meta = stack.getItemMeta();
        meta.displayName(s.name(type));
        List<net.kyori.adventure.text.Component> lore = s.lore(type);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        // textura/modelo no resource pack: assets/hitomic/items/<id>.json
        meta.setItemModel(new NamespacedKey(Settings.PACK_NAMESPACE, type.id()));
        // identificador interno à prova de renomear na bigorna
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, type.id());
        stack.setItemMeta(meta);

        if (type.isArmor()) {
            // parte do equippable padrão da Netherite (som, slot...) e troca só o modelo vestido
            Equippable prototype = type.material().getDefaultData(DataComponentTypes.EQUIPPABLE);
            Equippable.Builder builder = prototype != null
                    ? prototype.toBuilder()
                    : Equippable.equippable(type.armorSlot());
            builder.assetId(Key.key(Settings.PACK_NAMESPACE, Settings.ARMOR_ASSET));
            stack.setData(DataComponentTypes.EQUIPPABLE, builder.build());
        }

        int durability = s.durability(type);
        if (durability > 0) {
            stack.setData(DataComponentTypes.MAX_DAMAGE, durability);
        }
        return stack;
    }

    /** Retorna o tipo Hitomic do item, ou null se não for um item Hitomic legítimo. */
    public HitomicItem identify(ItemStack stack) {
        if (stack == null || !HitomicItem.isBaseMaterial(stack.getType()) || !stack.hasItemMeta()) {
            return null;
        }
        String id = stack.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        HitomicItem type = HitomicItem.byId(id);
        if (type == null || type.material() != stack.getType()) {
            return null;
        }
        return type;
    }

    public boolean is(ItemStack stack, HitomicItem expected) {
        return identify(stack) == expected;
    }
}
