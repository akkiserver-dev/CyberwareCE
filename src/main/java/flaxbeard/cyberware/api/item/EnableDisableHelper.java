package flaxbeard.cyberware.api.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.world.item.ItemStack;

/**
 * Stores an on/off toggle in the function data of a piece of cyberware. Cyberware is enabled by default.
 */
public final class EnableDisableHelper {
    public static final String ENABLED_KEY = "~enabled";

    private static final float[] ENABLED_COLOR = {1.0F, 0.0F, 0.0F};

    private EnableDisableHelper() {
    }

    public static boolean isEnabled(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var data = CyberwareAPI.getFunctionData(stack);
        return !data.contains(ENABLED_KEY) || data.getBoolean(ENABLED_KEY);
    }

    public static void toggle(ItemStack stack) {
        boolean enabled = isEnabled(stack);
        CyberwareAPI.updateFunctionData(stack, data -> {
            if (enabled) {
                data.putBoolean(ENABLED_KEY, false);
            } else {
                data.remove(ENABLED_KEY);
            }
        });
    }

    public static String getMenuLabel(ItemStack stack) {
        return isEnabled(stack) ? "cyberware.gui.active.disable" : "cyberware.gui.active.enable";
    }

    /**
     * @return the menu color used by toggleable cyberware: red while enabled, the HUD color otherwise
     */
    public static float[] getMenuColor(ItemStack stack) {
        return isEnabled(stack) ? ENABLED_COLOR : null;
    }
}
