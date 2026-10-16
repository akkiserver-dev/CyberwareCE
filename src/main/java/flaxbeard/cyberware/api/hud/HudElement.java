package flaxbeard.cyberware.api.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * An element of the cyberware HUD that can be moved, hidden and anchored in the HUD configuration screen.
 */
public interface HudElement {
    enum AnchorHorizontal {
        LEFT,
        RIGHT
    }

    enum AnchorVertical {
        TOP,
        BOTTOM
    }

    void render(GuiGraphics graphics, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick);

    boolean canMove();

    void setX(int x);

    void setY(int y);

    int getX();

    int getY();

    int getWidth();

    int getHeight();

    boolean canHide();

    void setHidden(boolean hidden);

    boolean isHidden();

    AnchorHorizontal getHorizontalAnchor();

    void setHorizontalAnchor(AnchorHorizontal anchor);

    AnchorVertical getVerticalAnchor();

    void setVerticalAnchor(AnchorVertical anchor);

    void reset();

    String getUniqueName();

    void save(HudSaveData data);

    void load(HudSaveData data);
}
