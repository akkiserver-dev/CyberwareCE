package flaxbeard.cyberware.client.handler;

import com.mojang.blaze3d.platform.InputConstants;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.Deconstructable;
import flaxbeard.cyberware.api.item.Quality;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Adds the cyberware details (info, requirements, slot and quality) to tooltips while sneak is held.
 */
public final class TooltipHandler {
    private TooltipHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (CyberwareAPI.isCyberware(stack)) {
            Cyberware cyberware = CyberwareAPI.getCyberware(stack);
            if (!isSneakDown()) {
                tooltip.add(Component.translatable("cyberware.tooltip.shift_prompt").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            tooltip.addAll(cyberware.getInfo(stack));

            List<List<ItemStack>> requirements = cyberware.required(stack);
            if (!requirements.isEmpty()) {
                MutableComponent joined = Component.empty();
                for (int i = 0; i < requirements.size(); i++) {
                    if (i != 0) {
                        joined.append(Component.translatable("cyberware.tooltip.joiner")).append(" ");
                    }
                    List<ItemStack> alternatives = requirements.get(i);
                    for (int j = 0; j < alternatives.size(); j++) {
                        if (j != 0) {
                            joined.append(" ").append(Component.translatable("cyberware.tooltip.joiner_or")).append(" ");
                        }
                        joined.append(alternatives.get(j).getHoverName());
                    }
                }
                tooltip.add(Component.translatable("cyberware.tooltip.requires").append(" ").append(joined).withStyle(ChatFormatting.AQUA));
            }
            tooltip.add(Component.translatable("cyberware.slot." + cyberware.getBodyRegion(stack).getName()).withStyle(ChatFormatting.RED));

            Quality quality = cyberware.getQuality(stack);
            if (quality != null) {
                tooltip.add(Component.translatable(quality.getTranslationKey()));
            }
        } else if (stack.getItem() instanceof Deconstructable) {
            Component line = Component.translatable("cyberware.tooltip.can_deconstruct").withStyle(ChatFormatting.DARK_GRAY);
            if (tooltip.size() > 1) {
                tooltip.add(1, line);
            } else {
                tooltip.add(line);
            }
        }
    }

    // key mappings are not updated while a screen is open, so the key itself is checked
    private static boolean isSneakDown() {
        Minecraft minecraft = Minecraft.getInstance();
        KeyMapping sneak = minecraft.options.keyShift;
        InputConstants.Key key = sneak.getKey();
        return key.getType() == InputConstants.Type.KEYSYM
                ? InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())
                : sneak.isDown();
    }
}
