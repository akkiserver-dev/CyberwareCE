package flaxbeard.cyberware.api.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Base HUD element handling position, anchors, visibility and persistence.
 */
public abstract class AbstractHudElement implements HudElement {
    private final String name;
    private int defaultX = 0;
    private int defaultY = 0;
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private boolean hidden = false;
    private AnchorHorizontal defaultHorizontalAnchor = AnchorHorizontal.LEFT;
    private AnchorVertical defaultVerticalAnchor = AnchorVertical.TOP;
    private AnchorHorizontal horizontalAnchor = AnchorHorizontal.LEFT;
    private AnchorVertical verticalAnchor = AnchorVertical.TOP;

    protected AbstractHudElement(String name) {
        this.name = name;
    }

    @Override
    public final void render(GuiGraphics graphics, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick) {
        int renderX = getX();
        int renderY = getY();
        if (getHorizontalAnchor() == AnchorHorizontal.RIGHT) {
            renderX = screenWidth - renderX - getWidth();
        }
        if (getVerticalAnchor() == AnchorVertical.BOTTOM) {
            renderY = screenHeight - renderY - getHeight();
        }
        renderElement(graphics, renderX, renderY, player, screenWidth, screenHeight, hudjackAvailable, isConfigOpen, partialTick);
    }

    /**
     * Renders the element at its resolved absolute position.
     */
    public abstract void renderElement(GuiGraphics graphics, int x, int y, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick);

    public void setDefaultX(int x) {
        defaultX = x;
        setX(x);
    }

    public void setDefaultY(int y) {
        defaultY = y;
        setY(y);
    }

    public void setDefaultHorizontalAnchor(AnchorHorizontal anchor) {
        defaultHorizontalAnchor = anchor;
        setHorizontalAnchor(anchor);
    }

    public void setDefaultVerticalAnchor(AnchorVertical anchor) {
        defaultVerticalAnchor = anchor;
        setVerticalAnchor(anchor);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean canHide() {
        return true;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public AnchorHorizontal getHorizontalAnchor() {
        return horizontalAnchor;
    }

    @Override
    public void setHorizontalAnchor(AnchorHorizontal anchor) {
        horizontalAnchor = anchor;
    }

    @Override
    public AnchorVertical getVerticalAnchor() {
        return verticalAnchor;
    }

    @Override
    public void setVerticalAnchor(AnchorVertical anchor) {
        verticalAnchor = anchor;
    }

    @Override
    public void reset() {
        x = defaultX;
        y = defaultY;
        horizontalAnchor = defaultHorizontalAnchor;
        verticalAnchor = defaultVerticalAnchor;
    }

    @Override
    public String getUniqueName() {
        return name;
    }

    @Override
    public void save(HudSaveData data) {
        data.setInteger("x", x);
        data.setInteger("y", y);
        data.setBoolean("top", verticalAnchor == AnchorVertical.TOP);
        data.setBoolean("left", horizontalAnchor == AnchorHorizontal.LEFT);
        data.setBoolean("hidden", hidden);
    }

    @Override
    public void load(HudSaveData data) {
        x = data.getInteger("x");
        y = data.getInteger("y");
        verticalAnchor = data.getBoolean("top") ? AnchorVertical.TOP : AnchorVertical.BOTTOM;
        horizontalAnchor = data.getBoolean("left") ? AnchorHorizontal.LEFT : AnchorHorizontal.RIGHT;
        hidden = data.getBoolean("hidden");
    }
}
