package flaxbeard.cyberware.common.world;

import com.mojang.serialization.MapCodec;
import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.registry.CyberwareEntities;
import flaxbeard.cyberware.common.registry.CyberwareRecipes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds CyberZombies to every biome where zombies spawn, with the weight and pack sizes from the config.
 */
public record CyberZombieSpawnModifier() implements BiomeModifier {
    public static final MapCodec<CyberZombieSpawnModifier> CODEC = MapCodec.unit(new CyberZombieSpawnModifier());

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !CyberwareConfig.MOBS_ENABLE_CYBER_ZOMBIES.get()) return;

        MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
        boolean hasZombies = spawns.getSpawner(MobCategory.MONSTER).stream().anyMatch(spawner -> spawner.type == EntityType.ZOMBIE);
        if (hasZombies) {
            int minPack = CyberwareConfig.MOBS_CYBER_ZOMBIE_MIN_PACK.get();
            int maxPack = Math.max(minPack, CyberwareConfig.MOBS_CYBER_ZOMBIE_MAX_PACK.get());
            spawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(CyberwareEntities.CYBER_ZOMBIE.get(),
                    CyberwareConfig.MOBS_CYBER_ZOMBIE_WEIGHT.get(), minPack, maxPack));
            CyberwareMod.LOGGER.debug("Adding CyberZombies spawning to {}", biome.getRegisteredName());
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CyberwareRecipes.CYBER_ZOMBIE_SPAWNS.get();
    }
}
