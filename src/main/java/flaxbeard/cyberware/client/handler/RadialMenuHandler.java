package flaxbeard.cyberware.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.HotkeyHelper;
import flaxbeard.cyberware.client.ClientCyberware;
import flaxbeard.cyberware.client.KeyBinds;
import flaxbeard.cyberware.client.gui.RadialMenuScreen;
import flaxbeard.cyberware.common.network.OpenRadialMenuPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Opens the radial menu and triggers active items bound to hotkeys.
 */
public final class RadialMenuHandler {
    private static Set<Integer> lastPressed = new HashSet<>();

    private RadialMenuHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        CyberwareUserData data = CyberwareAPI.getUserData(minecraft.player);
        if (data == null) return;

        if (minecraft.screen == null) {
            Set<Integer> pressed = new HashSet<>();
            for (int key : data.getHotkeys()) {
                if (isPressed(data, key)) {
                    pressed.add(key);
                    if (!lastPressed.contains(key)) {
                        ClientCyberware.useActiveItem(minecraft.player, data.getHotkey(key));
                    }
                }
            }
            lastPressed = pressed;
        }

        while (KeyBinds.MENU.consumeClick()) {
            if (minecraft.screen == null && data.getNumActiveItems() > 0) {
                minecraft.setScreen(new RadialMenuScreen());
                data.setOpenedRadialMenu(true);
                PacketDistributor.sendToServer(new OpenRadialMenuPayload());
            }
        }
    }

    private static boolean isPressed(CyberwareUserData data, int key) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (HotkeyHelper.isMouseButton(key)) {
            return GLFW.glfwGetMouseButton(window, key - HotkeyHelper.MOUSE_OFFSET) == GLFW.GLFW_PRESS;
        }
        if (HotkeyHelper.isShifted(key)) {
            return InputConstants.isKeyDown(window, key - HotkeyHelper.SHIFT_OFFSET) && Screen.hasShiftDown();
        }
        // a key also bound with shift only triggers without shift
        if (!data.getHotkey(key + HotkeyHelper.SHIFT_OFFSET).isEmpty()) {
            return InputConstants.isKeyDown(window, key) && !Screen.hasShiftDown();
        }
        return InputConstants.isKeyDown(window, key);
    }
}
