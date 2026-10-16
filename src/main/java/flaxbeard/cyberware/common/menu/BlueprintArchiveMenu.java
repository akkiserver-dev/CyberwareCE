package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.common.block.entity.BlueprintArchiveBlockEntity;
import flaxbeard.cyberware.common.registry.CyberwareMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class BlueprintArchiveMenu extends ChestLikeMenu {
    private final BlueprintArchiveBlockEntity archive;

    public BlueprintArchiveMenu(int containerId, Inventory playerInventory, BlueprintArchiveBlockEntity archive) {
        super(CyberwareMenus.BLUEPRINT_ARCHIVE.get(), containerId, BlueprintArchiveBlockEntity.SLOT_COUNT / 9);
        this.archive = archive;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(archive.slots, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }
        addPlayerSlots(playerInventory);
    }

    public static BlueprintArchiveMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        if (playerInventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof BlueprintArchiveBlockEntity archive) {
            return new BlueprintArchiveMenu(containerId, playerInventory, archive);
        }
        throw new IllegalStateException("Missing Blueprint Archive");
    }

    public BlueprintArchiveBlockEntity getArchive() {
        return archive;
    }

    public ItemStack getArchiveStack(int index) {
        return archive.slots.getStackInSlot(index);
    }

    @Override
    public boolean stillValid(Player player) {
        return archive.isUsableByPlayer(player);
    }
}
