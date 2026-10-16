package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.CyberwareTabItem;
import flaxbeard.cyberware.api.item.Quality;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.item.CyberwareArmorItem;
import flaxbeard.cyberware.common.item.KatanaItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CyberwareCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CyberwareMod.MOD_ID);

    /**
     * Whether the tab shows scavenged instead of manufactured cyberware, toggled by buttons in the creative screen.
     */
    public static boolean showScavenged = false;

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CYBERWARE = CREATIVE_TABS.register("cyberware",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cyberware"))
                    .icon(() -> new ItemStack(CyberwareItems.CYBEREYES.get()))
                    .displayItems((parameters, output) -> {
                        Quality quality = showScavenged ? CyberwareAPI.QUALITY_SCAVENGED : CyberwareAPI.QUALITY_MANUFACTURED;
                        Map<CyberwareTabItem.Category, List<ItemStack>> categories = new EnumMap<>(CyberwareTabItem.Category.class);
                        List<ItemStack> unsorted = new ArrayList<>();

                        for (var holder : CyberwareItems.ITEMS.getEntries()) {
                            Item item = holder.get();
                            if (!isEnabled(item)) continue;
                            for (ItemStack stack : getStacks(item)) {
                                if (CyberwareAPI.isCyberware(stack)) {
                                    Cyberware cyberware = CyberwareAPI.getCyberware(stack);
                                    if (cyberware.canHoldQuality(stack, quality)) {
                                        stack = cyberware.setQuality(stack, quality);
                                    }
                                }
                                if (item instanceof CyberwareTabItem tabItem) {
                                    categories.computeIfAbsent(tabItem.getCategory(stack), category -> new ArrayList<>()).add(stack);
                                } else {
                                    unsorted.add(stack);
                                }
                            }
                        }
                        for (CyberwareTabItem.Category category : CyberwareTabItem.Category.values()) {
                            categories.getOrDefault(category, List.of()).forEach(output::accept);
                        }
                        unsorted.forEach(output::accept);
                    })
                    .build());

    private CyberwareCreativeTabs() {
    }

    private static boolean isEnabled(Item item) {
        if (item instanceof KatanaItem) return CyberwareConfig.ENABLE_KATANA.get();
        if (item instanceof CyberwareArmorItem) return CyberwareConfig.ENABLE_CLOTHES.get();
        return true;
    }

    private static List<ItemStack> getStacks(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item == CyberwareItems.EXP_CAPSULE.get()) {
            stack.set(CyberwareDataComponents.EXPERIENCE, 100);
        }
        if (item == CyberwareItems.TRENCH_COAT.get()) {
            ItemStack brown = new ItemStack(item);
            brown.set(DataComponents.DYED_COLOR, new DyedItemColor(0x664028, true));
            ItemStack white = new ItemStack(item);
            white.set(DataComponents.DYED_COLOR, new DyedItemColor(0xEAEAEA, true));
            return List.of(stack, brown, white);
        }
        return List.of(stack);
    }
}
