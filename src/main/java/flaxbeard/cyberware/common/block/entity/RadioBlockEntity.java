package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.RadioBlock;
import flaxbeard.cyberware.common.registry.CyberwareBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Radio Kit, a tier 2 transmitter that works while it isn't powered by redstone.
 */
public class RadioBlockEntity extends BlockEntity {
    private boolean wasWorking = false;
    private int count = 0;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        this(CyberwareBlockEntities.RADIO.get(), pos, state);
    }

    protected RadioBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected int getTier() {
        return RadioNetwork.TIER_RADIO;
    }

    protected int getRange() {
        return CyberwareConstants.BEACON_RANGE;
    }

    protected boolean isWorking() {
        return level != null && !level.hasNeighborSignal(worldPosition);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBlockEntity radio) {
        boolean working = radio.isWorking();
        if (working && !radio.wasWorking) {
            RadioNetwork.enable(level, pos, radio.getTier(), radio.getRange());
        } else if (!working && radio.wasWorking) {
            RadioNetwork.disable(level, pos, radio.getTier());
        }
        radio.wasWorking = working;

        if (level.isClientSide() && working) {
            radio.count = (radio.count + 1) % 20;
            if (radio.count == 0) {
                radio.spawnParticles(level, pos, state);
            }
        }
    }

    protected void spawnParticles(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(RadioBlock.FACING)) return;
        Direction facing = state.getValue(RadioBlock.FACING);
        boolean northSouth = facing == Direction.NORTH || facing == Direction.SOUTH;
        boolean backwards = facing == Direction.SOUTH || facing == Direction.EAST;
        float dist = .2F;
        float speed = .08F;
        int degrees = 45;
        for (int index = 0; index < 5; index++) {
            float sin = (float) Math.sin(Math.toRadians(degrees));
            float cos = (float) Math.cos(Math.toRadians(degrees));
            float xOffset = dist * sin;
            float yOffset = .2F + dist * cos;
            float xSpeed = speed * sin;
            float ySpeed = speed * cos;
            float backOffsetX = backwards ^ northSouth ? -.3F : .3F;
            float backOffsetZ = backwards ? .4F : -.4F;

            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + .5F + (northSouth ? xOffset + backOffsetX : backOffsetZ),
                    pos.getY() + .5F + yOffset,
                    pos.getZ() + .5F + (northSouth ? backOffsetZ : xOffset + backOffsetX),
                    northSouth ? xSpeed : 0, ySpeed, northSouth ? 0 : xSpeed);
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + .5F + (northSouth ? -xOffset + backOffsetX : backOffsetZ),
                    pos.getY() + .5F + yOffset,
                    pos.getZ() + .5F + (northSouth ? backOffsetZ : -xOffset + backOffsetX),
                    northSouth ? -xSpeed : 0, ySpeed, northSouth ? 0 : -xSpeed);
            degrees += 18;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            RadioNetwork.disable(level, worldPosition, getTier());
        }
        super.setRemoved();
    }
}
