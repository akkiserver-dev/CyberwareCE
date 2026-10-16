package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Remembers the master block of the Radio Tower a post is part of.
 */
public class RadioPostBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos master = null;
    /** Set while the tower is being taken apart, to avoid recursion. */
    public boolean destructing = false;

    public RadioPostBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.RADIO_POST.get(), pos, state);
    }

    @Nullable
    public BlockPos getMaster() {
        return master;
    }

    public void setMaster(@Nullable BlockPos master) {
        this.master = master;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        master = NbtUtils.readBlockPos(tag, "master").orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (master != null) {
            tag.put("master", NbtUtils.writeBlockPos(master));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
