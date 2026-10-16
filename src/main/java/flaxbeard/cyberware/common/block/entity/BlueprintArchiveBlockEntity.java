package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Stores blueprints (and paper). Nearby Engineering Tables show its contents.
 */
public class BlueprintArchiveBlockEntity extends NameableBlockEntity {
    public static final int SLOT_COUNT = 18;

    public final ItemStackHandler slots = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidItem(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BlueprintArchiveBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.BLUEPRINT_ARCHIVE.get(), pos, state, "cyberware.container.blueprint_archive");
    }

    public static boolean isValidItem(ItemStack stack) {
        return stack.getItem() instanceof Blueprint || stack.is(CyberwareTags.PAPER);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.deserializeNBT(registries, tag.getCompound("inv"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", slots.serializeNBT(registries));
    }
}
