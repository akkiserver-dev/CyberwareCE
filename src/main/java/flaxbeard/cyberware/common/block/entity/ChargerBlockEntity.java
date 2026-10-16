package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Charges the cyberware of entities standing on it, using Forge Energy.
 */
public class ChargerBlockEntity extends BlockEntity {
    private static final int CAPACITY = 5000;
    private static final int TRANSFER_RATE = 50;
    private static final int POWER_PER_TICK = 20;

    private final EnergyStorage energy = new EnergyStorage(CAPACITY, TRANSFER_RATE, TRANSFER_RATE) {
        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }
    };
    private boolean lastHadPower = false;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.CHARGER.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity charger) {
        int cost = CyberwareConfig.FE_PER_POWER.get();
        AABB area = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1F, pos.getY() + 2.5F, pos.getZ() + 1F);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            CyberwareUserData data = CyberwareAPI.getUserData(entity);
            if (data == null || data.isAtCapacity(ItemStack.EMPTY, POWER_PER_TICK) || charger.energy.getEnergyStored() < cost) {
                continue;
            }
            if (!level.isClientSide()) {
                charger.energy.extractEnergy(cost, false);
                charger.setChanged();
            }
            data.addPower(POWER_PER_TICK, ItemStack.EMPTY);

            if (level.isClientSide() && entity.tickCount % 5 == 0) {
                double x = pos.getX() + .5F;
                double y = pos.getY() + 1F;
                double z = pos.getZ() + .5F;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0F, .05F, 0F);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, .04F, .05F, .04F);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, -.04F, .05F, .04F);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, .04F, .05F, -.04F);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, -.04F, .05F, -.04F);
            }
        }

        // keep the client informed about whether there is power, so it can predict the charging
        boolean hasPower = charger.energy.getEnergyStored() >= cost;
        if (hasPower != charger.lastHadPower && !level.isClientSide()) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            charger.lastHadPower = hasPower;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energy.deserializeNBT(registries, tag.get("energy"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
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
