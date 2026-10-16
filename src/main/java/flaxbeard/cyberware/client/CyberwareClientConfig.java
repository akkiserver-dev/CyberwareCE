package flaxbeard.cyberware.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client only settings.
 */
public final class CyberwareClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_PLAYER_MODEL;
    public static final ModConfigSpec.BooleanValue ENABLE_FLOAT;
    public static final ModConfigSpec.DoubleValue HUDJACK_FLOAT;
    public static final ModConfigSpec.DoubleValue HUDLENS_FLOAT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("other");
        ENABLE_CUSTOM_PLAYER_MODEL = builder.comment("Enable changes to player model (missing skin, missing limbs, Cybernetic limbs)")
                .define("enableCustomPlayerModel", true);
        builder.pop();

        builder.push("hud");
        ENABLE_FLOAT = builder.comment("Enable hudlens and hudjack float.", "Experimental, defaults to false.").define("enableFloat", false);
        HUDJACK_FLOAT = builder.comment("Amount hudjack HUD will 'float' with movement. Set to 0 for no float.").defineInRange("hudjackFloat", 0.05, 0.0, 100.0);
        HUDLENS_FLOAT = builder.comment("Amount hudlens HUD will 'float' with movement. Set to 0 for no float.").defineInRange("hudlensFloat", 0.1, 0.0, 100.0);
        builder.pop();

        SPEC = builder.build();
    }

    private CyberwareClientConfig() {
    }
}
