package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.common.block.entity.ComponentBoxBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * The inventory of a Component Box, either placed or held in a hand.
 */
public class ComponentBoxMenu extends ChestLikeMenu {
    private final Predicate<Player> validator;

    private ComponentBoxMenu(int containerId, Inventory playerInventory, ItemStackHandler handler, Predicate<Player> validator, @Nullable ItemStack heldBox) {
        super(CyberwareMenus.COMPONENT_BOX.get(), containerId, ComponentBoxBlockEntity.SLOT_COUNT / 9);
        this.validator = validator;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(handler, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }

        int y = 103 + (rows - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new LockableSlot(playerInventory, column + row * 9 + 9, 8 + column * 18, y + row * 18, heldBox));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new LockableSlot(playerInventory, column, 8 + column * 18, y + 58, heldBox));
        }
    }

    public static ComponentBoxMenu forBlock(int containerId, Inventory playerInventory, ComponentBoxBlockEntity box) {
        return new ComponentBoxMenu(containerId, playerInventory, box.slots, box::isUsableByPlayer, null);
    }

    public static ComponentBoxMenu forItem(int containerId, Inventory playerInventory, InteractionHand hand) {
        Player player = playerInventory.player;
        ItemStack box = player.getItemInHand(hand);
        ItemStackHandler handler = new ItemStackHandler(ComponentBoxBlockEntity.SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(CyberwareTags.COMPONENTS);
            }

            @Override
            protected void onContentsChanged(int slot) {
                box.set(DataComponents.CONTAINER, ComponentBoxBlockEntity.saveContents(this));
            }
        };
        ComponentBoxBlockEntity.loadContents(handler, box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
        return new ComponentBoxMenu(containerId, playerInventory, handler,
                menuPlayer -> menuPlayer.getItemInHand(hand) == box && box.is(CyberwareItems.COMPONENT_BOX.get()), box);
    }

    public static ComponentBoxMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        boolean isItem = buffer.readBoolean();
        if (isItem) {
            return forItem(containerId, playerInventory, buffer.readEnum(InteractionHand.class));
        }
        if (playerInventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof ComponentBoxBlockEntity box) {
            return forBlock(containerId, playerInventory, box);
        }
        throw new IllegalStateException("Missing Component Box");
    }

    @Override
    public boolean stillValid(Player player) {
        return validator.test(player);
    }

    /**
     * A player inventory slot that can't be picked up while it holds the opened component box.
     */
    private static class LockableSlot extends Slot {
        @Nullable
        private final ItemStack locked;

        private LockableSlot(Inventory inventory, int index, int x, int y, @Nullable ItemStack locked) {
            super(inventory, index, x, y);
            this.locked = locked;
        }

        @Override
        public boolean mayPickup(Player player) {
            return locked == null || getItem() != locked;
        }
    }
}
