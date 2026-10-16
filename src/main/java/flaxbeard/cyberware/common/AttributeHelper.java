package flaxbeard.cyberware.common;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Adds and removes the attribute modifiers granted by cyberware.
 */
public final class AttributeHelper {
    private AttributeHelper() {
    }

    /**
     * Adds a transient (unsaved) modifier, replacing an existing one with a different amount.
     */
    public static void applyTransient(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        apply(entity, attribute, id, amount, false);
    }

    /**
     * Adds a saved modifier, replacing an existing one with a different amount. Used for max health so that
     * the extra health isn't lost when the entity is loaded.
     */
    public static void applyPermanent(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        apply(entity, attribute, id, amount, true);
    }

    private static void apply(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean permanent) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (existing.amount() == amount) return;
            instance.removeModifier(id);
        }
        AttributeModifier modifier = new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE);
        if (permanent) {
            instance.addPermanentModifier(modifier);
        } else {
            instance.addTransientModifier(modifier);
        }
    }

    public static void remove(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }
}
