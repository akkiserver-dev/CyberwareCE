package flaxbeard.cyberware.api.hud;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired on the client to collect every HUD element whose settings should be saved and loaded,
 * regardless of whether it is currently visible.
 */
public class CyberwareHudDataEvent extends Event {
    private final List<HudElement> elements = new ArrayList<>();

    public List<HudElement> getElements() {
        return elements;
    }

    public void addElement(HudElement element) {
        elements.add(element);
    }
}
