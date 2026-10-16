package flaxbeard.cyberware.client;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.hud.NotificationInstance;
import flaxbeard.cyberware.client.gui.hud.NotificationDisplay;
import flaxbeard.cyberware.client.handler.CyberwareClientEffects;
import flaxbeard.cyberware.client.handler.HudHandler;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.network.CyberlegFallPayload;
import flaxbeard.cyberware.common.network.CyberwareSyncPayload;
import flaxbeard.cyberware.common.network.DodgePayload;
import flaxbeard.cyberware.common.network.EngineeringSmashPayload;
import flaxbeard.cyberware.common.network.ParticlePayload;
import flaxbeard.cyberware.common.network.SwitchHeldItemPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Handles the payloads sent by the server.
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleCyberwareSync(CyberwareSyncPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity entity = level.getEntity(payload.entityId());
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null) return;
        data.deserializeNBT(level.registryAccess(), payload.data());
        if (entity == Minecraft.getInstance().player) {
            HudHandler.loadHudLayout(data.getHudData());
        }
    }

    public static void handleDodge(DodgePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (entity == null) return;
        RandomSource random = entity.getRandom();
        for (int index = 0; index < 25; index++) {
            minecraft.level.addParticle(ParticleTypes.EFFECT, entity.getX(), entity.getY() + random.nextFloat() * entity.getBbHeight(), entity.getZ(),
                    (random.nextFloat() - .5F) * .2F, 0, (random.nextFloat() - .5F) * .2F);
        }
        entity.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1F, 1F);
        if (entity == minecraft.player) {
            HudHandler.addNotification(new NotificationInstance(entity.tickCount, new NotificationDisplay.DodgeNotification()));
        }
    }

    public static void handleParticles(ParticlePayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        ParticleOptions particle = payload.effect() == ParticlePayload.HEAL ? ParticleTypes.HEART : ParticleTypes.ANGRY_VILLAGER;
        RandomSource random = level.getRandom();
        for (int index = 0; index < 5; index++) {
            level.addParticle(particle,
                    payload.x() + random.nextFloat() - 0.5F, payload.y() + random.nextFloat() - 0.5F, payload.z() + random.nextFloat() - 0.5F,
                    2.0F * (random.nextFloat() - 0.5F), 0.5F, 2.0F * (random.nextFloat() - 0.5F));
        }
    }

    public static void handleSwitchHeldItem(SwitchHeldItemPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(payload.entityId()) instanceof Player player)) return;
        player.getInventory().selected = payload.slot();
        if (payload.attackerId() != -1) {
            if (player == minecraft.player) {
                minecraft.player.closeContainer();
            }
            Entity attacker = minecraft.level.getEntity(payload.attackerId());
            if (attacker != null) {
                faceEntity(player, attacker);
            }
        }
    }

    private static void faceEntity(Player player, Entity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = target instanceof LivingEntity living
                ? living.getEyeY() - player.getEyeY()
                : (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0D - player.getEyeY();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        player.setXRot((float) -(Mth.atan2(dy, horizontal) * (180D / Math.PI)));
        player.setYRot((float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90.0F);
    }

    public static void handleEngineeringSmash(EngineeringSmashPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;
        BlockPos pos = payload.pos();
        if (level.getBlockEntity(pos) instanceof EngineeringTableBlockEntity table) {
            table.clickedTime = minecraft.player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        }
        level.playLocalSound(pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1F, 1F, false);
        level.playLocalSound(pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, .5F, false);
        if (!payload.stack().isEmpty()) {
            RandomSource random = level.getRandom();
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, payload.stack());
            for (int index = 0; index < 10; index++) {
                level.addParticle(particle, pos.getX() + .5F, pos.getY(), pos.getZ() + .5F,
                        .25F * (random.nextFloat() - .5F), .1F, .25F * (random.nextFloat() - .5F));
            }
        }
    }

    public static void handleCyberlegFall(CyberlegFallPayload payload) {
        CyberwareClientEffects.markCyberlegFall(payload.entityId());
    }
}
