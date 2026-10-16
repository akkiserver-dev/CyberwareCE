package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ScannerMenu extends AbstractContainerMenu {
    private static final int INVENTORY_START = 3;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ScannerBlockEntity scanner;
    private final ContainerData data;

    public ScannerMenu(int containerId, Inventory playerInventory, ScannerBlockEntity scanner) {
        super(CyberwareMenus.SCANNER.get(), containerId);
        this.scanner = scanner;

        addSlot(new SlotItemHandler(scanner.slots, ScannerBlockEntity.SLOT_INPUT, 35, 53));
        addSlot(new SlotItemHandler(scanner.slots, ScannerBlockEntity.SLOT_PAPER, 15, 53));
        addSlot(new SlotItemHandler(scanner.slots, ScannerBlockEntity.SLOT_OUTPUT, 141, 57));
        MenuHelper.addPlayerInventory(this::addSlot, playerInventory, 8, 84);

        if (scanner.getLevel() != null && scanner.getLevel().isClientSide()) {
            data = new SimpleContainerData(1);
        } else {
            data = new ContainerData() {
                @Override
                public int get(int index) {
                    return scanner.ticks;
                }

                @Override
                public void set(int index, int value) {
                    scanner.ticks = value;
                }

                @Override
                public int getCount() {
                    return 1;
                }
            };
        }
        addDataSlots(data);
    }

    public static ScannerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        if (playerInventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ScannerBlockEntity scanner) {
            return new ScannerMenu(containerId, playerInventory, scanner);
        }
        throw new IllegalStateException("Missing Scanner");
    }

    public ScannerBlockEntity getScanner() {
        return scanner;
    }

    /**
     * @return the scan progress in ticks, as synced from the server
     */
    public int getTicks() {
        return data.get(0);
    }

    @Override
    public boolean stillValid(Player player) {
        return scanner.isUsableByPlayer(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (ScannerBlockEntity.isItemValidForSlot(ScannerBlockEntity.SLOT_PAPER, stack)) {
            if (!moveItemStackTo(stack, ScannerBlockEntity.SLOT_PAPER, ScannerBlockEntity.SLOT_PAPER + 1, false)) return ItemStack.EMPTY;
        } else if (ScannerBlockEntity.isItemValidForSlot(ScannerBlockEntity.SLOT_INPUT, stack)) {
            if (!moveItemStackTo(stack, ScannerBlockEntity.SLOT_INPUT, ScannerBlockEntity.SLOT_INPUT + 1, false)) return ItemStack.EMPTY;
        } else if (index < HOTBAR_START) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }
}
