package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.RadioBeaconBlock;
import flaxbeard.cyberware.common.block.RadioPostBlock;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Radio Beacon, a tier 3 transmitter that only works on top of a complete Radio Tower.
 */
public class RadioBeaconBlockEntity extends RadioBlockEntity {
    public RadioBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(CyberwareBlockEntities.RADIO_BEACON.get(), pos, state);
    }

    @Override
    protected int getTier() {
        return RadioNetwork.TIER_BEACON;
    }

    @Override
    protected int getRange() {
        return CyberwareConstants.LARGE_BEACON_RANGE;
    }

    @Override
    protected boolean isWorking() {
        if (level == null) return false;
        BlockPos masterPos = worldPosition.offset(0, -RadioPostBlock.TOWER_HEIGHT, 0);
        BlockState master = level.getBlockState(masterPos);
        boolean powered = level.hasNeighborSignal(masterPos.east())
                || level.hasNeighborSignal(masterPos.west())
                || level.hasNeighborSignal(masterPos.south())
                || level.hasNeighborSignal(masterPos.north());
        return !powered && master.is(CyberwareBlocks.RADIO_POST.get()) && master.getValue(RadioPostBlock.TRANSFORMED) == RadioPostBlock.MASTER;
    }

    @Override
    protected void spawnParticles(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(RadioBeaconBlock.FACING)) return;
        Direction facing = state.getValue(RadioBeaconBlock.FACING);
        boolean eastWest = facing == Direction.EAST || facing == Direction.WEST;
        float dist = .5F;
        float speed = .2F;
        int degrees = 45;
        for (int index = 0; index < 18; index++) {
            float sin = (float) Math.sin(Math.toRadians(degrees));
            float cos = (float) Math.cos(Math.toRadians(degrees));
            float xOffset = dist * sin;
            float yOffset = .2F + dist * cos;
            float xSpeed = speed * sin;
            float ySpeed = speed * cos;
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + .5F + (eastWest ? xOffset : 0), pos.getY() + .5F + yOffset, pos.getZ() + .5F + (eastWest ? 0 : xOffset),
                    eastWest ? xSpeed : 0, ySpeed, eastWest ? 0 : xSpeed);
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + .5F - (eastWest ? xOffset : 0), pos.getY() + .5F + yOffset, pos.getZ() + .5F - (eastWest ? 0 : xOffset),
                    eastWest ? -xSpeed : 0, ySpeed, eastWest ? 0 : -xSpeed);
            degrees += 5;
        }
    }
}
