package flaxbeard.cyberware.client.integration.jei;

import flaxbeard.cyberware.common.recipe.Deconstruction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * An item that can be deconstructed and scanned, with all the components it can break into.
 */
public record DeconstructableItem(Item item, List<ItemStack> components) {
    /**
     * @return every deconstructable item known to the client, in registration order
     */
    public static List<DeconstructableItem> all() {
        return Deconstruction.getPreviews().entrySet().stream()
                .map(entry -> new DeconstructableItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(entry -> BuiltInRegistries.ITEM.getId(entry.item())))
                .toList();
    }

    public ItemStack stack() {
        return new ItemStack(item);
    }

    public ResourceLocation id(String prefix) {
        return BuiltInRegistries.ITEM.getKey(item).withPrefix(prefix);
    }
}
