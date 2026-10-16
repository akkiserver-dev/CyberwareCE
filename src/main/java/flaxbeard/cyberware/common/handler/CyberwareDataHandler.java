package flaxbeard.cyberware.common.handler;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.entity.RadioNetwork;
import flaxbeard.cyberware.common.entity.CyberZombie;
import flaxbeard.cyberware.common.item.CyberwareItem;
import flaxbeard.cyberware.common.network.SyncConfigPayload;
import flaxbeard.cyberware.common.registry.CyberwareAttributes;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.common.registry.CyberwareEntities;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the cyberware data of entities in sync, handles death and respawn, and turns zombies into CyberZombies.
 */
public final class CyberwareDataHandler {
    public static GameRules.Key<GameRules.BooleanValue> KEEP_WARE_GAMERULE;
    public static GameRules.Key<GameRules.BooleanValue> DROP_WARE_GAMERULE;

    /** How many pieces of random cyberware a CyberZombie gets. */
    private static final List<WeightedEntry.Wrapper<Integer>> ITEM_COUNTS = List.of(
            WeightedEntry.wrap(4, 50),
            WeightedEntry.wrap(3, 25),
            WeightedEntry.wrap(5, 25),
            WeightedEntry.wrap(6, 15),
            WeightedEntry.wrap(10, 5));

    private CyberwareDataHandler() {
    }

    /**
     * Registers the game rules; called once the config is loaded since it provides their defaults.
     */
    public static void registerGameRules() {
        KEEP_WARE_GAMERULE = GameRules.register("keepCyberware", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(CyberwareConfig.DEFAULT_KEEP.get()));
        DROP_WARE_GAMERULE = GameRules.register("dropCyberware", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(CyberwareConfig.DEFAULT_DROP.get()));
    }

