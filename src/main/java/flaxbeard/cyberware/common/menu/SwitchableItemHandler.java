package flaxbeard.cyberware.common.menu;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

/**
 * A fixed size inventory view that forwards to a target which can be swapped (or be absent),
 * so that a menu can switch between several inventories without changing its slots.
 */
public class SwitchableItemHandler implements IItemHandlerModifiable {
    private final int size;
    @Nullable
    private IItemHandlerModifiable target;

    public SwitchableItemHandler(int size) {
        this.size = size;
    }

    public void setTarget(@Nullable IItemHandlerModifiable target) {
        this.target = target;
    }

    @Nullable
    public IItemHandlerModifiable getTarget() {
        return target;
    }

    public boolean hasTarget() {
        return target != null;
    }

    private boolean isValidSlot(int slot) {
        return target != null && slot < target.getSlots();
    }

    @Override
    public int getSlots() {
        return size;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isValidSlot(slot) ? target.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isValidSlot(slot) ? target.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return isValidSlot(slot) ? target.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return isValidSlot(slot) ? target.getSlotLimit(slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidSlot(slot) && target.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isValidSlot(slot)) {
            target.setStackInSlot(slot, stack);
        }
    }
}
