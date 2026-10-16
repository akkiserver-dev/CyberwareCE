package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.Quality;
import flaxbeard.cyberware.api.item.SidedLimb;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * A natural body part, such as human eyes or a human left arm.
 */
public class BodyPartItem extends CyberwareItem implements SidedLimb {
    @Nullable
    private final Side side;

    public BodyPartItem(Properties properties, Settings settings, @Nullable Side side) {
        super(properties, settings);
        this.side = side;
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return true;
    }

    @Override
    public int getEssenceCost(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        Cyberware otherWare = CyberwareAPI.getCyberware(other);
        if (side == null) {
            return otherWare.isEssential(other);
        }
        return otherWare instanceof SidedLimb sidedLimb
                && otherWare.isEssential(other)
                && sidedLimb.getSide(other) == side;
    }

    @Override
    public Category getCategory(ItemStack stack) {
        return Category.BODY_PARTS;
    }

    @Override
    public Side getSide(ItemStack stack) {
        // unsided body parts were treated as "left" in the original metadata scheme
        return side == null ? Side.LEFT : side;
    }

    @Override
    public Quality getQuality(ItemStack stack) {
        return null;
    }

    @Override
    public boolean canHoldQuality(ItemStack stack, Quality quality) {
        return false;
    }
}
