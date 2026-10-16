package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Holds the experience of a player that died with a Cortical Stack.
 */
public class ExperienceCapsuleItem extends Item {
    public ExperienceCapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int experience = stack.getOrDefault(CyberwareDataComponents.EXPERIENCE, 0);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.giveExperiencePoints(experience);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int experience = stack.getOrDefault(CyberwareDataComponents.EXPERIENCE, 0);
        String before = Language.getInstance().getOrDefault("cyberware.tooltip.exp_capsule.before");
        String after = Language.getInstance().getOrDefault("cyberware.tooltip.exp_capsule.after");
        if (!before.isEmpty()) before += " ";
        if (!after.isEmpty()) after = " " + after;
        tooltip.add(Component.literal(before + experience + after).withStyle(ChatFormatting.RED));
    }
}
