package flaxbeard.cyberware.common.entity;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.common.AttributeHelper;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.handler.CyberwareDataHandler;
import flaxbeard.cyberware.common.item.BodyPartItem;
import flaxbeard.cyberware.common.item.CreativeBatteryItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A zombie with random (scavenged) cyberware installed. Brutes are bigger and stronger and have more cyberware.
 */
public class CyberZombie extends Zombie {
    private static final EntityDataAccessor<Boolean> BRUTE = SynchedEntityData.defineId(CyberZombie.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation BRUTE_BONUS = CyberwareAPI.id("brute_bonus");

    public boolean hasRandomWare = false;

    public CyberZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BRUTE, false);
    }

    public boolean isBrute() {
        return entityData.get(BRUTE);
    }

    /**
     * @return whether the zombie didn't get its cyberware yet (brutes get more)
     */
    public boolean setBrute() {
        setBaby(false);
        entityData.set(BRUTE, true);
        return !hasRandomWare;
    }

    @Override
    public void aiStep() {
        if (!hasRandomWare && !level().isClientSide()) {
            if (!isBrute() && random.nextFloat() < CyberwareConstants.NATURAL_BRUTE_CHANCE / 100F) {
                setBrute();
            }
            CyberwareDataHandler.addRandomCyberware(this, isBrute());
            if (isBrute()) {
                AttributeHelper.applyPermanent(this, Attributes.MAX_HEALTH, BRUTE_BONUS, 6.0);
                AttributeHelper.applyPermanent(this, Attributes.ATTACK_DAMAGE, BRUTE_BONUS, 1.0);
                // brutes are 20% bigger
                AttributeHelper.applyPermanent(this, Attributes.SCALE, BRUTE_BONUS, 0.2);
            }
            setHealth(getMaxHealth());
            hasRandomWare = true;
        }
        super.aiStep();
    }

    @Override
    protected boolean convertsInWater() {
        // drowning would lose the cyberware
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("hasRandomWare", hasRandomWare);
        tag.putBoolean("brute", isBrute());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("brute")) {
            setBrute();
        }
        hasRandomWare = tag.getBoolean("hasRandomWare");
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        if (CyberwareConfig.ENABLE_KATANA.get() && CyberwareConfig.MOBS_ADD_CLOTHES.get()
                && getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD)) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(CyberwareItems.KATANA.get()));
            setDropChance(EquipmentSlot.MAINHAND, 0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        ItemStack mainHand = getItemBySlot(EquipmentSlot.MAINHAND);
        if (CyberwareConfig.ENABLE_KATANA.get() && CyberwareConfig.MOBS_ADD_CLOTHES.get() && mainHand.is(CyberwareItems.KATANA.get())) {
            ItemStack katana = mainHand.copy();
            if (katana.isDamageableItem()) {
                int maxDamage = Math.max(katana.getMaxDamage() - 25, 1);
                int damage = katana.getMaxDamage() - random.nextInt(random.nextInt(maxDamage) + 1);
                katana.setDamageValue(Math.max(1, Math.min(damage, maxDamage)));
            }
            spawnAtLocation(katana);
        }

        if (!hasRandomWare) return;
        int looting = 0;
        if (damageSource.getEntity() instanceof LivingEntity killer) {
            looting = EnchantmentHelper.getEnchantmentLevel(
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), killer);
        }
        float rarity = (float) Math.min(100.0, CyberwareConfig.MOBS_CYBER_ZOMBIE_DROP_RARITY.get() + looting * 5.0);
        if (random.nextFloat() >= rarity / 100F) return;

        CyberwareUserData data = CyberwareAPI.getUserData(this);
        if (data == null) return;
        List<ItemStack> wares = new ArrayList<>();
        for (BodyRegion region : BodyRegion.values()) {
            for (ItemStack stack : data.getInstalledCyberware(region)) {
                // never drop body parts or the infinite battery
                if (!stack.isEmpty() && !(stack.getItem() instanceof CreativeBatteryItem) && !(stack.getItem() instanceof BodyPartItem)) {
                    wares.add(stack);
                }
            }
        }
        if (wares.isEmpty()) {
            CyberwareMod.LOGGER.debug("CyberZombie {} has no cyberware to drop", this);
            return;
        }

        ItemStack drop = CyberwareAPI.sanitize(wares.get(random.nextInt(wares.size())).copyWithCount(1));
        drop = CyberwareAPI.getCyberware(drop).setQuality(drop, CyberwareAPI.QUALITY_SCAVENGED);
        spawnAtLocation(drop);
    }
}
