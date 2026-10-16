package flaxbeard.cyberware.api.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Cyberware with an action in the radial cyberware menu.
 */
public interface MenuItem {
    boolean hasMenu(ItemStack stack);

    void use(Entity entity, ItemStack stack);

    String getMenuLabel(ItemStack stack);

    /**
     * @return an RGB color for the menu segment, or {@code null} to use the HUD color
     */
    @Nullable
    float[] getColor(ItemStack stack);
}
