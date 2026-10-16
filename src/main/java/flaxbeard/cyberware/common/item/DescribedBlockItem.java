package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.item.CyberwareTabItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A block item with fixed gray tooltip lines, listed in the blocks section of the creative tab.
 */
public class DescribedBlockItem extends BlockItem implements CyberwareTabItem {
    private final String[] tooltipKeys;

    public DescribedBlockItem(Block block, Properties properties, String... tooltipKeys) {
        super(block, properties);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public Category getCategory(ItemStack stack) {
        return Category.BLOCKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String key : tooltipKeys) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }
}
