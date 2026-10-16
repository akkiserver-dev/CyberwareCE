package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.entity.CyberZombie;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CyberwareMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CyberZombie>> CYBER_ZOMBIE = ENTITY_TYPES.register("cyberzombie",
            () -> EntityType.Builder.of(CyberZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .passengerAttachments(2.0125F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(8)
                    .build(CyberwareMod.MOD_ID + ":cyberzombie"));

    public static final DeferredItem<DeferredSpawnEggItem> CYBER_ZOMBIE_SPAWN_EGG = CyberwareItems.ITEMS.register("cyberzombie_spawn_egg",
            () -> new DeferredSpawnEggItem(CYBER_ZOMBIE, 0x6B6B6B, 0x799C65, new Item.Properties()));

    private CyberwareEntities() {
    }
}
