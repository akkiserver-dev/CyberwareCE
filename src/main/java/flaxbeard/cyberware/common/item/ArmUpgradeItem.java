package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.List;

/**
 * Bow String Retractor: bows draw twice as fast.
 */
public class ArmUpgradeItem extends CyberwareItem {
    public ArmUpgradeItem(Properties properties, Settings settings) {
        super(properties, settings);
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        return List.of(List.of(
                new ItemStack(CyberwareItems.CYBERARM_LEFT.get()),
                new ItemStack(CyberwareItems.CYBERARM_RIGHT.get())));
    }

    @SubscribeEvent
    public static void useBow(LivingEntityUseItemEvent.Tick event) {
        // note: the use animation can't be used to detect bows since it's used by many unrelated items
        if (!(event.getItem().getItem() instanceof BowItem)) return;
        CyberwareUserData data = CyberwareAPI.getUserData(event.getEntity());
        if (data != null && data.isCyberwareInstalled(CyberwareItems.BOW_STRING_RETRACTOR.get())) {
            event.setDuration(event.getDuration() - 1);
        }
    }
}
