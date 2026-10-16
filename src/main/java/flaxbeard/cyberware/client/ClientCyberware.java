package flaxbeard.cyberware.client;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.network.HudColorPayload;
import flaxbeard.cyberware.common.network.TriggerActiveAbilityPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Client side helpers for the cyberware of the local player, mostly around the HUD color.
 */
public final class ClientCyberware {
    private static final float[] NO_COLOR = {0F, 0F, 0F};

    private ClientCyberware() {
    }

    @Nullable
    public static CyberwareUserData getLocalData() {
        return CyberwareAPI.getUserData(Minecraft.getInstance().player);
    }

    /**
     * Sets the HUD color used by the Hudjack, radial menu and other AR HUD elements.
     */
    public static void setHudColor(float[] color) {
        CyberwareUserData data = getLocalData();
        if (data != null) data.setHudColor(color);
    }

    public static void setHudColor(int color) {
        CyberwareUserData data = getLocalData();
        if (data != null) data.setHudColor(color);
    }

    public static int getHudColorHex() {
        CyberwareUserData data = getLocalData();
        return data == null ? 0 : data.getHudColorHex();
    }

    public static float[] getHudColor() {
        CyberwareUserData data = getLocalData();
        return data == null ? NO_COLOR : data.getHudColor();
    }

    /**
     * @return the HUD color as an opaque ARGB value
     */
    public static int getHudColorArgb() {
        return 0xFF000000 | getHudColorHex();
    }

    public static void syncHudColor() {
        PacketDistributor.sendToServer(new HudColorPayload(getHudColorHex()));
    }

    /**
     * Uses an active item locally and on the server.
     */
    public static void useActiveItem(Entity entity, ItemStack stack) {
        PacketDistributor.sendToServer(new TriggerActiveAbilityPayload(stack));
        CyberwareAPI.useActiveItem(entity, stack);
    }
}
