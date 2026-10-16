package flaxbeard.cyberware.api.item;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

/**
 * A region of the body that cyberware can be installed into. Each region holds
 * {@link flaxbeard.cyberware.common.CyberwareConstants#WARE_PER_SLOT} pieces of cyberware.
 * The ordinal is used as the storage index, so the declaration order must not change.
 */
public enum BodyRegion implements StringRepresentable {
    EYES(12, "eyes"),
    CRANIUM(11, "cranium"),
    HEART(14, "heart"),
    LUNGS(15, "lungs"),
    LOWER_ORGANS(17, "lower_organs"),
    SKIN(18, "skin"),
    MUSCLE(19, "muscle"),
    BONE(20, "bone"),
    ARM(21, "arm", true, true),
    HAND(22, "hand", true, false),
    LEG(23, "leg", true, true),
    FOOT(24, "foot", true, false);

    public static final Codec<BodyRegion> CODEC = StringRepresentable.fromEnum(BodyRegion::values);

    private final int pageId;
    private final String name;
    private final boolean sided;
    private final boolean hasEssential;

    BodyRegion(int pageId, String name, boolean sided, boolean hasEssential) {
        this.pageId = pageId;
        this.name = name;
        this.sided = sided;
        this.hasEssential = hasEssential;
    }

    BodyRegion(int pageId, String name) {
        this(pageId, name, false, true);
    }

    /**
     * @return the id of the Robosurgeon page that displays this region
     */
    public int getPageId() {
        return pageId;
    }

    @Nullable
    public static BodyRegion byPageId(int pageId) {
        for (BodyRegion region : values()) {
            if (region.pageId == pageId) {
                return region;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public boolean isSided() {
        return sided;
    }

    public boolean hasEssential() {
        return hasEssential;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
