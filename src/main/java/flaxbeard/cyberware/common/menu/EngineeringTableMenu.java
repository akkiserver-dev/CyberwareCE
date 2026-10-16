package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.block.entity.BlueprintArchiveBlockEntity;
import flaxbeard.cyberware.common.block.entity.ComponentBoxBlockEntity;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Engineering Table, with an optional panel for a nearby Blueprint Archive and one for a Component Box
 * (placed nearby or carried in the inventory). Several archives or boxes can be cycled through.
 */
public class EngineeringTableMenu extends AbstractContainerMenu {
    public static final int TABLE_SLOTS = 10;
    public static final int INVENTORY_START = TABLE_SLOTS;
    public static final int HOTBAR_START = INVENTORY_START + 27;
    public static final int ARCHIVE_START = HOTBAR_START + 9;
    public static final int ARCHIVE_SLOTS = 18;
    public static final int COMPONENT_START = ARCHIVE_START + ARCHIVE_SLOTS;
    public static final int COMPONENT_SLOTS = 18;
    public static final int PANEL_WIDTH = 65;

    private final EngineeringTableBlockEntity table;
    private final Inventory playerInventory;
    private final Layout layout;
    private final SwitchableItemHandler archiveHandler = new SwitchableItemHandler(ARCHIVE_SLOTS);
    private final SwitchableItemHandler componentHandler = new SwitchableItemHandler(COMPONENT_SLOTS);
    /** Horizontal offset of the table, which moves to the right when the component box panel is shown. */
    private final int offset;
    @Nullable
    private ItemStack openedItemBox;

