package flaxbeard.cyberware.api.hud;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired on the client to collect the HUD elements to render this frame.
 */
public class CyberwareHudEvent extends Event {
    private final List<HudElement> elements = new ArrayList<>();
    private final int screenWidth;
    private final int screenHeight;
    private boolean hudjackAvailable;

    public CyberwareHudEvent(int screenWidth, int screenHeight, boolean hudjackAvailable) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.hudjackAvailable = hudjackAvailable;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean isHudjackAvailable() {
        return hudjackAvailable;
    }

    public void setHudjackAvailable(boolean hudjackAvailable) {
        this.hudjackAvailable = hudjackAvailable;
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public void addElement(HudElement element) {
        elements.add(element);
    }
}
