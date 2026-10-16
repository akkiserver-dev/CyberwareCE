package flaxbeard.cyberware.common.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public final class CyberwareTiers {
    /** Iron harvest level, diamond durability, speed and damage, gold enchantability, repaired with plating. */
    public static final Tier KATANA = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL,
            Tiers.DIAMOND.getUses(), Tiers.DIAMOND.getSpeed(), Tiers.DIAMOND.getAttackDamageBonus(), Tiers.GOLD.getEnchantmentValue(),
            () -> Ingredient.of(CyberwareItems.PLATING.get()));

    private CyberwareTiers() {
    }
}