    public EngineeringTableMenu(int containerId, Inventory playerInventory, EngineeringTableBlockEntity table, Layout layout) {
        super(CyberwareMenus.ENGINEERING_TABLE.get(), containerId);
        this.table = table;
        this.playerInventory = playerInventory;
        this.layout = layout;
        this.offset = layout.boxes.isEmpty() ? 0 : PANEL_WIDTH;

        addSlot(new TableSlot(EngineeringTableBlockEntity.SLOT_DESTROY, 15 + offset, 20));
        addSlot(new TableSlot(EngineeringTableBlockEntity.SLOT_PAPER, 15 + offset, 53));
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                addSlot(new TableSlot(2 + row * 2 + column, 71 + column * 18 + offset, 17 + row * 18));
            }
        }
        addSlot(new TableSlot(EngineeringTableBlockEntity.SLOT_BLUEPRINT, 115 + offset, 53));
        addSlot(new TableSlot(EngineeringTableBlockEntity.SLOT_RESULT, 145 + offset, 21));

        MenuHelper.addPlayerInventory(this::addSlot, playerInventory, 8 + offset, 84);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new PanelSlot(archiveHandler, column + row * 3, 181 + column * 18 + offset, 22 + row * 18));
            }
        }
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new PanelSlot(componentHandler, column + row * 3, 8 + column * 18, 22 + row * 18));
            }
        }

        selectArchive();
        selectBox();
        table.updateRecipe();
    }

    public static EngineeringTableMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Layout layout = Layout.read(buffer);
        if (playerInventory.player.level().getBlockEntity(pos) instanceof EngineeringTableBlockEntity table) {
            return new EngineeringTableMenu(containerId, playerInventory, table, layout);
        }
        throw new IllegalStateException("Missing Engineering Table");
    }

    public EngineeringTableBlockEntity getTable() {
        return table;
    }

    public int getOffset() {
        return offset;
    }

    public boolean hasArchive() {
        return archiveHandler.hasTarget();
    }

    public boolean hasComponentBox() {
        return componentHandler.hasTarget();
    }

    public boolean canSwitchArchive() {
        return layout.archives.size() > 1;
    }

    public boolean canSwitchBox() {
        return layout.boxes.size() > 1;
    }

    @Nullable
    public BlueprintArchiveBlockEntity getArchive() {
        if (layout.archives.isEmpty()) return null;
        return table.getLevel().getBlockEntity(layout.archives.get(layout.archiveIndex)) instanceof BlueprintArchiveBlockEntity archive ? archive : null;
    }

    /**
     * @return the name of the selected component box
     */
    public Component getComponentBoxName() {
        if (layout.boxes.isEmpty()) return Component.empty();
        BoxSource source = layout.boxes.get(layout.boxIndex);
        if (source.slot >= 0) {
            return playerInventory.getItem(source.slot).getHoverName();
        }
        return table.getLevel().getBlockEntity(source.pos) instanceof ComponentBoxBlockEntity box ? box.getName() : Component.empty();
    }

    // ---- switching ----

    public void switchArchive(boolean forward) {
        if (layout.archives.isEmpty()) return;
        int size = layout.archives.size();
        layout.archiveIndex = (layout.archiveIndex + (forward ? 1 : size - 1)) % size;
        selectArchive();
        if (!table.getLevel().isClientSide()) {
            table.lastPlayerArchive.put(playerInventory.player.getStringUUID(), layout.archives.get(layout.archiveIndex));
            table.setChanged();
        }
    }

    public void switchBox(boolean forward) {
        if (layout.boxes.isEmpty()) return;
        int size = layout.boxes.size();
        layout.boxIndex = (layout.boxIndex + (forward ? 1 : size - 1)) % size;
        selectBox();
    }

    private void selectArchive() {
        BlueprintArchiveBlockEntity archive = getArchive();
        archiveHandler.setTarget(archive == null ? null : archive.slots);
    }

    private void selectBox() {
        openedItemBox = null;
        if (layout.boxes.isEmpty()) {
            componentHandler.setTarget(null);
            return;
        }
        BoxSource source = layout.boxes.get(layout.boxIndex);
        if (source.slot >= 0) {
            ItemStack box = playerInventory.getItem(source.slot);
            if (!box.is(CyberwareItems.COMPONENT_BOX.get())) {
                componentHandler.setTarget(null);
                return;
            }
            openedItemBox = box;
            ItemStackHandler handler = new ItemStackHandler(COMPONENT_SLOTS) {
                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return stack.is(CyberwareTags.COMPONENTS);
                }

                @Override
                protected void onContentsChanged(int slot) {
                    saveItemBox(this);
                }
            };
            ComponentBoxBlockEntity.loadContents(handler, box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            componentHandler.setTarget(handler);
        } else if (table.getLevel().getBlockEntity(source.pos) instanceof ComponentBoxBlockEntity box) {
            componentHandler.setTarget(box.slots);
        } else {
            componentHandler.setTarget(null);
        }
    }

    private void saveItemBox(ItemStackHandler handler) {
        if (openedItemBox != null && openedItemBox.is(CyberwareItems.COMPONENT_BOX.get())) {
            openedItemBox.set(DataComponents.CONTAINER, ComponentBoxBlockEntity.saveContents(handler));
        }
    }

    /**
     * Drops panels whose archive or box has been removed.
     */
    private void validatePanels() {
        if (archiveHandler.hasTarget() && getArchive() == null) {
            layout.archives.remove(layout.archiveIndex);
            layout.archiveIndex = 0;
            selectArchive();
        }
        if (componentHandler.hasTarget() && !isBoxValid()) {
            layout.boxes.remove(layout.boxIndex);
            layout.boxIndex = 0;
            selectBox();
        }
    }

    private boolean isBoxValid() {
        BoxSource source = layout.boxes.get(layout.boxIndex);
        if (source.slot >= 0) {
            return openedItemBox != null && playerInventory.getItem(source.slot) == openedItemBox;
        }
        return table.getLevel().getBlockEntity(source.pos) instanceof ComponentBoxBlockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        validatePanels();
        return table.isUsableByPlayer(player);
    }

    // ---- shift clicking ----

    private boolean moveToArchive(ItemStack stack) {
        return hasArchive() && moveItemStackTo(stack, ARCHIVE_START, ARCHIVE_START + ARCHIVE_SLOTS, false);
    }

    private boolean moveToBox(ItemStack stack) {
        return hasComponentBox() && moveItemStackTo(stack, COMPONENT_START, COMPONENT_START + COMPONENT_SLOTS, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index == EngineeringTableBlockEntity.SLOT_RESULT) {
            if (!moveItemStackTo(stack, INVENTORY_START, ARCHIVE_START, true)) return ItemStack.EMPTY;
            table.subtractResources();
        } else if ((index == EngineeringTableBlockEntity.SLOT_BLUEPRINT || index == EngineeringTableBlockEntity.SLOT_PAPER) && hasArchive()) {
            if (!moveItemStackTo(stack, ARCHIVE_START, ARCHIVE_START + ARCHIVE_SLOTS, true)
                    && !moveItemStackTo(stack, INVENTORY_START, ARCHIVE_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= TABLE_SLOTS) {
            if (stack.is(CyberwareTags.PAPER) && moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_PAPER, EngineeringTableBlockEntity.SLOT_PAPER + 1, false)) {
                // moved to the paper slot
            } else if (CyberwareAPI.canDeconstruct(stack)) {
                if (!moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_DESTROY, EngineeringTableBlockEntity.SLOT_DESTROY + 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof Blueprint) {
                if (!moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_BLUEPRINT, EngineeringTableBlockEntity.SLOT_BLUEPRINT + 1, false)) return ItemStack.EMPTY;
            } else if (index < HOTBAR_START) {
                if (!moveToArchive(stack)
                        && !moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_CRAFTING_START, EngineeringTableBlockEntity.SLOT_CRAFTING_END, false)
                        && !moveToBox(stack)
                        && !moveItemStackTo(stack, HOTBAR_START, ARCHIVE_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < ARCHIVE_START) {
                if (!moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_CRAFTING_START, EngineeringTableBlockEntity.SLOT_CRAFTING_END, false)
                        && !moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < COMPONENT_START) {
                if (!moveItemStackTo(stack, INVENTORY_START, ARCHIVE_START, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, EngineeringTableBlockEntity.SLOT_CRAFTING_START, EngineeringTableBlockEntity.SLOT_CRAFTING_END, false)
                    && !moveItemStackTo(stack, INVENTORY_START, ARCHIVE_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToBox(stack) && !moveToArchive(stack) && !moveItemStackTo(stack, INVENTORY_START, ARCHIVE_START, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        table.updateRecipe();
        return result;
    }

    private class TableSlot extends SlotItemHandler {
        private TableSlot(int index, int x, int y) {
            super(table.slots, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            table.setChanged();
        }
    }

    private class PanelSlot extends SlotItemHandler {
        private final SwitchableItemHandler handler;

        private PanelSlot(SwitchableItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
            this.handler = handler;
        }

        @Override
        public boolean isActive() {
            return handler.hasTarget();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // stacks modified in place must still be written back to carried component boxes
            if (handler == componentHandler && componentHandler.getTarget() instanceof ItemStackHandler itemHandler && openedItemBox != null) {
                saveItemBox(itemHandler);
            }
        }
    }

    /**
     * A component box, either in a player inventory slot or placed at a position.
     */
    public record BoxSource(int slot, BlockPos pos) {
    }

    /**
     * The archives and component boxes available to an Engineering Table, found by the server and sent to the client.
     */
    public static class Layout {
        private final List<BlockPos> archives;
        private final List<BoxSource> boxes;
        private int archiveIndex;
        private int boxIndex;

        private Layout(List<BlockPos> archives, int archiveIndex, List<BoxSource> boxes, int boxIndex) {
            this.archives = archives;
            this.archiveIndex = archiveIndex;
            this.boxes = boxes;
            this.boxIndex = boxIndex;
        }

        public static Layout find(EngineeringTableBlockEntity table, Player player) {
            Level level = table.getLevel();
            BlockPos center = table.getBlockPos();
            BlockPos preferred = table.lastPlayerArchive.get(player.getStringUUID());

            List<BlockPos> archives = new ArrayList<>();
            List<BoxSource> placedBoxes = new ArrayList<>();
            int archiveIndex = 0;
            for (int y = -2; y < 2; y++) {
                for (int x = -2; x < 3; x++) {
                    for (int z = -2; z < 3; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof BlueprintArchiveBlockEntity) {
                            if (pos.equals(preferred)) {
                                archiveIndex = archives.size();
                            }
                            archives.add(pos.immutable());
                        } else if (blockEntity instanceof ComponentBoxBlockEntity) {
                            placedBoxes.add(new BoxSource(-1, pos.immutable()));
                        }
                    }
                }
            }

            // carried boxes come first
            List<BoxSource> boxes = new ArrayList<>();
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                if (inventory.items.get(slot).is(CyberwareItems.COMPONENT_BOX.get())) {
                    boxes.add(new BoxSource(slot, BlockPos.ZERO));
                }
            }
            boxes.addAll(placedBoxes);
            return new Layout(archives, archiveIndex, boxes, 0);
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(archives.size());
            archives.forEach(buffer::writeBlockPos);
            buffer.writeVarInt(archiveIndex);
            buffer.writeVarInt(boxes.size());
            for (BoxSource box : boxes) {
                buffer.writeVarInt(box.slot);
                buffer.writeBlockPos(box.pos);
            }
            buffer.writeVarInt(boxIndex);
        }

        public static Layout read(FriendlyByteBuf buffer) {
            List<BlockPos> archives = new ArrayList<>();
            int archiveCount = buffer.readVarInt();
            for (int index = 0; index < archiveCount; index++) {
                archives.add(buffer.readBlockPos());
            }
            int archiveIndex = buffer.readVarInt();
            List<BoxSource> boxes = new ArrayList<>();
            int boxCount = buffer.readVarInt();
            for (int index = 0; index < boxCount; index++) {
                boxes.add(new BoxSource(buffer.readVarInt(), buffer.readBlockPos()));
            }
            int boxIndex = buffer.readVarInt();
            return new Layout(archives, archiveIndex, boxes, boxIndex);
        }
    }
}
