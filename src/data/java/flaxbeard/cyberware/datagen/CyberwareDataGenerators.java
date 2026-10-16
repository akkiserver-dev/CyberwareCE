package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Generates the data pack of the mod into {@code src/generated/resources} ({@code gradlew runData}).
 */
@EventBusSubscriber(modid = CyberwareMod.MOD_ID)
public final class CyberwareDataGenerators {
    private CyberwareDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper files = event.getExistingFileHelper();
        boolean server = event.includeServer();

        // damage types and the biome modifier; the providers below see them through its lookup
        CyberwareRegistryProvider registries = generator.addProvider(server, new CyberwareRegistryProvider(output, lookup));
        CompletableFuture<HolderLookup.Provider> fullLookup = registries.getRegistryProvider();

        generator.addProvider(server, new CyberwareRecipeProvider(output, fullLookup));
        generator.addProvider(server, CyberwareLootTables.create(output, fullLookup));
        generator.addProvider(server, new CyberwareLootModifierProvider(output, fullLookup));

        CyberwareBlockTagsProvider blockTags = generator.addProvider(server, new CyberwareBlockTagsProvider(output, lookup, files));
        generator.addProvider(server, new CyberwareItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));
        generator.addProvider(server, new CyberwareDamageTypeTagsProvider(output, fullLookup, files));
    }
}
