package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public final class CyberwareArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CyberwareMod.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SHADES = register("shades",
            SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(Items.GLASS), new ArmorMaterial.Layer(CyberwareAPI.id("vanity")));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SHADES2 = register("shades2",
            SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(Items.GLASS), new ArmorMaterial.Layer(CyberwareAPI.id("vanity_2")));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JACKET = register("jacket",
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER), new ArmorMaterial.Layer(CyberwareAPI.id("jacket")));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRENCH_COAT = register("trenchcoat",
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
            new ArmorMaterial.Layer(CyberwareAPI.id("trenchcoat"), "", true),
            new ArmorMaterial.Layer(CyberwareAPI.id("trenchcoat"), "_overlay", false));

    private CyberwareArmorMaterials() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> register(String name, Holder<SoundEvent> sound, Supplier<Ingredient> repair, ArmorMaterial.Layer... layers) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                    defense.put(ArmorItem.Type.BOOTS, 1);
                    defense.put(ArmorItem.Type.LEGGINGS, 2);
                    defense.put(ArmorItem.Type.CHESTPLATE, 3);
                    defense.put(ArmorItem.Type.HELMET, 1);
                    defense.put(ArmorItem.Type.BODY, 3);
                }),
                15, sound, repair, List.of(layers), 0.0F, 0.0F));
    }
}