    // ---- sync ----

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)) return;
        AttributeInstance tolerance = entity.getAttribute(CyberwareAttributes.TOLERANCE);
        if (tolerance != null) {
            tolerance.setBaseValue(CyberwareConfig.ESSENCE.get());
        }
        if (entity instanceof ServerPlayer player) {
            CyberwareAPI.sendDataTo(player, player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && CyberwareAPI.canHaveCyberware(event.getTarget())) {
            CyberwareAPI.sendDataTo(event.getTarget(), player);
        }
    }

    // ---- death ----

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new SyncConfigPayload(CyberwareConfig.Synced.fromConfig()));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        boolean keep = clone.level().getGameRules().getBoolean(KEEP_WARE_GAMERULE);
        if (event.isWasDeath() && !keep) return;

        CyberwareUserData originalData = CyberwareAPI.getUserData(original);
        CyberwareUserData cloneData = CyberwareAPI.getUserData(clone);
        if (originalData != null && cloneData != null) {
            cloneData.deserializeNBT(clone.registryAccess(), originalData.serializeNBT(original.registryAccess()));
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        GameRules rules = player.level().getGameRules();
        boolean keep = rules.getBoolean(KEEP_WARE_GAMERULE);
        boolean drop = rules.getBoolean(DROP_WARE_GAMERULE);

        boolean shouldDrop = drop && !keep;
        boolean shouldDropOnEssentialLoss = keep && CyberwareDamageTypes.isMissingEssential(event.getSource());

        if (!shouldDrop && !shouldDropOnEssentialLoss) return;

        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null) return;
        for (BodyRegion region : BodyRegion.values()) {
            NonNullList<ItemStack> defaults = CyberwareConfig.getStartingItems(region);
            for (ItemStack installed : data.getInstalledCyberware(region)) {
                if (installed.isEmpty()) continue;
                // the default body parts aren't dropped
                ItemStack toDrop = installed.copy();
                boolean isDefault = false;
                for (ItemStack defaultStack : defaults) {
                    if (CyberwareAPI.areCyberwareStacksEqual(defaultStack, toDrop)) {
                        if (toDrop.getCount() > defaultStack.getCount()) {
                            toDrop.shrink(defaultStack.getCount());
                        } else {
                            isDefault = true;
                        }
                    }
                }
                if (!isDefault && player.getRandom().nextFloat() < CyberwareConfig.DROP_CHANCE.get() / 100F) {
                    ItemStack stack = CyberwareAPI.sanitize(toDrop);
                    if (player.getRandom().nextFloat() < CyberwareConfig.SCAVENGED_CHANCE.get() / 100F) {
                        CyberwareAPI.writeQuality(stack, CyberwareAPI.QUALITY_SCAVENGED);
                    }
                    event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
                }
            }
        }
        data.resetWare(player);
    }

    // ---- CyberZombies ----

    private static boolean isZombie(Entity entity) {
        if (entity instanceof ZombifiedPiglin) return false;
        return entity instanceof Zombie || BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().contains("zombie" /* Old code was using "ombie" to check, but its typo? ill collect it here... */);
    }

    private static boolean isNaturalSpawn(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.SPAWNER || type == MobSpawnType.CHUNK_GENERATION || type == MobSpawnType.TRIAL_SPAWNER;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!isZombie(mob) || event.isSpawnCancelled()) return;
        ServerLevelAccessor levelAccessor = event.getLevel();
        ServerLevel level = levelAccessor.getLevel();

        if (CyberwareConfig.MOBS_ENABLE_CYBER_ZOMBIES.get()
                && !(mob instanceof CyberZombie)
                && isNaturalSpawn(event.getSpawnType())
                && (!CyberwareConfig.MOBS_APPLY_DIMENSION_TO_BEACON.get() || CyberwareConfig.isValidDimension(level.dimension().location()))) {
            int tier = RadioNetwork.getTierInRange(level, event.getX(), event.getY(), event.getZ());
            if (tier > 0) {
                float chance = switch (tier) {
                    case RadioNetwork.TIER_RADIO -> CyberwareConstants.BEACON_CHANCE;
                    case RadioNetwork.TIER_PLAYER -> CyberwareConstants.BEACON_CHANCE_INTERNAL;
                    default -> CyberwareConstants.LARGE_BEACON_CHANCE;
                };
                RandomSource random = level.getRandom();
                if (random.nextFloat() < chance / 100F) {
                    CyberZombie cyberZombie = CyberwareEntities.CYBER_ZOMBIE.get().create(level);
                    if (cyberZombie != null) {
                        if (random.nextFloat() < CyberwareConstants.BEACON_BRUTE_CHANCE / 100F) {
                            cyberZombie.setBrute();
                        }
                        cyberZombie.moveTo(event.getX(), event.getY(), event.getZ(), mob.getYRot(), mob.getXRot());
                        cyberZombie.finalizeSpawn(levelAccessor, event.getDifficulty(), event.getSpawnType(), null);
                        for (EquipmentSlot slot : EquipmentSlot.values()) {
                            if (cyberZombie.getItemBySlot(slot).isEmpty()) {
                                cyberZombie.setItemSlot(slot, mob.getItemBySlot(slot));
                            }
                        }
                        addClothes(cyberZombie, random);
                        levelAccessor.addFreshEntityWithPassengers(cyberZombie);
                        event.setSpawnCancelled(true);
                        return;
                    }
                }
            }
        }
        addClothes(mob, level.getRandom());
    }

    private static void addClothes(Mob mob, RandomSource random) {
        if (!CyberwareConfig.ENABLE_CLOTHES.get() || !CyberwareConfig.MOBS_ADD_CLOTHES.get()) return;
        float dropChance = (float) (CyberwareConfig.MOBS_CLOTH_DROP_RARITY.get() / 100F);

        if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && random.nextFloat() < CyberwareConstants.ZOMBIE_SHADES_CHANCE / 100F) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(random.nextBoolean() ? CyberwareItems.SHADES.get() : CyberwareItems.SHADES2.get()));
            mob.setDropChance(EquipmentSlot.HEAD, dropChance);
        }

        float chestRandom = random.nextFloat();
        if (mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            if (chestRandom < CyberwareConstants.ZOMBIE_TRENCH_CHANCE / 100F) {
                ItemStack coat = new ItemStack(CyberwareItems.TRENCH_COAT.get());
                int color = random.nextInt(3);
                if (color == 0) {
                    coat.set(DataComponents.DYED_COLOR, new DyedItemColor(0x664028, true));
                } else if (color == 1) {
                    coat.set(DataComponents.DYED_COLOR, new DyedItemColor(0xEAEAEA, true));
                }
                mob.setItemSlot(EquipmentSlot.CHEST, coat);
                mob.setDropChance(EquipmentSlot.CHEST, dropChance);
            } else if (chestRandom - CyberwareConstants.ZOMBIE_TRENCH_CHANCE / 100F < CyberwareConstants.ZOMBIE_BIKER_CHANCE / 100F) {
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(CyberwareItems.JACKET.get()));
                mob.setDropChance(EquipmentSlot.CHEST, dropChance);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !CyberwareConfig.MOBS_APPLY_DIMENSION_TO_SPAWNING.get()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || CyberwareConfig.isValidDimension(level.dimension().location())) return;

        for (MobSpawnSettings.SpawnerData spawner : List.copyOf(event.getSpawnerDataList())) {
            if (spawner.type == CyberwareEntities.CYBER_ZOMBIE.get()) {
                event.removeSpawnerData(spawner);
            }
        }
    }

    /**
     * Installs random scavenged cyberware into a CyberZombie, along with everything it requires.
     */
    public static void addRandomCyberware(CyberZombie cyberZombie, boolean brute) {
        CyberwareUserData data = CyberwareAPI.getUserData(cyberZombie);
        if (data == null) return;
        RandomSource random = cyberZombie.getRandom();

        List<List<ItemStack>> wares = new ArrayList<>();
        for (BodyRegion region : BodyRegion.values()) {
            List<ItemStack> installed = new ArrayList<>();
            for (ItemStack stack : data.getInstalledCyberware(region)) {
                if (!stack.isEmpty()) installed.add(stack);
            }
            wares.add(installed);
        }

        // CyberZombies get all the power
        ItemStack battery = new ItemStack(CyberwareItems.CREATIVE_BATTERY.get());
        wares.get(CyberwareItems.CREATIVE_BATTERY.get().getBodyRegion(battery).ordinal()).add(battery);

        int toInstall = WeightedRandom.getRandomItem(random, ITEM_COUNTS).map(WeightedEntry.Wrapper::data).orElse(4);
        if (brute) {
            toInstall += CyberwareConstants.MORE_ITEMS_BRUTE;
        }

        List<WeightedEntry.Wrapper<ItemStack>> candidates = getZombieItems();
        if (candidates.isEmpty()) return;
        for (int installed = 0; installed < toInstall; installed++) {
            int tries = 0;
            ItemStack randomItem;
            Cyberware randomWare;
            // ensure we get a unique item
            do {
                randomItem = WeightedRandom.getRandomItem(random, candidates).orElseThrow().data().copy();
                randomWare = CyberwareAPI.getCyberware(randomItem);
                randomItem.setCount(randomWare.installedStackSize(randomItem));
                tries++;
            } while (contains(wares.get(randomWare.getBodyRegion(randomItem).ordinal()), randomItem) && tries < 10);

            if (tries >= 10) continue;

            // fulfill the requirements
            for (List<ItemStack> alternatives : randomWare.required(randomItem)) {
                boolean found = false;
                for (ItemStack option : alternatives) {
                    Cyberware optionWare = CyberwareAPI.getCyberware(option);
                    if (contains(wares.get(optionWare.getBodyRegion(option).ordinal()), option)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    ItemStack required = alternatives.get(random.nextInt(alternatives.size())).copy();
                    Cyberware requiredWare = CyberwareAPI.getCyberware(required);
                    required.setCount(requiredWare.installedStackSize(required));
                    wares.get(requiredWare.getBodyRegion(required).ordinal()).add(required);
                    installed++;
                }
            }
            wares.get(randomWare.getBodyRegion(randomItem).ordinal()).add(randomItem);
        }

        for (BodyRegion region : BodyRegion.values()) {
            data.setInstalledCyberware(cyberZombie, region, wares.get(region.ordinal()));
        }
        data.updateCapacity();
        cyberZombie.setHealth(cyberZombie.getMaxHealth());
        cyberZombie.hasRandomWare = true;
        CyberwareAPI.updateData(cyberZombie);
    }

    private static List<WeightedEntry.Wrapper<ItemStack>> getZombieItems() {
        List<WeightedEntry.Wrapper<ItemStack>> items = new ArrayList<>();
        for (var holder : CyberwareItems.getCyberware()) {
            if (holder.get() instanceof CyberwareItem item && item.getZombieWeight() > 0) {
                ItemStack stack = new ItemStack(item);
                stack.setCount(item.installedStackSize(stack));
                item.setQuality(stack, CyberwareAPI.QUALITY_SCAVENGED);
                items.add(WeightedEntry.wrap(stack, item.getZombieWeight()));
            }
        }
        return items;
    }

    private static boolean contains(List<ItemStack> haystack, ItemStack needle) {
        for (ItemStack stack : haystack) {
            if (!stack.isEmpty() && !needle.isEmpty() && stack.is(needle.getItem())) {
                return true;
            }
        }
        return false;
    }
}
