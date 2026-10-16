package flaxbeard.cyberware.datagen;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.registry.CyberwareDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * All damage of the mod ignores armor; except for surgery it is also "absolute" like in 1.12:
 * effects, resistance and enchantments don't reduce it.
 */
public class CyberwareDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public CyberwareDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper files) {
        super(output, Registries.DAMAGE_TYPE, lookup, CyberwareMod.MOD_ID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        List<ResourceKey<DamageType>> all = CyberwareRegistryProvider.DAMAGE_TYPES;
        List<ResourceKey<DamageType>> absolute = all.stream().filter(key -> key != CyberwareDamageTypes.SURGERY).toList();
        add(DamageTypeTags.BYPASSES_ARMOR, all);
        add(DamageTypeTags.BYPASSES_EFFECTS, absolute);
        add(DamageTypeTags.BYPASSES_RESISTANCE, absolute);
        add(DamageTypeTags.BYPASSES_ENCHANTMENTS, absolute);
    }

    private void add(TagKey<DamageType> tag, List<ResourceKey<DamageType>> types) {
        TagAppender<DamageType> appender = tag(tag);
        types.forEach(appender::add);
    }
}
