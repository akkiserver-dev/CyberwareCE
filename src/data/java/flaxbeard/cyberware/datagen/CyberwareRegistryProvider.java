package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import flaxbeard.cyberware.common.world.CyberZombieSpawnModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Damage types and the CyberZombie spawn biome modifier.
 */
public class CyberwareRegistryProvider extends DatapackBuiltinEntriesProvider {
    /** All damage types of the mod; they are death messages {@code death.attack.cyberware.<name>}. */
    static final List<ResourceKey<DamageType>> DAMAGE_TYPES = List.of(
            CyberwareDamageTypes.BRAINLESS, CyberwareDamageTypes.HEARTLESS, CyberwareDamageTypes.SURGERY,
            CyberwareDamageTypes.SPINELESS, CyberwareDamageTypes.NO_MUSCLES, CyberwareDamageTypes.NO_ESSENCE,
            CyberwareDamageTypes.LOW_ESSENCE);

    private static final RegistrySetBuilder ENTRIES = new RegistrySetBuilder()
            .add(Registries.DAMAGE_TYPE, CyberwareRegistryProvider::damageTypes)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CyberwareRegistryProvider::biomeModifiers);

    public CyberwareRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ENTRIES, Set.of(CyberwareMod.MOD_ID));
    }

    private static void damageTypes(BootstrapContext<DamageType> context) {
        for (ResourceKey<DamageType> key : DAMAGE_TYPES) {
            context.register(key, new DamageType("cyberware." + key.location().getPath(), DamageScaling.NEVER, 0.0F));
        }
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        context.register(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CyberwareAPI.id("cyberzombie_spawns")),
                new CyberZombieSpawnModifier());
    }
}
