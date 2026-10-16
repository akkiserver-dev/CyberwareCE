package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.registry.CyberwareBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class CyberwareBlockTagsProvider extends BlockTagsProvider {
    public CyberwareBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper files) {
        super(output, lookup, CyberwareMod.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        CyberwareBlocks.BLOCKS.getEntries().stream().map(Holder::value).forEach(tag(BlockTags.MINEABLE_WITH_PICKAXE)::add);
    }
}
