package flaxbeard.cyberware.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

final class MenuHelper {
    private MenuHelper() {
    }

    /**
     * Adds the 27 inventory slots followed by the 9 hotbar slots.
     */
    static void addPlayerInventory(Consumer<Slot> addSlot, Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot.accept(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot.accept(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }
}
