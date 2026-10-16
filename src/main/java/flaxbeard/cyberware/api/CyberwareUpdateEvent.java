package flaxbeard.cyberware.api;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Fired every tick, on both sides, for every living entity that has cyberware data.
 * Most cyberware logic hooks into this event.
 */
public class CyberwareUpdateEvent extends LivingEvent {
    private final CyberwareUserData userData;

    public CyberwareUpdateEvent(LivingEntity entity, CyberwareUserData userData) {
        super(entity);
        this.userData = userData;
    }

    public CyberwareUserData getUserData() {
        return userData;
    }
}
