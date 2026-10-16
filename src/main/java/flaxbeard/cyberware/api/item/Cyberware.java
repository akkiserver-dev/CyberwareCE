package flaxbeard.cyberware.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Something that can be installed into a body by the Robosurgeon. Usually implemented by an
 * {@link net.minecraft.world.item.Item}, but can also be linked to foreign items through
 * {@link flaxbeard.cyberware.api.CyberwareAPI#linkCyberware}.
 */
public interface Cyberware {
    BodyRegion getBodyRegion(ItemStack stack);

    /**
     * @return how many of this item can be installed in one slot
     */
    int installedStackSize(ItemStack stack);

    /**
     * @return a list of requirements; each inner list is a set of alternatives of which one must be installed
     */
    List<List<ItemStack>> required(ItemStack stack);

    boolean isIncompatible(ItemStack stack, ItemStack other);

    boolean isEssential(ItemStack stack);

    /**
     * @return the extra tooltip lines shown while sneaking
     */
    List<Component> getInfo(ItemStack stack);

    int getCapacity(ItemStack stack);

    /**
     * Returns the Quality of this stack. All functional changes a Quality brings must be handled
     * internally, this is only for the tooltip and external factors.
     *
     * @see flaxbeard.cyberware.api.CyberwareAPI#QUALITY_SCAVENGED
     */
    Quality getQuality(ItemStack stack);

    ItemStack setQuality(ItemStack stack, Quality quality);

    boolean canHoldQuality(ItemStack stack, Quality quality);

    void onAdded(LivingEntity entity, ItemStack stack);

    void onRemoved(LivingEntity entity, ItemStack stack);

    int getEssenceCost(ItemStack stack);
}
