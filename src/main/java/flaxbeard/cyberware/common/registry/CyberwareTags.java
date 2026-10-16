package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class CyberwareTags {
    /** Items accepted as paper by the Engineering Table, Scanner and Blueprint Archive (the old "paper" ore dictionary). */
    public static final TagKey<Item> PAPER = TagKey.create(Registries.ITEM, CyberwareAPI.id("paper"));

    /** Cyberware components, the only items a Component Box accepts. */
    public static final TagKey<Item> COMPONENTS = TagKey.create(Registries.ITEM, CyberwareAPI.id("components"));

    private CyberwareTags() {
    }
}
