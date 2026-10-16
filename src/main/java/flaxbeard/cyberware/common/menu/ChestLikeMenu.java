package flaxbeard.cyberware.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A menu laid out like a chest: some rows of 9 slots above the player inventory.
 */
public abstract class ChestLikeMenu extends AbstractContainerMenu {
    protected final int rows;

    protected ChestLikeMenu(MenuType<?> type, int containerId, int rows) {
        super(type, containerId);
        this.rows = rows;
    }

    public int getRows() {
        return rows;
    }

    protected void addPlayerSlots(Inventory playerInventory) {
        MenuHelper.addPlayerInventory(this::addSlot, playerInventory, 8, 103 + (rows - 4) * 18);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        int containerSlots = rows * 9;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < containerSlots) {
            if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, containerSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }
}
