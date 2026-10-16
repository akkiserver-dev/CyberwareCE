package flaxbeard.cyberware.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A marker effect without behaviour of its own, checked by the cyberware handlers.
 * The icon is taken from {@code textures/gui/potions.png} at {@link #getIconIndex()}.
 */
public class CyberwareMobEffect extends MobEffect {
    private final int iconIndex;

    public CyberwareMobEffect(MobEffectCategory category, int color, int iconIndex) {
        super(category, color);
        this.iconIndex = iconIndex;
    }

    public int getIconIndex() {
        return iconIndex;
    }
}
