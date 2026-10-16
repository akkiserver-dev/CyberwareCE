package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareSurgeryEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.SidedLimb;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.SurgeryChamberBlock;
import flaxbeard.cyberware.common.item.CreativeBatteryItem;
import flaxbeard.cyberware.common.item.CyberwareItem;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.CyberwareMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Robosurgeon. It holds two inventories of {@code 12 * 10} slots: the cyberware currently installed in the patient
 * ({@link #slotsPlayer}) and the cyberware to install ({@link #slots}). Installed cyberware can be marked for removal
 * in {@link #discardSlots}. The surgery itself happens when an entity closes the Surgery Chamber below.
 */
public class SurgeryBlockEntity extends BlockEntity {
    public static final int SLOT_COUNT = BodyRegion.values().length * CyberwareConstants.WARE_PER_SLOT;
    private static final int SURGERY_TICKS = 80;

    /** Client side: whether the local player is currently being operated on, used for the fade to black overlay. */
    public static boolean workingOnPlayer = false;
    public static int playerProgressTicks = 0;

    public final ItemStackHandler slotsPlayer = new ItemStackHandler(SLOT_COUNT);
    public final ItemStackHandler slots = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public boolean[] discardSlots = new boolean[SLOT_COUNT];
    public boolean[] isEssentialMissing = new boolean[BodyRegion.values().length * 2];
    public int essence = 0;
    public int maxEssence = 0;
    public int wrongSlot = -1;
    public long ticksWrong = 0;
    public int lastEntity = -1;
    public int cooldownTicks = 0;
    public boolean missingPower = false;

    public boolean inProgress = false;
    @Nullable
    public LivingEntity targetEntity = null;
    public int progressTicks = 0;

    public SurgeryBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.SURGERY.get(), pos, state);
    }

    public boolean isUsableByPlayer(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    private static int index(BodyRegion region, int slot) {
        return region.ordinal() * CyberwareConstants.WARE_PER_SLOT + slot;
    }

    /**
     * @return the stack that will be in the patient after the surgery at an index, ignoring stacking
     */
    private ItemStack getResultingStack(int index) {
        ItemStack slotStack = slots.getStackInSlot(index);
        if (!slotStack.isEmpty()) return slotStack;
        return discardSlots[index] ? ItemStack.EMPTY : slotsPlayer.getStackInSlot(index);
    }

    // ---- patient ----

    /**
     * Copies the cyberware of the patient into {@link #slotsPlayer} and revalidates the planned changes.
     */
    public void updatePlayerSlots(LivingEntity entity, @Nullable CyberwareUserData data) {
        setChanged();

        if (data != null) {
            if (entity.getId() != lastEntity) {
                discardSlots = new boolean[SLOT_COUNT];
                lastEntity = entity.getId();
            }
            maxEssence = data.getMaxTolerance(entity);

            // update slotsPlayer with the items in the patient's body
            for (BodyRegion region : BodyRegion.values()) {
                NonNullList<ItemStack> installed = data.getInstalledCyberware(region);
                for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                    int index = index(region, slot);
                    ItemStack toPut = installed.get(slot).copy();
                    // a new item isn't set to discard by default unless it conflicts
                    if (!ItemStack.matches(toPut, slotsPlayer.getStackInSlot(index))) {
                        discardSlots[index] = doesItemConflict(toPut, region, slot);
                    }
                    slotsPlayer.setStackInSlot(index, toPut);
                }
                updateEssential(region);
            }

            // remove planned items whose requirements are no longer fulfilled
            boolean needToCheck = true;
            while (needToCheck) {
                needToCheck = false;
                for (BodyRegion region : BodyRegion.values()) {
                    for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                        int index = index(region, slot);
                        ItemStack stack = slots.getStackInSlot(index);
                        if (!stack.isEmpty() && !areRequirementsFulfilled(stack, region, slot)) {
                            addItemStack(entity, stack);
                            slots.setStackInSlot(index, ItemStack.EMPTY);
                            needToCheck = true;
                        }
                    }
                }
            }

            updateEssence();
        } else {
            clear(slotsPlayer);
            maxEssence = CyberwareConfig.get(level).essence();
            for (BodyRegion region : BodyRegion.values()) {
                updateEssential(region);
            }
        }
        wrongSlot = -1;
    }

    public boolean doesItemConflict(ItemStack stack, BodyRegion region, int slotToCheck) {
        if (stack.isEmpty()) return false;
        for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
            if (slot == slotToCheck) continue;
            int index = index(region, slot);
            ItemStack other = getResultingStack(index);
            if (other.isEmpty()) continue;

            // the same item always conflicts, even with a different quality
            if (other.is(stack.getItem())
                    || CyberwareAPI.getCyberware(other).isIncompatible(other, stack)
                    || CyberwareAPI.getCyberware(stack).isIncompatible(stack, other)) {
                setWrongSlot(index);
                return true;
            }
        }
        return false;
    }

    public void setWrongSlot(int index) {
        wrongSlot = index;
        if (level != null) {
            ticksWrong = level.getGameTime();
        }
    }

    /**
     * Marks the installed cyberware that requires {@code stack} for removal.
     */
    public void disableDependants(ItemStack stack, BodyRegion region, int slotToCheck) {
        if (stack.isEmpty()) return;
        for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
            if (slot == slotToCheck) continue;
            int index = index(region, slot);
            if (!areRequirementsFulfilled(slotsPlayer.getStackInSlot(index), region, slotToCheck)) {
                discardSlots[index] = true;
            }
        }
    }

    /**
     * Unmarks the installed cyberware that {@code stack} requires.
     */
    public void enableDependsOn(ItemStack stack, BodyRegion region, int slotToCheck) {
        if (stack.isEmpty()) return;
        Cyberware cyberware = CyberwareAPI.getCyberware(stack);
        for (List<ItemStack> alternatives : cyberware.required(stack)) {
            boolean found = false;
            search:
            for (ItemStack needed : alternatives) {
                for (BodyRegion otherRegion : BodyRegion.values()) {
                    for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                        if (slot == slotToCheck) continue;
                        int index = index(otherRegion, slot);
                        ItemStack playerStack = slotsPlayer.getStackInSlot(index);
                        if (!playerStack.isEmpty() && playerStack.is(needed.getItem())) {
                            found = true;
                            discardSlots[index] = false;
                            break search;
                        }
                    }
                }
            }
            if (!found) {
                CyberwareMod.LOGGER.error("Can't find required {} for {} in {}:{}", alternatives, stack, region, slotToCheck);
            }
        }
    }

    public boolean canDisableItem(ItemStack stack, BodyRegion region, int slotToCheck) {
        if (stack.isEmpty()) return true;
        for (BodyRegion otherRegion : BodyRegion.values()) {
            for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                if (slot == slotToCheck) continue;
                int index = index(otherRegion, slot);
                // note: like the original, only planned items are checked here
                ItemStack slotStack = slots.getStackInSlot(index);
                if (!areRequirementsFulfilled(slotStack, region, slotToCheck)) {
                    setWrongSlot(index);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean areRequirementsFulfilled(ItemStack stack, BodyRegion region, int slotToCheck) {
        if (stack.isEmpty()) return true;
        Cyberware cyberware = CyberwareAPI.getCyberware(stack);
        for (List<ItemStack> alternatives : cyberware.required(stack)) {
            boolean found = false;
            search:
            for (ItemStack needed : alternatives) {
                for (BodyRegion otherRegion : BodyRegion.values()) {
                    for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                        if (slot == slotToCheck) continue;
                        ItemStack other = getResultingStack(index(otherRegion, slot));
                        if (!other.isEmpty() && other.is(needed.getItem())) {
                            found = true;
                            break search;
                        }
                    }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public void updateEssential(BodyRegion region) {
        if (region.hasEssential()) {
            int found = findEssentials(region);
            isEssentialMissing[region.ordinal() * 2] = (found & 1) == 0;
            isEssentialMissing[region.ordinal() * 2 + 1] = (found & 2) == 0;
        } else {
            isEssentialMissing[region.ordinal() * 2] = false;
            isEssentialMissing[region.ordinal() * 2 + 1] = false;
        }
    }

    /**
     * @return a bit mask of the essential sides present after the surgery: 1 for left, 2 for right (3 if unsided)
     */
    private int findEssentials(BodyRegion region) {
        int result = 0;
        for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
            ItemStack stack = getResultingStack(index(region, slot));
            if (stack.isEmpty()) continue;

            Cyberware cyberware = CyberwareAPI.getCyberware(stack);
            if (!cyberware.isEssential(stack)) continue;
            if (region.isSided() && cyberware instanceof SidedLimb sidedLimb) {
                if (sidedLimb.getSide(stack) == SidedLimb.Side.LEFT && (result & 1) == 0) {
                    result += 1;
                } else if ((result & 2) == 0) {
                    result += 2;
                }
            } else {
                return 3;
            }
        }
        return result;
    }

    /**
     * Recomputes the essence left after the surgery and whether the patient would be left without power.
     */
    public void updateEssence() {
        essence = maxEssence;
        boolean hasConsumer = false;
        boolean hasProducer = false;

        for (int index = 0; index < SLOT_COUNT; index++) {
            ItemStack slotStack = slots.getStackInSlot(index);
            ItemStack playerStack = slotsPlayer.getStackInSlot(index);
            ItemStack stack = getResultingStack(index);
            if (stack.isEmpty()) continue;

            ItemStack result = stack.copy();
            if (!slotStack.isEmpty() && !playerStack.isEmpty() && CyberwareAPI.areCyberwareStacksEqual(playerStack, result)) {
                int maxSize = CyberwareAPI.getCyberware(result).installedStackSize(result);
                if (result.getCount() < maxSize) {
                    result.grow(Math.min(maxSize - result.getCount(), playerStack.getCount()));
                }
            }

            Cyberware cyberware = CyberwareAPI.getCyberware(result);
            essence -= cyberware.getEssenceCost(result);
            if (cyberware instanceof CyberwareItem item) {
                if (item.getPowerConsumption(result) > 0) hasConsumer = true;
                if (item.getPowerProduction(result) > 0 || item instanceof CreativeBatteryItem) hasProducer = true;
            }
        }
        missingPower = hasConsumer && !hasProducer;
    }

    // ---- surgery ----

    public boolean canOpen() {
        return !inProgress && cooldownTicks <= 0;
    }

    /**
     * Called when the chamber below is opened or closed. Closing it with exactly one entity inside starts the surgery.
     */
    public void notifyChange() {
        if (level == null) return;
        BlockState chamber = level.getBlockState(worldPosition.below());
        if (!(chamber.getBlock() instanceof SurgeryChamberBlock) || chamber.getValue(SurgeryChamberBlock.OPEN)) return;

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition.getX(), worldPosition.getY() - 2F, worldPosition.getZ(),
                        worldPosition.getX() + 1F, worldPosition.getY(), worldPosition.getZ() + 1F));
        if (entities.size() != 1) return;

        LivingEntity entity = entities.get(0);
        CyberwareSurgeryEvent.Pre event = new CyberwareSurgeryEvent.Pre(entity, slotsPlayer, slots);
        if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
            inProgress = true;
            progressTicks = 0;
            targetEntity = entity;
        } else {
            openChamber();
        }
    }

    private void openChamber() {
        if (level == null) return;
        BlockPos chamberPos = worldPosition.below();
        BlockState chamber = level.getBlockState(chamberPos);
        if (chamber.getBlock() instanceof SurgeryChamberBlock chamberBlock) {
            chamberBlock.toggleDoor(true, chamber, chamberPos, level);
        }
    }

    private static boolean isLocalPlayer(@Nullable LivingEntity entity) {
        return entity instanceof Player player && player.isLocalPlayer();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SurgeryBlockEntity surgery) {
        surgery.tick();
    }

    private void tick() {
        if (inProgress && progressTicks < SURGERY_TICKS) {
            CyberwareUserData data = CyberwareAPI.getUserData(targetEntity);
            if (targetEntity != null && targetEntity.isAlive() && data != null) {
                if (progressTicks > 20 && progressTicks < 60) {
                    targetEntity.setPos(worldPosition.getX() + .5F, targetEntity.getY(), worldPosition.getZ() + .5F);
                }
                if (progressTicks >= 20 && progressTicks <= 60 && progressTicks % 5 == 0 && !level.isClientSide()) {
                    targetEntity.hurt(CyberwareDamageTypes.source(level, CyberwareDamageTypes.SURGERY), 2F);
                }
                if (progressTicks == 60) {
                    processUpdate(data);
                }
                progressTicks++;

                if (isLocalPlayer(targetEntity)) {
                    workingOnPlayer = true;
                    playerProgressTicks = progressTicks;
                }
            } else {
                if (isLocalPlayer(targetEntity)) {
                    workingOnPlayer = false;
                }
                inProgress = false;
                progressTicks = 0;
                targetEntity = null;
                openChamber();
            }
        } else if (inProgress) {
            if (isLocalPlayer(targetEntity)) {
                workingOnPlayer = false;
            }
            inProgress = false;
            progressTicks = 0;
            targetEntity = null;
            cooldownTicks = 60;
            openChamber();
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public void processUpdate(CyberwareUserData data) {
        if (targetEntity == null || level == null) return;
        updatePlayerSlots(targetEntity, data);

        for (BodyRegion region : BodyRegion.values()) {
            NonNullList<ItemStack> toInstall = NonNullList.withSize(CyberwareConstants.WARE_PER_SLOT, ItemStack.EMPTY);
            int installIndex = 0;
            for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                int index = index(region, slot);
                ItemStack surgeryStack = slots.getStackInSlot(index);
                ItemStack playerStack = slotsPlayer.getStackInSlot(index).copy();

                if (!surgeryStack.isEmpty()) {
                    ItemStack toSet = surgeryStack.copy();
                    if (CyberwareAPI.areCyberwareStacksEqual(toSet, playerStack)) {
                        int maxSize = CyberwareAPI.getCyberware(toSet).installedStackSize(toSet);
                        if (toSet.getCount() < maxSize) {
                            int toShift = Math.min(maxSize - toSet.getCount(), playerStack.getCount());
                            playerStack.shrink(toShift);
                            toSet.grow(toShift);
                        }
                    }
                    if (!playerStack.isEmpty()) {
                        addItemStack(targetEntity, CyberwareAPI.sanitize(playerStack));
                    }
                    toInstall.set(installIndex++, toSet);
                } else if (!playerStack.isEmpty()) {
                    if (discardSlots[index]) {
                        addItemStack(targetEntity, CyberwareAPI.sanitize(playerStack));
                    } else {
                        toInstall.set(installIndex++, slotsPlayer.getStackInSlot(index).copy());
                    }
                }
            }
            if (!level.isClientSide()) {
                data.setInstalledCyberware(targetEntity, region, toInstall);
            }
            data.setHasEssential(region, !isEssentialMissing[region.ordinal() * 2], !isEssentialMissing[region.ordinal() * 2 + 1]);
        }
        data.setTolerance(targetEntity, essence);
        data.updateCapacity();
        data.setImmune();
        if (!level.isClientSide()) {
            CyberwareAPI.updateData(targetEntity);
        }
        clear(slots);
        setChanged();

        NeoForge.EVENT_BUS.post(new CyberwareSurgeryEvent.Post(targetEntity));
    }

    /**
     * Empties a handler in place, so that menus referencing it stay valid.
     */
    private static void clear(ItemStackHandler handler) {
        for (int index = 0; index < handler.getSlots(); index++) {
            handler.setStackInSlot(index, ItemStack.EMPTY);
        }
    }

    private void addItemStack(LivingEntity entity, ItemStack stack) {
        boolean drop = true;
        if (entity instanceof Player player) {
            drop = !player.getInventory().add(stack);
        }
        if (drop && level != null && !level.isClientSide()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5F, worldPosition.getY() - 2F, worldPosition.getZ() + .5F, stack));
        }
    }

    // ---- persistence ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.deserializeNBT(registries, tag.getCompound("inv"));
        slotsPlayer.deserializeNBT(registries, tag.getCompound("inv2"));
        ListTag discard = tag.getList("discard", Tag.TAG_BYTE);
        discardSlots = new boolean[SLOT_COUNT];
        for (int index = 0; index < Math.min(discard.size(), SLOT_COUNT); index++) {
            discardSlots[index] = ((ByteTag) discard.get(index)).getAsByte() > 0;
        }
        essence = tag.getInt("essence");
        maxEssence = tag.getInt("maxEssence");
        lastEntity = tag.getInt("lastEntity");
        missingPower = tag.getBoolean("missingPower");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("essence", essence);
        tag.putInt("maxEssence", maxEssence);
        tag.putInt("lastEntity", lastEntity);
        tag.putBoolean("missingPower", missingPower);
        tag.put("inv", slots.serializeNBT(registries));
        tag.put("inv2", slotsPlayer.serializeNBT(registries));
        ListTag discard = new ListTag();
        for (boolean value : discardSlots) {
            discard.add(ByteTag.valueOf(value));
        }
        tag.put("discard", discard);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
