package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * A blueprint stores the item it was made from; at the Engineering Table it crafts that item from its components.
 * A blueprint without a stored item is blank and can be turned into a blueprint by crafting it with an item.
 */
public class BlueprintItem extends Item implements Blueprint {
    public BlueprintItem(Properties properties) {
        super(properties);
    }

    public static ItemStack getBlueprintItem(ItemStack blueprint) {
        ItemContainerContents stored = blueprint.get(CyberwareDataComponents.BLUEPRINT_ITEM);
        return stored == null ? ItemStack.EMPTY : stored.copyOne();
    }

    public static boolean isBlank(ItemStack blueprint) {
        return getBlueprintItem(blueprint).isEmpty();
    }

    /**
     * @return a blueprint of the given item, or {@link ItemStack#EMPTY} if it can't be deconstructed
     */
    public static ItemStack getBlueprintForItem(ItemStack stack) {
        if (!CyberwareAPI.canDeconstruct(stack)) {
            return ItemStack.EMPTY;
        }
        // store a pristine copy: no damage, quality or other data
        ItemStack stored = new ItemStack(stack.getItem());
        ItemStack blueprint = new ItemStack(CyberwareItems.BLUEPRINT.get());
        blueprint.set(CyberwareDataComponents.BLUEPRINT_ITEM, ItemContainerContents.fromItems(List.of(stored)));
        return blueprint;
    }

    private static List<ItemStack> getRequirements(ItemStack blueprint) {
        ItemStack stored = getBlueprintItem(blueprint);
        if (!stored.isEmpty() && CyberwareAPI.canDeconstruct(stored)) {
            return CyberwareAPI.getComponents(stored);
        }
        return null;
    }

    private static boolean matches(ItemStack crafting, ItemStack required) {
        return !crafting.isEmpty()
                && ItemStack.isSameItem(crafting, required)
                && (required.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(required, crafting));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack stored = getBlueprintItem(stack);
        if (!stored.isEmpty()) {
            if (flag.hasShiftDown()) {
                List<ItemStack> requirements = getRequirements(stack);
                if (requirements != null) {
                    tooltip.add(Component.translatable("cyberware.tooltip.blueprint", stored.getHoverName()));
                    for (ItemStack requirement : requirements) {
                        if (!requirement.isEmpty()) {
                            tooltip.add(Component.literal(requirement.getCount() + " x ").append(requirement.getHoverName()));
                        }
                    }
                    return;
                }
            } else {
                tooltip.add(Component.translatable("cyberware.tooltip.shift_prompt").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
        }
        tooltip.add(Component.translatable("cyberware.tooltip.craft_blueprint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack stored = getBlueprintItem(stack);
        if (!stored.isEmpty()) {
            return Component.translatable("item.cyberware.blueprint.not_blank", stored.getHoverName());
        }
        return super.getName(stack);
    }

    @Override
    public ItemStack getResult(ItemStack stack, List<ItemStack> craftingItems) {
        List<ItemStack> requirements = getRequirements(stack);
        if (requirements == null) return ItemStack.EMPTY;

        for (ItemStack requirement : requirements) {
            int missing = requirement.getCount();
            for (ItemStack crafting : craftingItems) {
                if (matches(crafting, requirement)) {
                    missing -= crafting.getCount();
                }
                if (missing <= 0) break;
            }
            if (missing > 0) return ItemStack.EMPTY;
        }
        return getBlueprintItem(stack).copy();
    }

    @Override
    public List<ItemStack> consumeItems(ItemStack stack, List<ItemStack> craftingItems) {
        List<ItemStack> requirements = getRequirements(stack);
        if (requirements == null) {
            throw new IllegalStateException("Consuming items when items shouldn't be consumed!");
        }

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack crafting : craftingItems) {
            remaining.add(crafting.copy());
        }
        for (ItemStack requirement : requirements) {
            int missing = requirement.getCount();
            for (int index = 0; index < remaining.size() && missing > 0; index++) {
                ItemStack crafting = remaining.get(index);
                if (matches(crafting, requirement)) {
                    int toSubtract = Math.min(missing, crafting.getCount());
                    missing -= toSubtract;
                    crafting.shrink(toSubtract);
                    if (crafting.isEmpty()) {
                        remaining.set(index, ItemStack.EMPTY);
                    }
                }
            }
        }
        return remaining;
    }

    @Override
    public List<ItemStack> getRequirementsForDisplay(ItemStack stack) {
        List<ItemStack> requirements = getRequirements(stack);
        return requirements == null ? List.of() : requirements;
    }

    @Override
    public ItemStack getIconForDisplay(ItemStack stack) {
        return getBlueprintItem(stack);
    }
}
