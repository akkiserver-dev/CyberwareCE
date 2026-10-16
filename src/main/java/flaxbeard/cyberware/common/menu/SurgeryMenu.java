package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SurgeryMenu extends AbstractContainerMenu {
    public static final int SURGERY_SLOTS = SurgeryBlockEntity.SLOT_COUNT;
    /** Only the first 8 of the 10 slots of a body region are shown, like in the original. */
    public static final int VISIBLE_SLOTS_PER_REGION = 8;
    private static final int INVENTORY_START = SURGERY_SLOTS;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final SurgeryBlockEntity surgery;

    public SurgeryMenu(int containerId, Inventory playerInventory, SurgeryBlockEntity surgery) {
        super(CyberwareMenus.SURGERY.get(), containerId);
        this.surgery = surgery;

        int index = 0;
        for (BodyRegion region : BodyRegion.values()) {
            for (int slot = 0; slot < CyberwareConstants.WARE_PER_SLOT; slot++) {
                int x = slot < VISIBLE_SLOTS_PER_REGION ? 9 + 20 * slot : -1000;
                int y = slot < VISIBLE_SLOTS_PER_REGION ? 109 : -1000;
                addSlot(new SurgerySlot(index, x, y, region));
                index++;
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    public static SurgeryMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        if (playerInventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof SurgeryBlockEntity surgery) {
            return new SurgeryMenu(containerId, playerInventory, surgery);
        }
        throw new IllegalStateException("Missing Robosurgeon");
    }

    public SurgeryBlockEntity getSurgery() {
        return surgery;
    }

    @Override
    public boolean stillValid(Player player) {
        return surgery.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot instanceof SurgerySlot || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    /**
     * A slot of the cyberware to install. Its index matches the index of the installed cyberware in the Robosurgeon.
     */
    public class SurgerySlot extends SlotItemHandler {
        public final BodyRegion region;
        private final int surgeryIndex;
        /** Client side: whether the slot is on the displayed page of the Robosurgeon screen. */
        public boolean visible = true;

        private SurgerySlot(int index, int x, int y, BodyRegion region) {
            super(surgery.slots, index, x, y);
            this.region = region;
            this.surgeryIndex = index;
        }

        private int slotInRegion() {
            return surgeryIndex % CyberwareConstants.WARE_PER_SLOT;
        }

        public int getSurgeryIndex() {
            return surgeryIndex;
        }

        /**
         * @return the cyberware currently installed in the patient at this position
         */
        public ItemStack getPlayerStack() {
            return surgery.slotsPlayer.getStackInSlot(surgeryIndex);
        }

        public boolean isDiscarded() {
            return surgery.discardSlots[surgeryIndex];
        }

        public void setDiscarded(boolean discarded) {
            surgery.discardSlots[surgeryIndex] = discarded;
            surgery.updateEssential(region);
            surgery.updateEssence();
        }

        @Override
        public boolean isActive() {
            return visible && slotInRegion() < VISIBLE_SLOTS_PER_REGION;
        }

        @Override
        public boolean mayPickup(Player player) {
            return surgery.canDisableItem(getItem(), region, slotInRegion());
        }

        @Override
        public void setChanged() {
            super.setChanged();
            surgery.updateEssence();
            surgery.setChanged();
        }

        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty() && mayPlace(stack)) {
                surgery.disableDependants(getPlayerStack(), region, slotInRegion());
            }
            super.set(stack);
            surgery.updateEssential(region);
            surgery.updateEssence();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            ItemStack playerStack = getPlayerStack();
            if (!playerStack.isEmpty() && !surgery.canDisableItem(playerStack, region, slotInRegion())) {
                return false;
            }
            if (!CyberwareAPI.isCyberware(stack) || CyberwareAPI.getCyberware(stack).getBodyRegion(stack) != region) {
                return false;
            }
            if (CyberwareAPI.areCyberwareStacksEqual(stack, playerStack)
                    && playerStack.getCount() == CyberwareAPI.getCyberware(stack).installedStackSize(stack)) {
                return false;
            }
            return !doesItemConflict(stack) && areRequirementsFulfilled(stack);
        }

        public boolean doesItemConflict(ItemStack stack) {
            return surgery.doesItemConflict(stack, region, slotInRegion());
        }

        public boolean areRequirementsFulfilled(ItemStack stack) {
            return surgery.areRequirementsFulfilled(stack, region, slotInRegion());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            if (!CyberwareAPI.isCyberware(stack)) return 1;
            int maxSize = CyberwareAPI.getCyberware(stack).installedStackSize(stack);
            ItemStack playerStack = getPlayerStack();
            if (CyberwareAPI.areCyberwareStacksEqual(playerStack, stack)) {
                return maxSize - playerStack.getCount();
            }
            return maxSize;
        }
    }
}
