package flaxbeard.cyberware.common.data;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.SpecialBattery;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.HotkeyHelper;
import flaxbeard.cyberware.api.item.Hudjack;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareAttributes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link CyberwareUserData}, stored as a data attachment on entities.
 * <p>
 * Power works with a one second buffer: produced power is collected in a buffer and only stored at the next reset,
 * while consumers may use the power stored plus the power produced during the previous second.
 */
public class DefaultCyberwareUserData implements CyberwareUserData {
    private static final int DEFAULT_HUD_COLOR = 0x00FFFF;
    private static final int MAX_POWER_OUTAGES = 8;

    @Nullable
    private final LivingEntity owner;

    private final Map<BodyRegion, NonNullList<ItemStack>> cyberwareByRegion = new EnumMap<>(BodyRegion.class);
    private boolean[] missingEssentials = new boolean[BodyRegion.values().length * 2];

    private int powerStored = 0;
    private int powerProduction = 0;
    private int powerLastProduction = 0;
    private int powerConsumption = 0;
    private int powerLastConsumption = 0;
    private int powerCapacity = 0;
    // keyed by stack identity, like the original implementation; ItemStack.EMPTY is the key of external sources
    private Map<ItemStack, Integer> powerBuffer = new IdentityHashMap<>();
    private Map<ItemStack, Integer> powerLastBuffer = new IdentityHashMap<>();
    private boolean canGiveOut = true;
    private boolean isImmune = false;

    private final List<ItemStack> powerOutages = new ArrayList<>();
    private final List<Integer> powerOutageTimes = new ArrayList<>();
    private int missingEssence = 0;
    private List<ItemStack> specialBatteries = new ArrayList<>();
    private List<ItemStack> activeItems = new ArrayList<>();
    private List<ItemStack> hudjackItems = new ArrayList<>();
    private Map<Integer, ItemStack> hotkeys = new HashMap<>();
    private CompoundTag hudData = new CompoundTag();
    private boolean hasOpenedRadialMenu = false;

    private int hudColor = DEFAULT_HUD_COLOR;
    private float[] hudColorFloat = {0.0F, 1.0F, 1.0F};

