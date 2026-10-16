package flaxbeard.cyberware.api;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class CyberwareSurgeryEvent extends LivingEvent {
    protected CyberwareSurgeryEvent(LivingEntity entity) {
        super(entity);
    }

    /**
     * Fired when the Surgery Chamber starts altering an entity's installed cyberware.
     * Changing the inventories isn't supported. Cancel to prevent any changes.
     */
    public static class Pre extends CyberwareSurgeryEvent implements ICancellableEvent {
        private final ItemStackHandler installed;
        private final ItemStackHandler target;

        public Pre(LivingEntity entity, ItemStackHandler installed, ItemStackHandler target) {
            super(entity);
            this.installed = installed;
            this.target = target;
        }

        /**
         * @return the cyberware currently installed in the entity
         */
        public ItemStackHandler getInstalledCyberware() {
            return installed;
        }

        /**
         * @return the cyberware that will be added by the surgery
         */
        public ItemStackHandler getTargetCyberware() {
            return target;
        }
    }

    /**
     * Fired when the Surgery Chamber finishes altering an entity's installed cyberware.
     */
    public static class Post extends CyberwareSurgeryEvent {
        public Post(LivingEntity entity) {
            super(entity);
        }
    }
}
