package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.recipe.EngineeringRecipe;
import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Optional;

/**
 * A blueprint stores the item it was made from; at the Engineering Table it crafts that item from its components,
 * see {@link EngineeringRecipe}.
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
        return CyberwareAPI.canDeconstruct(stack) ? createBlueprint(stack.getItem()) : ItemStack.EMPTY;
    }

    /**
     * @return a blueprint of the given item, without checking whether it can be deconstructed
     */
    public static ItemStack createBlueprint(ItemLike item) {
        // store a pristine copy: no damage, quality or other data
        ItemStack stored = new ItemStack(item);
        ItemStack blueprint = new ItemStack(CyberwareItems.BLUEPRINT.get());
        blueprint.set(CyberwareDataComponents.BLUEPRINT_ITEM, ItemContainerContents.fromItems(List.of(stored)));
        return blueprint;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack stored = getBlueprintItem(stack);
        if (!stored.isEmpty()) {
            if (flag.hasShiftDown()) {
                Level level = context.level();
                Optional<EngineeringRecipe> recipe = level == null ? Optional.empty() : EngineeringRecipe.findFor(level.getRecipeManager(), stored);
                if (recipe.isPresent()) {
                    tooltip.add(Component.translatable("cyberware.tooltip.blueprint", stored.getHoverName()));
                    for (SizedIngredient ingredient : recipe.get().ingredients()) {
                        ItemStack[] items = ingredient.ingredient().getItems();
                        if (items.length > 0) {
                            tooltip.add(Component.literal(ingredient.count() + " x ").append(items[0].getHoverName()));
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
    public ItemStack getTarget(ItemStack blueprint) {
        return getBlueprintItem(blueprint);
    }
}
