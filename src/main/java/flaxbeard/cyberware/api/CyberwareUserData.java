package flaxbeard.cyberware.api;

import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.SidedLimb;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The cyberware installed in an entity, along with its power grid, tolerance (essence) and HUD settings.
 * Obtain it with {@link CyberwareAPI#getUserData}.
 */
public interface CyberwareUserData {
    // ---- installed cyberware ----

    NonNullList<ItemStack> getInstalledCyberware(BodyRegion region);

    /**
     * Replaces the installed cyberware of a region, calling {@code onAdded}/{@code onRemoved} for changed items
     * when {@code entity} isn't null. The list is padded or trimmed to the slot count.
     */
    void setInstalledCyberware(@Nullable LivingEntity entity, BodyRegion region, List<ItemStack> cyberware);

    boolean isCyberwareInstalled(ItemLike cyberware);

    /**
     * @return the installed amount of a piece of cyberware, 0 if not installed
     */
    int getCyberwareRank(ItemLike cyberware);

    /**
     * @return the installed stack matching the given item, or {@link ItemStack#EMPTY}
     */
    ItemStack getCyberware(ItemLike cyberware);

    default boolean isCyberwareInstalled(ItemStack cyberware) {
        return !cyberware.isEmpty() && isCyberwareInstalled(cyberware.getItem());
    }

    default int getCyberwareRank(ItemStack cyberware) {
        return cyberware.isEmpty() ? 0 : getCyberwareRank(cyberware.getItem());
    }

    default ItemStack getCyberware(ItemStack cyberware) {
        return cyberware.isEmpty() ? ItemStack.EMPTY : getCyberware(cyberware.getItem());
    }

    /**
     * Uninstalls everything and reinstalls the configured starting cyberware.
     */
    void resetWare(@Nullable LivingEntity entity);

    // ---- essentials ----

    boolean hasEssential(BodyRegion region);

    boolean hasEssential(BodyRegion region, SidedLimb.Side side);

    void setHasEssential(BodyRegion region, boolean hasLeft, boolean hasRight);

    // ---- power ----

    /**
     * Recomputes capacity, batteries, active (menu) items, hotkeys and hudjack items from the installed cyberware.
     */
    void updateCapacity();

    /**
     * Called every second: stores the power produced during the last second and starts a new buffer.
     */
    void resetBuffer();

    void addPower(int amount, ItemStack producer);

    boolean isAtCapacity(ItemStack producer);

    boolean isAtCapacity(ItemStack producer, int buffer);

    /**
     * @return the fill ratio between 0 and 1, or -1 if there is no capacity
     */
    float getPercentFull();

    int getCapacity();

    int getStoredPower();

    int getProduction();

    int getConsumption();

    /**
     * Uses power for a passive item; once a passive request fails, no more power is given out until the next reset.
     */
    boolean usePower(ItemStack consumer, int amount);

    boolean usePower(ItemStack consumer, int amount, boolean isPassive);

    /**
     * Makes all power requests succeed until the next buffer reset (used right after surgery).
     */
    void setImmune();

    List<ItemStack> getPowerOutages();

    List<Integer> getPowerOutageTimes();

    // ---- active items and hotkeys ----

    int getNumActiveItems();

    List<ItemStack> getActiveItems();

    void removeHotkey(int key);

    void addHotkey(int key, ItemStack stack);

    ItemStack getHotkey(int key);

    Iterable<Integer> getHotkeys();

    List<ItemStack> getHudjackItems();

    // ---- HUD ----

    void setHudData(CompoundTag tag);

    CompoundTag getHudData();

    boolean hasOpenedRadialMenu();

    void setOpenedRadialMenu(boolean opened);

    void setHudColor(int color);

    void setHudColor(float[] color);

    int getHudColorHex();

    float[] getHudColor();

    // ---- tolerance ----

    int getMaxTolerance(LivingEntity entity);

    void setTolerance(LivingEntity entity, int amount);

    int getTolerance(LivingEntity entity);

    // ---- persistence ----

    CompoundTag serializeNBT(HolderLookup.Provider provider);

    void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag);
}
