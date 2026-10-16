package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.CyberwareTabItem;
import flaxbeard.cyberware.api.item.Deconstructable;
import flaxbeard.cyberware.api.item.Quality;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base class of all installable cyberware items.
 * <p>
 * Static properties (body region, essence cost, zombie weight, components) are configured with {@link Settings};
 * behaviour is added by overriding the hooks or by event handlers in the subclasses.
 */
public class CyberwareItem extends Item implements Cyberware, CyberwareTabItem, Deconstructable {
    private final BodyRegion region;
    private final int[] essence;
    private final int weight;
    private final String tooltipKey;
    private final Supplier<List<ItemStack>> components;
    private List<ItemStack> cachedComponents;

    public CyberwareItem(Properties properties, Settings settings) {
        super(properties);
        this.region = settings.region;
        this.essence = settings.essence;
        this.weight = settings.weight;
        this.tooltipKey = settings.tooltipKey;
        this.components = settings.components;
    }

    public static Settings settings(BodyRegion region, String tooltipKey) {
        return new Settings(region, tooltipKey);
    }

    /**
     * @return the weight of this cyberware when picking random cyberware for CyberZombies, 0 if never picked
     */
    public int getZombieWeight() {
        return weight;
    }

    public String getTooltipKey() {
        return tooltipKey;
    }

    // ---- Cyberware ----

    @Override
    public BodyRegion getBodyRegion(ItemStack stack) {
        return region;
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        return List.of();
    }

    @Override
    public boolean isIncompatible(ItemStack stack, ItemStack other) {
        return false;
    }

    @Override
    public boolean isEssential(ItemStack stack) {
        return false;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return 0;
    }

    @Override
    public void onAdded(LivingEntity entity, ItemStack stack) {
        // no operation
    }

    @Override
    public void onRemoved(LivingEntity entity, ItemStack stack) {
        // no operation
    }

    @Override
    public int getEssenceCost(ItemStack stack) {
        int cost = getUnmodifiedEssenceCost(stack);
        if (getQuality(stack) == CyberwareAPI.QUALITY_SCAVENGED) {
            // note: negative costs (essence gains) are reduced for scavenged cyberware
            int half = (int) Math.ceil(cost / 2F);
            cost = cost > 0 ? cost + half : cost - half;
        }
        return cost;
    }

    protected int getUnmodifiedEssenceCost(ItemStack stack) {
        if (essence.length == 1) {
            return essence[0];
        }
        int index = Math.max(0, Math.min(essence.length - 1, stack.getCount() - 1));
        return essence[index];
    }

    public int getPowerConsumption(ItemStack stack) {
        return 0;
    }

    public int getPowerProduction(ItemStack stack) {
        return 0;
    }

    public boolean hasCustomPowerMessage(ItemStack stack) {
        return false;
    }

    public boolean hasCustomCapacityMessage(ItemStack stack) {
        return false;
    }

    // ---- quality ----

    @Override
    public Quality getQuality(ItemStack stack) {
        Quality quality = CyberwareAPI.readQuality(stack);
        return quality == null ? CyberwareAPI.QUALITY_MANUFACTURED : quality;
    }

    @Override
    public ItemStack setQuality(ItemStack stack, Quality quality) {
        if (quality == CyberwareAPI.QUALITY_MANUFACTURED) {
            stack.remove(flaxbeard.cyberware.common.registry.CyberwareDataComponents.QUALITY);
            return stack;
        }
        return canHoldQuality(stack, quality) ? CyberwareAPI.writeQuality(stack, quality) : stack;
    }

