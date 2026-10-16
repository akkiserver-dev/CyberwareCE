package flaxbeard.cyberware.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import flaxbeard.cyberware.common.CyberwareConfig;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.function.Supplier;

/**
 * A recipe condition on a boolean config option, e.g. {@code {"type": "cyberware:config", "option": "surgery_crafting"}}.
 */
public record ConfigCondition(Option option) implements ICondition {
    public static final MapCodec<ConfigCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Option.CODEC.fieldOf("option").forGetter(ConfigCondition::option)
    ).apply(instance, ConfigCondition::new));

    @Override
    public boolean test(IContext context) {
        return option.value.get().get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    public enum Option implements StringRepresentable {
        SURGERY_CRAFTING("surgery_crafting", () -> CyberwareConfig.SURGERY_CRAFTING),
        KATANA("katana", () -> CyberwareConfig.ENABLE_KATANA),
        CLOTHES("clothes", () -> CyberwareConfig.ENABLE_CLOTHES);

        public static final Codec<Option> CODEC = StringRepresentable.fromEnum(Option::values);

        private final String name;
        private final Supplier<ModConfigSpec.BooleanValue> value;

        Option(String name, Supplier<ModConfigSpec.BooleanValue> value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
