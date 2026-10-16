package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Blueprint;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.network.EngineeringSmashPayload;
import flaxbeard.cyberware.common.recipe.Deconstruction;
import flaxbeard.cyberware.common.recipe.EngineeringRecipe;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Destroys cyberware into components (sometimes producing a blueprint) and crafts cyberware from blueprints.
 * <p>
 * Slots: 0 item to destroy, 1 paper, 2-7 components, 8 blueprint, 9 crafting result.
 */
public class EngineeringTableBlockEntity extends NameableBlockEntity {
    public static final int SLOT_DESTROY = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_CRAFTING_START = 2;
    public static final int SLOT_CRAFTING_END = 8;
    public static final int SLOT_BLUEPRINT = 8;
    public static final int SLOT_RESULT = 9;
    private static final int[] BOTTOM_SLOTS = {2, 3, 4, 5, 6, 7, 9};

    /** The inventory as seen by players in the GUI. */
    public final ItemStackHandler slots = new ItemStackHandler(10) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_DESTROY -> CyberwareAPI.canDeconstruct(stack);
                case SLOT_PAPER -> stack.is(CyberwareTags.PAPER);
                case SLOT_BLUEPRINT -> stack.getItem() instanceof Blueprint;
                case SLOT_RESULT -> false;
                default -> true;
            };
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack result = super.extractItem(slot, amount, simulate);
            if (slot == SLOT_RESULT && !result.isEmpty() && !simulate) {
                subtractResources();
            }
            return result;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_DESTROY) {
                // the item to destroy is rendered on the table
                markForSync();
            } else {
                setChanged();
            }
            if (slot >= SLOT_CRAFTING_START && slot <= SLOT_BLUEPRINT && !updatingRecipe) {
                updateRecipe();
            }
        }
    };

    private final IItemHandler topAndSides = new AutomationHandler(new int[]{0, 1, 2, 3, 4, 5, 6});
    private final IItemHandler bottom = new AutomationHandler(BOTTOM_SLOTS);

    /** Client side time of the last smash, for the press animation. */
    public float clickedTime = -100F;
    private int time;
    private boolean updatingRecipe = false;
    public final Map<String, BlockPos> lastPlayerArchive = new HashMap<>();

    public EngineeringTableBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.ENGINEERING_TABLE.get(), pos, state, "cyberware.container.engineering");
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == Direction.DOWN ? bottom : topAndSides;
    }

    /**
     * @return the chance in percent of getting a blueprint when destroying the given item with paper
     */
    public static float getBlueprintChance(@Nullable Level level, ItemStack toDestroy) {
        float chance = CyberwareConfig.get(level).engineeringChance();
        if (!toDestroy.isEmpty() && toDestroy.isDamageableItem()) {
            chance = Math.min(100F, chance * 5F * (1F - toDestroy.getDamageValue() / (float) toDestroy.getMaxDamage()));
        }
        return chance;
    }

    /**
     * @return how many components are lost at random when destroying an undamaged item
     */
    public static int getComponentsLost(Difficulty difficulty) {
        return switch (difficulty) {
            case NORMAL, HARD -> 2;
            default -> 1;
        };
    }

    private List<ItemStack> getCraftingItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = SLOT_CRAFTING_START; slot < SLOT_CRAFTING_END; slot++) {
            items.add(slots.getStackInSlot(slot).copy());
        }
        return items;
    }

    /**
     * @return the recipe matching the blueprint and the crafting slots
     */
    private Optional<RecipeHolder<EngineeringRecipe>> findRecipe() {
        if (level == null || !(slots.getStackInSlot(SLOT_BLUEPRINT).getItem() instanceof Blueprint blueprint)) {
            return Optional.empty();
        }
        return EngineeringRecipe.find(level, blueprint.getTarget(slots.getStackInSlot(SLOT_BLUEPRINT)), getCraftingItems());
    }

    public void updateRecipe() {
        updatingRecipe = true;
        ItemStack result = findRecipe().map(recipe -> recipe.value().result().copy()).orElse(ItemStack.EMPTY);
        if (!result.isEmpty()) {
            result.setCount(1);
        }
        slots.setStackInSlot(SLOT_RESULT, result);
        updatingRecipe = false;
    }

    public void subtractResources() {
        EngineeringRecipe recipe = findRecipe().map(RecipeHolder::value)
                .orElseThrow(() -> new IllegalStateException("Tried to subtract resources without a matching recipe!"));
        List<ItemStack> remaining = recipe.consume(getCraftingItems());
        updatingRecipe = true;
        for (int index = 0; index < remaining.size(); index++) {
            slots.setStackInSlot(SLOT_CRAFTING_START + index, remaining.get(index));
        }
        updatingRecipe = false;
        updateRecipe();
    }

    /**
     * Destroys the item in the first slot into its components. Server side only.
     */
    public void smash() {
        if (level == null || level.isClientSide()) return;
        ItemStack toDestroy = slots.getStackInSlot(SLOT_DESTROY);
        if (!CyberwareAPI.canDeconstruct(toDestroy)) return;

        ItemStack paper = slots.getStackInSlot(SLOT_PAPER);
        boolean doBlueprint = !paper.isEmpty() && paper.is(CyberwareTags.PAPER);
        // the loot table gives all the components, some are lost below
        List<ItemStack> components = Deconstruction.rollComponents((ServerLevel) level, worldPosition, toDestroy, null);

        // pick the components that survive
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack component : components) {
            for (int count = 0; count < component.getCount(); count++) {
                drops.add(component.copyWithCount(1));
            }
        }
        int numToRemove = getComponentsLost(level.getDifficulty());
        if (toDestroy.isDamageableItem()) {
            // damaged items yield less
            float percent = toDestroy.getDamageValue() / (float) toDestroy.getMaxDamage();
            numToRemove += Math.max(0, (int) (drops.size() * percent) - 1);
        }
        numToRemove = Math.min(numToRemove, drops.size() - 1);
        for (int index = 0; index < numToRemove; index++) {
            drops.remove(level.random.nextInt(drops.size()));
        }

        // check that everything would fit
        ItemStackHandler simulation = new ItemStackHandler(6);
        for (int index = 0; index < 6; index++) {
            simulation.setStackInSlot(index, slots.getStackInSlot(SLOT_CRAFTING_START + index).copy());
        }
        List<ItemStack> toFit = new ArrayList<>(components);
        if (doBlueprint) {
            toFit.add(BlueprintItem.getBlueprintForItem(toDestroy));
        }
        for (ItemStack stack : toFit) {
            ItemStack left = stack.copy();
            for (int index = 0; index < 6 && !left.isEmpty(); index++) {
                left = simulation.insertItem(index, left, false);
            }
            if (!left.isEmpty()) return;
        }

        PacketDistributor.sendToPlayersNear((ServerLevel) level, null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 25,
                new EngineeringSmashPayload(worldPosition, toDestroy.copy()));

        if (doBlueprint && level.random.nextFloat() < getBlueprintChance(level, toDestroy) / 100F) {
            drops.add(BlueprintItem.getBlueprintForItem(toDestroy));
            slots.setStackInSlot(SLOT_PAPER, paper.copyWithCount(paper.getCount() - 1));
        }

        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            // prefer stacking onto existing stacks
            for (int slot = SLOT_CRAFTING_START; slot < SLOT_CRAFTING_END && !left.isEmpty(); slot++) {
                if (!slots.getStackInSlot(slot).isEmpty()) {
                    left = slots.insertItem(slot, left, false);
                }
            }
            for (int slot = SLOT_CRAFTING_START; slot < SLOT_CRAFTING_END && !left.isEmpty(); slot++) {
                left = slots.insertItem(slot, left, false);
            }
        }

        slots.setStackInSlot(SLOT_DESTROY, toDestroy.copyWithCount(toDestroy.getCount() - 1));
        updateRecipe();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EngineeringTableBlockEntity table) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.below())) {
            if (table.time == 0) {
                table.smash();
            }
            table.time = (table.time + 1) % 25;
        } else {
            table.time = 0;
        }
    }

    // ---- persistence ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.deserializeNBT(registries, tag.getCompound("inv"));
        time = tag.getInt("time");
        lastPlayerArchive.clear();
        ListTag archives = tag.getList("playerArchive", Tag.TAG_COMPOUND);
        for (int index = 0; index < archives.size(); index++) {
            CompoundTag entry = archives.getCompound(index);
            NbtUtils.readBlockPos(entry, "pos").ifPresent(pos -> lastPlayerArchive.put(entry.getString("name"), pos));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", slots.serializeNBT(registries));
        tag.putInt("time", time);
        ListTag archives = new ListTag();
        for (Map.Entry<String, BlockPos> entry : lastPlayerArchive.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("name", entry.getKey());
            entryTag.put("pos", NbtUtils.writeBlockPos(entry.getValue()));
            archives.add(entryTag);
        }
        tag.put("playerArchive", archives);
    }

    /**
     * The inventory as seen by hoppers and pipes, which may not take components while a blueprint is loaded
     * and can't take the paper or blueprint.
     */
    private class AutomationHandler implements IItemHandler {
        private final int[] mapping;

        private AutomationHandler(int[] mapping) {
            this.mapping = mapping;
        }

        @Override
        public int getSlots() {
            return mapping.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots.getStackInSlot(mapping[slot]);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int target = mapping[slot];
            if (!isItemValid(slot, stack)) return stack;
            return slots.insertItem(target, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int target = mapping[slot];
            boolean blueprintLoaded = !slots.getStackInSlot(SLOT_BLUEPRINT).isEmpty();
            if ((blueprintLoaded && target >= SLOT_CRAFTING_START && target < SLOT_CRAFTING_END)
                    || target == SLOT_PAPER || target == SLOT_BLUEPRINT) {
                return ItemStack.EMPTY;
            }
            return slots.extractItem(target, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slots.getSlotLimit(mapping[slot]);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int target = mapping[slot];
            if (target >= SLOT_CRAFTING_START && target < SLOT_CRAFTING_END) {
                // automation can't put cyberware into the crafting grid
                return !CyberwareAPI.canDeconstruct(stack);
            }
            return slots.isItemValid(target, stack);
        }
    }
}
