package flaxbeard.cyberware.common.item;

import flaxbeard.cyberware.api.CyberwareUpdateEvent;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.api.item.Hudjack;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/**
 * Upgrades that require cybereyes. The visual effects (fog, glowing, zoom) are handled on the client in
 * {@code flaxbeard.cyberware.client.handler.CyberwareClientEffects}.
 */
public class CybereyeUpgradeItem extends CyberwareItem implements MenuItem, Hudjack {
    /** Amplifier used to recognize the night vision effect given by this cyberware. */
    public static final int NIGHT_VISION_AMPLIFIER = 53;

    /** Client side zoom level of the Distance Enhancer, 0 (off) to 3. */
    public static int zoomLevel = 0;

    private final Kind kind;

    public CybereyeUpgradeItem(Properties properties, Settings settings, Kind kind) {
        super(properties, settings);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public List<List<ItemStack>> required(ItemStack stack) {
        if (kind == Kind.TARGETING) {
            return List.of(
                    List.of(new ItemStack(CyberwareItems.CYBEREYES.get())),
                    List.of(new ItemStack(CyberwareItems.HUDJACK.get())));
        }
        return List.of(List.of(new ItemStack(CyberwareItems.CYBEREYES.get())));
    }

    @SubscribeEvent
    public static void handleNightVision(CyberwareUpdateEvent event) {
        LivingEntity entity = event.getEntity();
        CyberwareUserData data = event.getUserData();
        ItemStack nightVision = data.getCyberware(CyberwareItems.NIGHT_VISION.get());

        if (!nightVision.isEmpty() && EnableDisableHelper.isEnabled(nightVision)) {
            MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
            if (effect == null || effect.getAmplifier() != NIGHT_VISION_AMPLIFIER) {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, NIGHT_VISION_AMPLIFIER, true, false));
            }
        } else {
            MobEffectInstance effect = entity.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getAmplifier() == NIGHT_VISION_AMPLIFIER) {
                entity.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public boolean hasMenu(ItemStack stack) {
        return kind != Kind.UNDERWATER_VISION;
    }

    @Override
    public void use(Entity entity, ItemStack stack) {
        if (kind == Kind.ZOOM) {
            if (entity.level().isClientSide()) {
                zoomLevel = entity.isShiftKeyDown() ? (zoomLevel + 3) % 4 : (zoomLevel + 1) % 4;
            }
            return;
        }
        EnableDisableHelper.toggle(stack);
    }

    @Override
    public String getMenuLabel(ItemStack stack) {
        return kind == Kind.ZOOM ? "cyberware.gui.active.zoom" : EnableDisableHelper.getMenuLabel(stack);
    }

    @Override
    public float[] getColor(ItemStack stack) {
        return kind == Kind.ZOOM ? null : EnableDisableHelper.getMenuColor(stack);
    }

    @Override
    public boolean isActive(ItemStack stack) {
        return kind == Kind.HUDJACK && EnableDisableHelper.isEnabled(stack);
    }

    public enum Kind {
        NIGHT_VISION,
        UNDERWATER_VISION,
        HUDJACK,
        TARGETING,
        ZOOM
    }
}
