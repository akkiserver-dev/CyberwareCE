package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.network.ParticlePayload;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

public class HeartUpgradeItem extends CyberwareItem {
    private static final String USED_KEY = "used";

    private static final Map<LivingEntity, Integer> PLATELET_TIMES = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> PLATELET_WORKING = new WeakHashMap<>();
    private static final Map<LivingEntity, Boolean> STEM_CELL_WORKING = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> MEDKIT_TIMES = new WeakHashMap<>();
    private static final Map<LivingEntity, Float> MEDKIT_DAMAGE = new WeakHashMap<>();

    private final Kind kind;

    public HeartUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return other.is(CyberwareItems.CYBERHEART.get())
                && (kind == Kind.INTERNAL_DEFIBRILLATOR || kind == Kind.CARDIOVASCULAR_COUPLER);
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return switch (kind) {
            case INTERNAL_DEFIBRILLATOR -> CyberwareConstants.DEFIBRILLATOR_CONSUMPTION;
            case PLATELET_DISPATCHER -> CyberwareConstants.PLATELET_CONSUMPTION;
            case STEM_CELL_SYNTHESIZER -> CyberwareConstants.STEMCELL_CONSUMPTION;
            default -> 0;
        };
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return kind == Kind.INTERNAL_DEFIBRILLATOR ? CyberwareConstants.DEFIBRILLATOR_CONSUMPTION : 0;
    }

    @Override
    public boolean hasCustomPowerMessage(ItemStack stack) {
        return kind == Kind.INTERNAL_DEFIBRILLATOR;
    }

    @Override
    public int getPowerProduction(ItemStack stack) {
        return kind == Kind.CARDIOVASCULAR_COUPLER ? CyberwareConstants.COUPLER_PRODUCTION + 1 : 0;
    }

    @SubscribeEvent
    public static void handleDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null) return;

        HeartUpgradeItem defibrillatorItem = CyberwareItems.INTERNAL_DEFIBRILLATOR.get();
        ItemStack defibrillator = data.getCyberware(defibrillatorItem);
        if (defibrillator.isEmpty()
                || CyberwareAPI.getFunctionData(defibrillator).contains(USED_KEY)
                || !data.usePower(defibrillator, defibrillatorItem.getPowerConsumption(defibrillator), false)) {
            return;
        }

        if (entity instanceof Player) {
            // players lose the defibrillator once used
            NonNullList<ItemStack> installed = data.getInstalledCyberware(BodyRegion.HEART);
            NonNullList<ItemStack> remaining = NonNullList.create();
            remaining.addAll(installed);
            for (int index = 0; index < installed.size(); index++) {
                if (installed.get(index).is(defibrillatorItem)) {
                    remaining.set(index, ItemStack.EMPTY);
                    break;
                }
            }
            data.setInstalledCyberware(entity, BodyRegion.HEART, remaining);
            data.updateCapacity();
        } else {
            CyberwareAPI.updateFunctionData(defibrillator, tag -> tag.putBoolean(USED_KEY, true));
        }
        CyberwareAPI.updateData(entity);

        entity.setHealth(entity.getMaxHealth() / 3F);
        PacketDistributor.sendToPlayersNear((ServerLevel) entity.level(), null, entity.getX(), entity.getY(), entity.getZ(), 20,
                new ParticlePayload(ParticlePayload.DEFIBRILLATOR, (float) entity.getX(), (float) (entity.getY() + entity.getBbHeight() / 2F), (float) entity.getZ()));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        boolean isSecond = entity.tickCount % 20 == 0;

        if (isSecond) {
            ItemStack coupler = data.getCyberware(CyberwareItems.CARDIOVASCULAR_COUPLER.get());
            if (!coupler.isEmpty()) {
                data.addPower(CyberwareItems.CARDIOVASCULAR_COUPLER.get().getPowerProduction(coupler), coupler);
            }
        }

        ItemStack stemCells = data.getCyberware(CyberwareItems.STEM_CELL_SYNTHESIZER.get());
        if (isSecond && !stemCells.isEmpty()) {
            STEM_CELL_WORKING.put(entity, data.usePower(stemCells, CyberwareConstants.STEMCELL_CONSUMPTION));
        }

        ItemStack platelets = data.getCyberware(CyberwareItems.PLATELET_DISPATCHER.get());
        if (isSecond && !platelets.isEmpty()) {
            PLATELET_WORKING.put(entity, data.usePower(platelets, CyberwareConstants.PLATELET_CONSUMPTION));
        }

        if (!platelets.isEmpty() && PLATELET_WORKING.getOrDefault(entity, false)) {
            if (entity.getHealth() >= entity.getMaxHealth() * .8F && entity.getHealth() != entity.getMaxHealth()) {
                int start = PLATELET_TIMES.computeIfAbsent(entity, e -> e.tickCount);
                if (entity.tickCount - start >= 40) {
                    PLATELET_TIMES.put(entity, entity.tickCount);
                    entity.heal(1);
                }
            } else {
                PLATELET_TIMES.put(entity, entity.tickCount);
            }
        } else {
            PLATELET_TIMES.remove(entity);
        }

        if (!stemCells.isEmpty() && STEM_CELL_WORKING.getOrDefault(entity, false)) {
            int start = MEDKIT_TIMES.computeIfAbsent(entity, e -> {
                MEDKIT_DAMAGE.put(e, 0F);
                return e.tickCount;
            });
            float damage = MEDKIT_DAMAGE.getOrDefault(entity, 0F);
            if (entity.tickCount - start >= 100 && damage > 0F) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    PacketDistributor.sendToPlayersNear(serverLevel, null, entity.getX(), entity.getY(), entity.getZ(), 20,
                            new ParticlePayload(ParticlePayload.HEAL, (float) entity.getX(), (float) (entity.getY() + entity.getBbHeight() / 2F), (float) entity.getZ()));
                }
                entity.heal(damage);
                MEDKIT_TIMES.put(entity, 0);
                MEDKIT_DAMAGE.put(entity, 0F);
            }
        }
    }

    @SubscribeEvent
    public static void handleHurt(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = CyberwareAPI.getUserData(entity);
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.STEM_CELL_SYNTHESIZER.get())) return;

        // remember the damage that actually reached the health bar, it will be healed after 5 seconds without damage
        MEDKIT_DAMAGE.put(entity, event.getNewDamage());
        MEDKIT_TIMES.put(entity, entity.tickCount);
    }

    public enum Kind {
        INTERNAL_DEFIBRILLATOR,
        PLATELET_DISPATCHER,
        STEM_CELL_SYNTHESIZER,
        CARDIOVASCULAR_COUPLER
    }
}
