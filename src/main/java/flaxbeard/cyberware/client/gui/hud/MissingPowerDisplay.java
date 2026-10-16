package flaxbeard.cyberware.client.gui.hud;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.hud.AbstractHudElement;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the cyberware that recently ran out of power, sliding away after a while.
 */
public class MissingPowerDisplay extends AbstractHudElement {
    private static final int EXAMPLE_COUNT = 8;

    public MissingPowerDisplay() {
        super("cyberware:missing_power");
        setDefaultX(-15);
        setDefaultY(35);
        setWidth(16 + 20);
        setHeight(18 * 8);
    }

    @Override
    public void renderElement(GuiGraphics graphics, int x, int y, Player player, int screenWidth, int screenHeight, boolean hudjackAvailable, boolean isConfigOpen, float partialTick) {
        if (isHidden() || !hudjackAvailable) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;

        boolean rightAnchored = getHorizontalAnchor() == AnchorHorizontal.RIGHT;
        float currentTime = player.tickCount + partialTick;
        List<ItemStack> outages = data.getPowerOutages();
        List<Integer> outageTimes = data.getPowerOutageTimes();
        if (isConfigOpen) {
            outages = new ArrayList<>();
            for (int index = 0; index < EXAMPLE_COUNT; index++) {
                outages.add(new ItemStack(CyberwareItems.CYBEREYES.get()));
            }
        }

        List<Integer> elapsed = new ArrayList<>();
        int itemX = x - 1 + (rightAnchored ? 0 : 20);
        int itemY = y;
        for (int index = outages.size() - 1; index >= 0; index--) {
            ItemStack stack = outages.get(index);
            if (stack.isEmpty()) continue;

            int time;
            if (isConfigOpen) {
                // animate the oldest example so the player sees how it looks
                time = index == 0 ? (int) (currentTime - 20 - player.tickCount % 40) : (int) currentTime;
            } else {
                time = outageTimes.get(index);
            }

            if (player.tickCount - time < 50) {
                double percentVisible = Math.max(0F, (currentTime - time - 20) / 30F);
                float xOffset = (float) (20F * Math.sin(percentVisible * Math.PI / 2F));
                graphics.pose().pushPose();
                graphics.pose().translate(rightAnchored ? xOffset : -xOffset, 0.0F, 0.0F);
                graphics.drawString(Minecraft.getInstance().font, "!", itemX + 14, itemY + 8, 0xFF0000);
                graphics.renderItem(stack, itemX, itemY);
                graphics.pose().popPose();
                itemY += 18;
            } else if (!isConfigOpen) {
                elapsed.add(index);
            }
        }

        // indexes are in descending order, so removing them one by one is safe
        for (int index : elapsed) {
            outages.remove(index);
            outageTimes.remove(index);
        }
    }
}
