package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class LowerOrgansUpgradeItem extends CyberwareItem implements MenuItem {
    private static final String TICKS_UNTIL_REMOVE_KEY = "toRemove";
    private static final String WAS_BELOW_KEY = "wasBelow";

    private static final Map<Player, Collection<MobEffectInstance>> EFFECTS_BEFORE_EATING = new WeakHashMap<>();

    private final Kind kind;

    public LowerOrgansUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return switch (kind) {
            case METABOLIC_GENERATOR -> CyberwareConstants.METABOLIC_PRODUCTION;
            case BATTERY -> CyberwareConstants.BATTERY_CAPACITY * stack.getCount();
            default -> 0;
        };
    }

    @Override
    public int installedStackSize(ItemStack stack) {
        return kind == Kind.BATTERY ? 4 : 1;
    }

    @Override
    public int getPowerProduction(ItemStack stack) {
        return kind == Kind.METABOLIC_GENERATOR ? CyberwareConstants.METABOLIC_PRODUCTION : 0;
    }

    @Override
    public int getPowerConsumption(ItemStack stack) {
        return kind == Kind.ADRENALINE_PUMP ? CyberwareConstants.ADRENALINE_CONSUMPTION : 0;
    }

    // ---- Liver Filter ----

    private static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        UseAnim animation = stack.getUseAnimation();
        return animation == UseAnim.EAT || animation == UseAnim.DRINK;
    }

    @SubscribeEvent
    public static void handleEatFoodTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || !isFood(event.getItem())) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data != null && data.isCyberwareInstalled(CyberwareItems.LIVER_FILTER.get())) {
            List<MobEffectInstance> copies = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                copies.add(new MobEffectInstance(effect));
            }
            EFFECTS_BEFORE_EATING.put(player, copies);
        }
    }

    @SubscribeEvent
    public static void handleEatFoodEnd(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || !isFood(event.getItem())) return;
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data == null || !data.isCyberwareInstalled(CyberwareItems.LIVER_FILTER.get())) return;

        List<Holder<MobEffect>> removed = new ArrayList<>();
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
                removed.add(effect.getEffect());
            }
        }

        // restore harmful effects that were already present before eating
        Collection<MobEffectInstance> before = EFFECTS_BEFORE_EATING.remove(player);
        if (before != null) {
            for (MobEffectInstance effect : before) {
                if (removed.contains(effect.getEffect())) {
                    player.addEffect(effect);
                }
            }
        }
    }

    // ---- Metabolic Generator & Adrenaline Pump ----

    @SubscribeEvent
    public static void handleLivingUpdate(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 20 != 0) return;
        CyberwareUserData data = event.getUserData();

        LowerOrgansUpgradeItem metabolicItem = CyberwareItems.METABOLIC_GENERATOR.get();
        ItemStack metabolic = data.getCyberware(metabolicItem);
        if (!metabolic.isEmpty()
                && EnableDisableHelper.isEnabled(metabolic)
                && !data.isAtCapacity(metabolic, metabolicItem.getPowerProduction(metabolic))) {
            if (entity instanceof Player player) {
                if (player.getFoodData().getFoodLevel() > 0 || player.isCreative()) {
                    var functionData = CyberwareAPI.getFunctionData(metabolic);
                    int toRemove = functionData.contains(TICKS_UNTIL_REMOVE_KEY) ? functionData.getInt(TICKS_UNTIL_REMOVE_KEY) : CyberwareConstants.METABOLIC_USES;
                    if (!player.isCreative() && toRemove <= 0) {
                        player.getFoodData().addExhaustion(6.0F);
                        toRemove = CyberwareConstants.METABOLIC_USES;
                    } else if (toRemove > 0) {
                        toRemove--;
                    }
                    int newToRemove = toRemove;
                    CyberwareAPI.updateFunctionData(metabolic, tag -> tag.putInt(TICKS_UNTIL_REMOVE_KEY, newToRemove));
                    data.addPower(metabolicItem.getPowerProduction(metabolic), metabolic);
                }
            } else {
                data.addPower(metabolicItem.getPowerProduction(metabolic) / 10, metabolic);
            }
        }

        LowerOrgansUpgradeItem adrenalineItem = CyberwareItems.ADRENALINE_PUMP.get();
        ItemStack adrenaline = data.getCyberware(adrenalineItem);
        if (!adrenaline.isEmpty()) {
            boolean wasBelow = CyberwareAPI.getFunctionData(adrenaline).getBoolean(WAS_BELOW_KEY);
            boolean isBelow = false;
            if (entity.getMaxHealth() > 8 && entity.getHealth() < 8) {
                isBelow = true;
                if (!wasBelow && data.usePower(adrenaline, adrenalineItem.getPowerConsumption(adrenaline), false)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0, true, false));
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, true, false));
                }
            }
            if (isBelow != wasBelow) {
                boolean newBelow = isBelow;
                CyberwareAPI.updateFunctionData(adrenaline, tag -> tag.putBoolean(WAS_BELOW_KEY, newBelow));
            }
        }
    }

    // ---- menu ----

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind == Kind.METABOLIC_GENERATOR;
    }

    @Override
    public void use(Entity entity, ItemStack stack) {
        EnableDisableHelper.toggle(stack);
    }

    @Override
    public String getMenuLabel(ItemStack stack) {
        return EnableDisableHelper.getMenuLabel(stack);
    }

    @Override
    public float[] getColor(ItemStack stack) {
        return EnableDisableHelper.getMenuColor(stack);
    }

    public enum Kind {
        LIVER_FILTER,
        METABOLIC_GENERATOR,
        BATTERY,
        ADRENALINE_PUMP
    }
}
