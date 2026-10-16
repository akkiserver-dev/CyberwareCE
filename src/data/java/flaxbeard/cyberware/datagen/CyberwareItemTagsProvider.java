package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import flaxbeard.cyberware.common.registry.CyberwareTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class CyberwareItemTagsProvider extends ItemTagsProvider {
    public CyberwareItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                     CompletableFuture<TagsProvider.TagLookup<Block>> blockTags, @Nullable ExistingFileHelper files) {
        super(output, lookup, blockTags, CyberwareMod.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(CyberwareTags.PAPER).add(Items.PAPER);
        tag(CyberwareTags.COMPONENTS).add(
                CyberwareItems.ACTUATOR.get(), CyberwareItems.REACTOR.get(), CyberwareItems.TITANIUM.get(), CyberwareItems.SSC.get(),
                CyberwareItems.PLATING.get(), CyberwareItems.FIBER_OPTICS.get(), CyberwareItems.FULLERENE.get(),
                CyberwareItems.SYNTH_NERVES.get(), CyberwareItems.STORAGE.get(), CyberwareItems.MICROELECTRIC.get());
        tag(ItemTags.DYEABLE).add(CyberwareItems.TRENCH_COAT.get());
    }
}
