package flaxbeard.cyberware.client.render;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * How the body of a player looks, derived from their cyberware.
 */
public record PlayerCyberwareState(
        boolean hasLeftArm, boolean hasRightArm, boolean hasLeftLeg, boolean hasRightLeg,
        LimbLook leftArm, LimbLook rightArm, LimbLook leftLeg, LimbLook rightLeg,
        boolean hasSkin) {

    public enum LimbLook {
        NORMAL,
        ROBOT,
        RUSTY
    }

    @Nullable
    public static PlayerCyberwareState of(Player player) {
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return null;
        boolean syntheticSkin = data.isCyberwareInstalled(CyberwareItems.SYNTHETIC_SKIN.get());
        return new PlayerCyberwareState(
                data.hasEssential(BodyRegion.ARM, SidedLimb.Side.LEFT),
                data.hasEssential(BodyRegion.ARM, SidedLimb.Side.RIGHT),
                data.hasEssential(BodyRegion.LEG, SidedLimb.Side.LEFT),
                data.hasEssential(BodyRegion.LEG, SidedLimb.Side.RIGHT),
                look(data, CyberwareItems.CYBERARM_LEFT.get(), syntheticSkin),
                look(data, CyberwareItems.CYBERARM_RIGHT.get(), syntheticSkin),
                look(data, CyberwareItems.CYBERLEG_LEFT.get(), syntheticSkin),
                look(data, CyberwareItems.CYBERLEG_RIGHT.get(), syntheticSkin),
                data.hasEssential(BodyRegion.SKIN));
    }

    private static LimbLook look(CyberwareUserData data, Item limb, boolean syntheticSkin) {
        ItemStack installed = data.getCyberware(limb);
        if (installed.isEmpty() || syntheticSkin) return LimbLook.NORMAL;
        return CyberwareAPI.getCyberware(installed).getQuality(installed) == CyberwareAPI.QUALITY_SCAVENGED ? LimbLook.RUSTY : LimbLook.ROBOT;
    }

    public boolean hasNoLegs() {
        return !hasLeftLeg && !hasRightLeg;
    }

    /**
     * @return whether the default player rendering has to be changed at all
     */
    public boolean isModified() {
        return !hasLeftArm || !hasRightArm || !hasLeftLeg || !hasRightLeg || !hasSkin
                || leftArm != LimbLook.NORMAL || rightArm != LimbLook.NORMAL || leftLeg != LimbLook.NORMAL || rightLeg != LimbLook.NORMAL;
    }
}
