package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A portable box for cyberware components. Its contents are kept on the item when picked up.
 */
public class ComponentBoxBlockEntity extends NameableBlockEntity {
    public static final int SLOT_COUNT = 18;

    public final ItemStackHandler slots = createHandler(this::setChanged);

    public ComponentBoxBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.COMPONENT_BOX.get(), pos, state, "cyberware.container.component_box");
    }

    /**
     * @return a handler with the rules of a component box: components only
     */
    public static ItemStackHandler createHandler(Runnable onChanged) {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(CyberwareTags.COMPONENTS);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }
        };
    }

    public static void loadContents(ItemStackHandler handler, ItemContainerContents contents) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
        contents.copyInto(stacks);
        for (int index = 0; index < stacks.size(); index++) {
            handler.setStackInSlot(index, stacks.get(index));
        }
    }

    public static ItemContainerContents saveContents(ItemStackHandler handler) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
        for (int index = 0; index < stacks.size(); index++) {
            stacks.set(index, handler.getStackInSlot(index).copy());
        }
        return ItemContainerContents.fromItems(stacks);
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

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        loadContents(slots, input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, saveContents(slots));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("inv");
    }
}