    @Override
    public boolean canHoldQuality(ItemStack stack, Quality quality) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        Quality quality = getQuality(stack);
        if (quality != null && quality.getNameModifier() != null) {
            return Component.translatable(quality.getNameModifier(), name);
        }
        return name;
    }

    // ---- tooltip ----

    @Override
    public List<Component> getInfo(ItemStack stack) {
        List<Component> info = new ArrayList<>();
        for (String line : splitTranslation(tooltipKey)) {
            info.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }

        int maxSize = installedStackSize(stack);
        if (maxSize > 1) {
            info.add(Component.translatable("cyberware.tooltip.max_install", maxSize).withStyle(ChatFormatting.BLUE));
        }

        String consumption = joinPerCount(stack, this::getPowerConsumption, false);
        if (consumption != null) {
            String key = hasCustomPowerMessage(stack) ? tooltipKey + ".power_consumption" : "cyberware.tooltip.power_consumption";
            info.add(Component.translatable(key, consumption).withStyle(ChatFormatting.GREEN));
        }

        String production = joinPerCount(stack, this::getPowerProduction, false);
        if (production != null) {
            String key = hasCustomPowerMessage(stack) ? tooltipKey + ".power_production" : "cyberware.tooltip.power_production";
            info.add(Component.translatable(key, production).withStyle(ChatFormatting.GREEN));
        }

        if (getCapacity(stack) > 0) {
            String key = hasCustomCapacityMessage(stack) ? tooltipKey + ".capacity" : "cyberware.tooltip.capacity";
            info.add(Component.translatable(key, getCapacity(stack)).withStyle(ChatFormatting.GREEN));
        }

        boolean hasEssenceCost = false;
        boolean essenceCostPositive = true;
        StringBuilder essenceText = new StringBuilder();
        for (int count = 1; count <= maxSize; count++) {
            ItemStack temp = stack.copyWithCount(count);
            int cost = getEssenceCost(temp);
            if (cost != 0) hasEssenceCost = true;
            if (cost < 0) essenceCostPositive = false;
            if (count != 1) essenceText.append(Language.getInstance().getOrDefault("cyberware.tooltip.joiner"));
            essenceText.append(' ').append(Math.abs(cost));
        }
        if (hasEssenceCost) {
            info.add(Component.translatable(essenceCostPositive ? "cyberware.tooltip.essence" : "cyberware.tooltip.essence_add", essenceText.toString())
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        return info;
    }

    private String joinPerCount(ItemStack stack, java.util.function.ToIntFunction<ItemStack> valueFunction, boolean allowZero) {
        boolean hasValue = false;
        StringBuilder text = new StringBuilder();
        for (int count = 1; count <= installedStackSize(stack); count++) {
            int value = valueFunction.applyAsInt(stack.copyWithCount(count));
            if (value > 0 || allowZero) hasValue = true;
            if (count != 1) text.append(Language.getInstance().getOrDefault("cyberware.tooltip.joiner"));
            text.append(' ').append(value);
        }
        return hasValue ? text.toString() : null;
    }

    /**
     * Splits a translation on literal {@code \n} sequences, the format used by the lang files for multi line tooltips.
     */
    public static List<String> splitTranslation(String key) {
        String translated = Language.getInstance().getOrDefault(key, "");
        List<String> lines = new ArrayList<>(Arrays.asList(translated.split("\\\\n")));
        if (!lines.isEmpty() && lines.get(0).isEmpty()) {
            lines.remove(0);
        }
        return lines;
    }

    // ---- creative tab ----

    @Override
    public Category getCategory(ItemStack stack) {
        return Category.of(getBodyRegion(stack));
    }

    // ---- deconstruction ----

    @Override
    public boolean canDestroy(ItemStack stack) {
        return !getComponentList().isEmpty();
    }

    @Override
    public List<ItemStack> getComponents(ItemStack stack) {
        return getComponentList();
    }

    private List<ItemStack> getComponentList() {
        if (cachedComponents == null) {
            cachedComponents = components.get();
        }
        return cachedComponents;
    }

    private record ComponentCost(Supplier<? extends ItemLike> item, int count) {
    }

    /**
     * Static configuration of a cyberware item.
     */
    public static class Settings {
        private final BodyRegion region;
        private final String tooltipKey;
        private int[] essence = {0};
        private int weight = 0;
        private final List<ComponentCost> componentList = new ArrayList<>();
        private final Supplier<List<ItemStack>> components = () -> componentList.stream()
                .map(cost -> new ItemStack(cost.item().get(), cost.count()))
                .toList();

        private Settings(BodyRegion region, String tooltipKey) {
            this.region = region;
            this.tooltipKey = tooltipKey;
        }

        /**
         * @param essence the essence cost; if several values are given they are the cost for 1, 2, 3... installed
         */
        public Settings essence(int... essence) {
            this.essence = essence;
            return this;
        }

        public Settings weight(int weight) {
            this.weight = weight;
            return this;
        }

        /**
         * Adds an item produced when this cyberware is destroyed in the Engineering Table.
         */
        public Settings component(Supplier<? extends ItemLike> item, int count) {
            this.componentList.add(new ComponentCost(item, count));
            return this;
        }
    }
}
