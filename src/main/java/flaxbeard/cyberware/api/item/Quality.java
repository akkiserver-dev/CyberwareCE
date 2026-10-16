package flaxbeard.cyberware.api.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The quality of a piece of cyberware (e.g. scavenged from a mob or manufactured).
 * Qualities are identified by their translation key, which is also what gets saved on the stack.
 */
public final class Quality {
    private static final Map<String, Quality> BY_NAME = new HashMap<>();
    private static final List<Quality> QUALITIES = new ArrayList<>();

    public static final Codec<Quality> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                Quality quality = byName(name);
                return quality != null ? DataResult.success(quality) : DataResult.error(() -> "Unknown cyberware quality: " + name);
            },
            Quality::getTranslationKey);

    public static final StreamCodec<ByteBuf, Quality> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Quality::byName, Quality::getTranslationKey);

    private final String translationKey;
    @Nullable
    private final String nameModifier;
    @Nullable
    private final String spriteSuffix;

    public Quality(String translationKey) {
        this(translationKey, null, null);
    }

    /**
     * @param translationKey key used for the tooltip line, also the saved identifier
     * @param nameModifier   optional translation key formatting the item name, receives the base name as argument
     * @param spriteSuffix   optional suffix of the item model used for stacks of this quality
     */
    public Quality(String translationKey, @Nullable String nameModifier, @Nullable String spriteSuffix) {
        this.translationKey = translationKey;
        this.nameModifier = nameModifier;
        this.spriteSuffix = spriteSuffix;
        BY_NAME.put(translationKey, this);
        QUALITIES.add(this);
    }

    public String getTranslationKey() {
        return translationKey;
    }

    @Nullable
    public String getNameModifier() {
        return nameModifier;
    }

    @Nullable
    public String getSpriteSuffix() {
        return spriteSuffix;
    }

    @Nullable
    public static Quality byName(String name) {
        return BY_NAME.get(name);
    }

    public static List<Quality> values() {
        return Collections.unmodifiableList(QUALITIES);
    }

    // required for data components; qualities are identified by their translation key
    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Quality quality && translationKey.equals(quality.translationKey));
    }

    @Override
    public int hashCode() {
        return translationKey.hashCode();
    }

    @Override
    public String toString() {
        return translationKey;
    }
}
