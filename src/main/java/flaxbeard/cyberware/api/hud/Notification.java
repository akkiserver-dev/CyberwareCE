package flaxbeard.cyberware.api.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A small icon shown in the HUD notification bar.
 */
public interface Notification {
    void render(GuiGraphics graphics, int x, int y);

    /**
     * @return how many ticks the notification stays visible before sliding away
     */
    int getDuration();
}
