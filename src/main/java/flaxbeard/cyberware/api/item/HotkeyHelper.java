package flaxbeard.cyberware.api.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import net.minecraft.world.item.ItemStack;

/**
 * Hotkeys are stored both on the cyberware stack (so they persist) and in the user data (for quick lookup).
 * <p>
 * Key codes are GLFW key codes; mouse buttons are encoded as {@code button + MOUSE_OFFSET} (negative values)
 * and keys pressed with shift as {@code key + SHIFT_OFFSET}.
 */
public final class HotkeyHelper {
    public static final String HOTKEY_KEY = "hotkey";
    public static final int NONE = -1;
    public static final int MOUSE_OFFSET = -100;
    public static final int SHIFT_OFFSET = 1000;

    private HotkeyHelper() {
    }

    public static void assignHotkey(CyberwareUserData data, ItemStack stack, int key) {
        removeHotkey(data, stack);
        data.addHotkey(key, stack);
        CyberwareAPI.updateFunctionData(stack, tag -> tag.putInt(HOTKEY_KEY, key));
    }

    public static void removeHotkey(CyberwareUserData data, int key) {
        removeHotkey(data, data.getHotkey(key));
    }

    public static void removeHotkey(CyberwareUserData data, ItemStack stack) {
        int hotkey = getHotkey(stack);
        if (hotkey != NONE) {
            data.removeHotkey(hotkey);
            CyberwareAPI.updateFunctionData(stack, tag -> tag.remove(HOTKEY_KEY));
        }
    }

    public static int getHotkey(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        var data = CyberwareAPI.getFunctionData(stack);
        return data.contains(HOTKEY_KEY) ? data.getInt(HOTKEY_KEY) : NONE;
    }

    public static boolean isMouseButton(int key) {
        return key < 0 && key != NONE;
    }

    public static boolean isShifted(int key) {
        return key >= SHIFT_OFFSET;
    }
}
