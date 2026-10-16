package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.item.BlueprintItem;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

import javax.annotation.Nullable;

/**
 * Slowly scans an item to produce its blueprint. Slots: 0 item to scan, 1 paper, 2 resulting blueprint.
 */
public class ScannerBlockEntity extends NameableBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_PAPER = 1;
    public static final int SLOT_OUTPUT = 2;

    public final ItemStackHandler slots = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markForSync();
        }
    };
    private final IItemHandler topAndSides = new RangedWrapper(slots, 0, 2);
    private final IItemHandler bottomOutput = new RangedWrapper(slots, 2, 3);
    private final IItemHandler bottomInput = new RangedWrapper(slots, 0, 1);

    public int ticks = 0;
    public int ticksMove = 0;
    public int lastX = 0;
    public int x = 0;
    public int lastZ = 0;
    public int z = 0;

    public ScannerBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.SCANNER.get(), pos, state, "cyberware.container.scanner");
    }

    public static boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> CyberwareAPI.canDeconstruct(stack);
            case SLOT_PAPER -> stack.is(CyberwareTags.PAPER);
            default -> false;
        };
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == Direction.DOWN) {
            // once a blueprint is done, the scanned item can be taken out from below as well
            return !slots.getStackInSlot(SLOT_OUTPUT).isEmpty() && !slots.getStackInSlot(SLOT_INPUT).isEmpty() ? bottomInput : bottomOutput;
        }
        return topAndSides;
    }

    /**
     * @return the chance in percent of creating a blueprint of the current input
     */
    public static float getChance(@Nullable Level level, ItemStack toScan) {
        if (toScan.isEmpty()) return 0F;
        CyberwareConfig.Synced config = CyberwareConfig.get(level);
        float chance = config.scannerChance() + config.scannerChanceAdditional() * (toScan.getCount() - 1);
        if (toScan.isDamageableItem()) {
            chance = 50F * (1F - toScan.getDamageValue() / (float) toScan.getMaxDamage());
        }
        return Math.min(chance, 50F);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ScannerBlockEntity scanner) {
        scanner.tick();
    }

    private void tick() {
        ItemStack toScan = slots.getStackInSlot(SLOT_INPUT);
        if (CyberwareAPI.canDeconstruct(toScan) && slots.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            ticks++;

            // move the scanning head around
            if (ticksMove > ticks || ticks - ticksMove > Math.max(Math.abs(lastX - x) * 3, Math.abs(lastZ - z) * 3) + 10) {
                ticksMove = ticks;
                lastX = x;
                lastZ = z;
                while (x == lastX) {
                    x = level.random.nextInt(11);
                }
                while (z == lastZ) {
                    z = level.random.nextInt(11);
                }
            }

            if (ticks > CyberwareConfig.get(level).scannerTime()) {
                ticks = 0;
                ticksMove = 0;

                ItemStack paper = slots.getStackInSlot(SLOT_PAPER);
                if (!level.isClientSide() && !paper.isEmpty() && level.random.nextFloat() < getChance(level, toScan) / 100F) {
                    slots.setStackInSlot(SLOT_OUTPUT, BlueprintItem.getBlueprintForItem(toScan));
                    ItemStack remainingPaper = paper.copy();
                    remainingPaper.shrink(1);
                    slots.setStackInSlot(SLOT_PAPER, remainingPaper);
                }
            }
            setChanged();
        } else {
            x = lastX = z = lastZ = 0;
            if (ticks != 0) {
                ticks = 0;
                setChanged();
            }
        }
    }

    public float getProgress() {
        return ticks / (float) Math.max(1, CyberwareConfig.get(level).scannerTime());
    }

    // ---- persistence ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slots.deserializeNBT(registries, tag.getCompound("inv"));
        ticks = tag.getInt("ticks");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", slots.serializeNBT(registries));
        tag.putInt("ticks", ticks);
    }
}
