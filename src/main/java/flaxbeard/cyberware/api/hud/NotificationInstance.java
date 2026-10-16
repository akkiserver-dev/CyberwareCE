package flaxbeard.cyberware.api.hud;

/**
 * A notification together with the (partial) tick time it was created at.
 */
public record NotificationInstance(float createdTime, Notification notification) {
}
