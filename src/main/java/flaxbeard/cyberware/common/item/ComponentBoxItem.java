package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.item.CyberwareTabItem;
import flaxbeard.cyberware.common.menu.ComponentBoxMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A component box can be opened from the hand; it is only placed while sneaking.
 */
public class ComponentBoxItem extends BlockItem implements CyberwareTabItem {
    public ComponentBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        open(player, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            open(player, context.getHand());
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return super.useOn(context);
    }

    private static void open(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) return;
        ItemStack stack = player.getItemInHand(hand);
        player.openMenu(new SimpleMenuProvider((id, inventory, menuPlayer) -> ComponentBoxMenu.forItem(id, inventory, hand),
                stack.getHoverName()), buffer -> {
            buffer.writeBoolean(true);
            buffer.writeEnum(hand);
        });
    }

    @Override
    public Category getCategory(ItemStack stack) {
        return Category.BLOCKS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cyberware.tooltip.component_box").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cyberware.tooltip.component_box2").withStyle(ChatFormatting.GRAY));
    }
}