    public DefaultCyberwareUserData(IAttachmentHolder holder) {
        this.owner = holder instanceof LivingEntity livingEntity ? livingEntity : null;
        for (BodyRegion region : BodyRegion.values()) {
            cyberwareByRegion.put(region, NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY));
        }
        resetWare(null);
    }

    // ---- installed cyberware ----

    @Override
    public void resetWare(@Nullable LivingEntity entity) {
        for (NonNullList<ItemStack> installed : cyberwareByRegion.values()) {
            for (ItemStack stack : installed) {
                if (entity != null && CyberwareAPI.isCyberware(stack)) {
                    CyberwareAPI.getCyberware(stack).onRemoved(entity, stack);
                }
            }
        }
        missingEssence = 0;
        for (BodyRegion region : BodyRegion.values()) {
            NonNullList<ItemStack> installed = NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY);
            NonNullList<ItemStack> startingItems = CyberwareConfig.getStartingItems(region);
            for (int index = 0; index < startingItems.size(); index++) {
                installed.set(index, startingItems.get(index).copy());
            }
            cyberwareByRegion.put(region, installed);
        }
        missingEssentials = new boolean[BodyRegion.values().length * 2];
        updateCapacity();
    }

    @Override
    public NonNullList<ItemStack> getInstalledCyberware(BodyRegion region) {
        return cyberwareByRegion.get(region);
    }

    @Override
    public void setInstalledCyberware(@Nullable LivingEntity entity, BodyRegion region, List<ItemStack> cyberware) {
        NonNullList<ItemStack> toInstall = NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY);
        if (cyberware.size() > CyberwareConstants.WARE_PER_SLOT) {
            CyberwareMod.LOGGER.error("Invalid number of cyberware to install: found {}, expecting {}", cyberware.size(), CyberwareConstants.WARE_PER_SLOT);
        }
        for (int index = 0; index < Math.min(cyberware.size(), toInstall.size()); index++) {
            toInstall.set(index, cyberware.get(index));
        }

        NonNullList<ItemStack> installed = cyberwareByRegion.get(region);
        if (entity != null) {
            for (ItemStack oldStack : installed) {
                if (CyberwareAPI.isCyberware(oldStack) && !containsSameStack(toInstall, oldStack)) {
                    CyberwareAPI.getCyberware(oldStack).onRemoved(entity, oldStack);
                }
            }
            for (ItemStack newStack : toInstall) {
                if (CyberwareAPI.isCyberware(newStack) && !containsSameStack(installed, newStack)) {
                    CyberwareAPI.getCyberware(newStack).onAdded(entity, newStack);
                }
            }
        }
        cyberwareByRegion.put(region, toInstall);
    }

    private static boolean containsSameStack(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack other : stacks) {
            if (CyberwareAPI.areCyberwareStacksEqual(other, stack) && other.getCount() == stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCyberwareInstalled(ItemLike cyberware) {
        return getCyberwareRank(cyberware) > 0;
    }

    @Override
    public int getCyberwareRank(ItemLike cyberware) {
        ItemStack found = getCyberware(cyberware);
        return found.isEmpty() ? 0 : found.getCount();
    }

    @Override
    public ItemStack getCyberware(ItemLike cyberware) {
        Item item = cyberware.asItem();
        ItemStack template = new ItemStack(item);
        if (!CyberwareAPI.isCyberware(template)) {
            return ItemStack.EMPTY;
        }
        BodyRegion region = CyberwareAPI.getCyberware(template).getBodyRegion(template);
        for (ItemStack stack : getInstalledCyberware(region)) {
            if (!stack.isEmpty() && stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ---- essentials ----

    @Override
    public boolean hasEssential(BodyRegion region) {
        return !missingEssentials[region.ordinal() * 2];
    }

    @Override
    public boolean hasEssential(BodyRegion region, SidedLimb.Side side) {
        return !missingEssentials[region.ordinal() * 2 + (side == SidedLimb.Side.LEFT ? 0 : 1)];
    }

    @Override
    public void setHasEssential(BodyRegion region, boolean hasLeft, boolean hasRight) {
        missingEssentials[region.ordinal() * 2] = !hasLeft;
        missingEssentials[region.ordinal() * 2 + 1] = !hasRight;
    }

    // ---- power ----

    @Override
    public void updateCapacity() {
        powerCapacity = 0;
        specialBatteries = new ArrayList<>();
        activeItems = new ArrayList<>();
        hudjackItems = new ArrayList<>();
        hotkeys = new HashMap<>();

        for (BodyRegion region : BodyRegion.values()) {
            for (ItemStack stack : getInstalledCyberware(region)) {
                if (!CyberwareAPI.isCyberware(stack)) continue;
                Cyberware cyberware = CyberwareAPI.getCyberware(stack);

                if (cyberware instanceof MenuItem menuItem && menuItem.hasMenu(stack)) {
                    activeItems.add(stack);
                    int hotkey = HotkeyHelper.getHotkey(stack);
                    if (hotkey != HotkeyHelper.NONE) {
                        hotkeys.put(hotkey, stack);
                    }
                }
                if (cyberware instanceof Hudjack) {
                    hudjackItems.add(stack);
                }
                if (cyberware instanceof SpecialBattery) {
                    specialBatteries.add(stack);
                } else {
                    powerCapacity += cyberware.getCapacity(stack);
                }
            }
        }
        powerStored = Math.min(powerStored, powerCapacity);
    }

    private static SpecialBattery battery(ItemStack stack) {
        return (SpecialBattery) CyberwareAPI.getCyberware(stack);
    }

    @Override
    public int getCapacity() {
        int specialCapacity = 0;
        for (ItemStack stack : specialBatteries) {
            specialCapacity += battery(stack).getCapacity(stack);
        }
        return powerCapacity + specialCapacity;
    }

    @Override
    public int getStoredPower() {
        int specialStored = 0;
        for (ItemStack stack : specialBatteries) {
            specialStored += battery(stack).getStoredEnergy(stack);
        }
        return powerStored + specialStored;
    }

    @Override
    public float getPercentFull() {
        int capacity = getCapacity();
        if (capacity == 0) return -1F;
        return getStoredPower() / (float) capacity;
    }

    @Override
    public boolean isAtCapacity(ItemStack producer) {
        return isAtCapacity(producer, 0);
    }

    @Override
    public boolean isAtCapacity(ItemStack producer, int buffer) {
        int spaceNormal = powerCapacity - powerStored;
        if (spaceNormal > buffer) return false;

        int spaceSpecial = 0;
        for (ItemStack stack : specialBatteries) {
            spaceSpecial += battery(stack).add(stack, producer, buffer + 1, true);
            if (spaceNormal + spaceSpecial > buffer) return false;
        }
        return true;
    }

    @Override
    public void addPower(int amount, ItemStack producer) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive!");
        }
        ItemStack key = ItemStack.EMPTY;
        if (!producer.isEmpty()) {
            key = producer.getComponentsPatch().isEmpty() && producer.getCount() == 1 ? producer : new ItemStack(producer.getItem());
        }
        powerBuffer.merge(key, amount, Integer::sum);
        powerProduction += amount;
    }

    @Override
    public boolean usePower(ItemStack consumer, int amount) {
        return usePower(consumer, amount, true);
    }

    @Override
    public boolean usePower(ItemStack consumer, int amount, boolean isPassive) {
        if (isImmune) return true;

        if (!canGiveOut) {
            setOutOfPower(consumer);
            return false;
        }

        powerConsumption += amount;

        int lastBufferSum = sum(powerLastBuffer);
        int available = powerStored + lastBufferSum;

        if (available < amount) {
            int missing = amount - available;
            int availableSpecial = 0;
            for (ItemStack stack : specialBatteries) {
                int extracted = battery(stack).extract(stack, missing, true);
                missing -= extracted;
                availableSpecial += extracted;
                if (missing <= 0) break;
            }

            if (availableSpecial + available >= amount) {
                missing = amount - available;
                for (ItemStack stack : specialBatteries) {
                    missing -= battery(stack).extract(stack, missing, false);
                    if (missing <= 0) break;
                }
                amount -= availableSpecial;
            }
        }

        if (available < amount) {
            setOutOfPower(consumer);
            if (isPassive) {
                canGiveOut = false;
            }
            return false;
        }

        int leftAfterBuffer = Math.max(0, amount - lastBufferSum);
        subtractFromLastBuffer(amount);
        powerStored -= leftAfterBuffer;
        return true;
    }

    private static int sum(Map<ItemStack, Integer> map) {
        int total = 0;
        for (int value : map.values()) {
            total += value;
        }
        return total;
    }

    private void subtractFromLastBuffer(int amount) {
        for (Map.Entry<ItemStack, Integer> entry : powerLastBuffer.entrySet()) {
            int value = entry.getValue();
            int toSubtract = Math.min(value, amount);
            amount -= toSubtract;
            entry.setValue(value - toSubtract);
            if (amount <= 0) break;
        }
    }

    /**
     * Remembers a consumer that ran out of power, shown by the HUD on the client.
     */
    private void setOutOfPower(ItemStack stack) {
        if (owner == null || !owner.level().isClientSide() || stack.isEmpty()) return;

        for (int index = 0; index < powerOutages.size(); index++) {
            if (powerOutages.get(index).is(stack.getItem())) {
                powerOutages.remove(index);
                powerOutageTimes.remove(index);
                break;
            }
        }
        powerOutages.add(stack);
        powerOutageTimes.add(owner.tickCount);
        if (powerOutages.size() >= MAX_POWER_OUTAGES) {
            powerOutages.remove(0);
            powerOutageTimes.remove(0);
        }
    }

    private void storePower(Map<ItemStack, Integer> buffer) {
        for (ItemStack batteryStack : specialBatteries) {
            SpecialBattery specialBattery = battery(batteryStack);
            for (Map.Entry<ItemStack, Integer> entry : buffer.entrySet()) {
                int amount = entry.getValue();
                int taken = specialBattery.add(batteryStack, entry.getKey(), amount, false);
                entry.setValue(amount - taken);
            }
        }
        powerStored = Math.min(powerCapacity, powerStored + sum(buffer));
    }

    @Override
    public void resetBuffer() {
        canGiveOut = true;
        storePower(powerLastBuffer);
        powerLastBuffer = powerBuffer;
        powerBuffer = new IdentityHashMap<>();
        isImmune = false;

        powerLastConsumption = powerConsumption;
        powerLastProduction = powerProduction;
        powerProduction = 0;
        powerConsumption = 0;
    }

    @Override
    public void setImmune() {
        isImmune = true;
    }

    @Override
    public int getProduction() {
        return powerLastProduction;
    }

    @Override
    public int getConsumption() {
        return powerLastConsumption;
    }

    @Override
    public List<ItemStack> getPowerOutages() {
        return powerOutages;
    }

    @Override
    public List<Integer> getPowerOutageTimes() {
        return powerOutageTimes;
    }

    // ---- active items and hotkeys ----

    @Override
    public int getNumActiveItems() {
        return activeItems.size();
    }

    @Override
    public List<ItemStack> getActiveItems() {
        return activeItems;
    }

    @Override
    public List<ItemStack> getHudjackItems() {
        return hudjackItems;
    }

    @Override
    public void removeHotkey(int key) {
        hotkeys.remove(key);
    }

    @Override
    public void addHotkey(int key, ItemStack stack) {
        hotkeys.put(key, stack);
    }

    @Override
    public ItemStack getHotkey(int key) {
        return hotkeys.getOrDefault(key, ItemStack.EMPTY);
    }

    @Override
    public Iterable<Integer> getHotkeys() {
        return hotkeys.keySet();
    }

    // ---- HUD ----

    @Override
    public void setHudData(CompoundTag tag) {
        hudData = tag;
    }

    @Override
    public CompoundTag getHudData() {
        return hudData;
    }

    @Override
    public boolean hasOpenedRadialMenu() {
        return hasOpenedRadialMenu;
    }

    @Override
    public void setOpenedRadialMenu(boolean opened) {
        hasOpenedRadialMenu = opened;
    }

    @Override
    public void setHudColor(int color) {
        float red = ((color >> 16) & 0xFF) / 255F;
        float green = ((color >> 8) & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        setHudColor(new float[]{red, green, blue});
    }

    @Override
    public void setHudColor(float[] color) {
        hudColorFloat = color;
        int red = Math.round(color[0] * 255);
        int green = Math.round(color[1] * 255);
        int blue = Math.round(color[2] * 255);
        hudColor = ((red << 16) & 0xFF0000) | ((green << 8) & 0x00FF00) | (blue & 0x0000FF);
    }

    @Override
    public int getHudColorHex() {
        return hudColor;
    }

    @Override
    public float[] getHudColor() {
        return hudColorFloat;
    }

    // ---- tolerance ----

    @Override
    public int getMaxTolerance(LivingEntity entity) {
        AttributeInstance tolerance = entity.getAttribute(CyberwareAttributes.TOLERANCE);
        return tolerance == null ? CyberwareConfig.get(entity.level()).essence() : (int) tolerance.getValue();
    }

    @Override
    public int getTolerance(LivingEntity entity) {
        return getMaxTolerance(entity) - missingEssence;
    }

    @Override
    public void setTolerance(LivingEntity entity, int amount) {
        missingEssence = getMaxTolerance(entity) - amount;
    }

    // ---- persistence ----

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        ListTag regions = new ListTag();
        for (BodyRegion region : BodyRegion.values()) {
            ListTag stacks = new ListTag();
            for (ItemStack stack : getInstalledCyberware(region)) {
                stacks.add(stack.isEmpty() ? new CompoundTag() : stack.save(provider));
            }
            regions.add(stacks);
        }
        tag.put("cyberware", regions);

        ListTag essentials = new ListTag();
        for (boolean missing : missingEssentials) {
            essentials.add(ByteTag.valueOf(missing));
        }
        tag.put("discard", essentials);
        tag.put("powerBuffer", serializeBuffer(powerBuffer, provider));
        tag.put("powerBufferLast", serializeBuffer(powerLastBuffer, provider));
        tag.putInt("powerCap", powerCapacity);
        tag.putInt("storedPower", powerStored);
        tag.putInt("missingEssence", missingEssence);
        tag.put("hud", hudData);
        tag.putInt("color", hudColor);
        tag.putBoolean("hasOpenedRadialMenu", hasOpenedRadialMenu);
        return tag;
    }

    private static ListTag serializeBuffer(Map<ItemStack, Integer> buffer, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<ItemStack, Integer> entry : buffer.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putBoolean("null", entry.getKey().isEmpty());
            if (!entry.getKey().isEmpty()) {
                entryTag.put("item", entry.getKey().save(provider));
            }
            entryTag.putInt("value", entry.getValue());
            list.add(entryTag);
        }
        return list;
    }

    private static Map<ItemStack, Integer> deserializeBuffer(ListTag list, HolderLookup.Provider provider) {
        Map<ItemStack, Integer> buffer = new IdentityHashMap<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            ItemStack stack = entryTag.getBoolean("null") ? ItemStack.EMPTY : ItemStack.parseOptional(provider, entryTag.getCompound("item"));
            buffer.merge(stack, entryTag.getInt("value"), Integer::sum);
        }
        return buffer;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        powerBuffer = deserializeBuffer(tag.getList("powerBuffer", Tag.TAG_COMPOUND), provider);
        powerLastBuffer = deserializeBuffer(tag.getList("powerBufferLast", Tag.TAG_COMPOUND), provider);
        powerCapacity = tag.getInt("powerCap");
        powerStored = tag.getInt("storedPower");
        missingEssence = tag.getInt("missingEssence");
        hudData = tag.getCompound("hud");
        hasOpenedRadialMenu = tag.getBoolean("hasOpenedRadialMenu");

        ListTag essentials = tag.getList("discard", Tag.TAG_BYTE);
        missingEssentials = new boolean[BodyRegion.values().length * 2];
        for (int index = 0; index < Math.min(essentials.size(), missingEssentials.length); index++) {
            missingEssentials[index] = ((ByteTag) essentials.get(index)).getAsByte() > 0;
        }

        ListTag regions = tag.getList("cyberware", Tag.TAG_LIST);
        for (int regionIndex = 0; regionIndex < Math.min(regions.size(), BodyRegion.values().length); regionIndex++) {
            ListTag stacks = regions.getList(regionIndex);
            NonNullList<ItemStack> installed = NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY);
            for (int index = 0; index < Math.min(stacks.size(), installed.size()); index++) {
                installed.set(index, ItemStack.parseOptional(provider, stacks.getCompound(index)));
            }
            setInstalledCyberware(null, BodyRegion.values()[regionIndex], installed);
        }

        setHudColor(tag.contains("color") ? tag.getInt("color") : DEFAULT_HUD_COLOR);
        updateCapacity();
    }
}
