package flaxbeard.cyberware.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {
    public static final KeyMapping MENU = new KeyMapping("cyberware.keybinds.menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "cyberware.keybinds.category");

    private KeyBinds() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(MENU);
    }
}
