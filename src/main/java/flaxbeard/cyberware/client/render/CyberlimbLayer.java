package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.client.CyberwareClientConfig;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Renders the body parts that were hidden by {@link PlayerModelHandler}: cyberlimbs with the robot textures and,
 * for players without skin, the remaining parts with the muscle texture.
 */
public class CyberlimbLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation MUSCLES = CyberwareAPI.id("textures/models/player_muscles.png");
    public static final ResourceLocation ROBOT = CyberwareAPI.id("textures/models/player_robot.png");
    public static final ResourceLocation RUSTY_ROBOT = CyberwareAPI.id("textures/models/player_rusty_robot.png");

    public CyberlimbLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Nullable
    static ResourceLocation getLimbTexture(PlayerCyberwareState.LimbLook look, boolean hasSkin) {
        return switch (look) {
            case ROBOT -> ROBOT;
            case RUSTY -> RUSTY_ROBOT;
            case NORMAL -> hasSkin ? null : MUSCLES;
        };
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!CyberwareClientConfig.ENABLE_CUSTOM_PLAYER_MODEL.get() || player.isInvisible()) return;
        PlayerCyberwareState state = PlayerCyberwareState.of(player);
        if (state == null || !state.isModified()) return;

        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        if (!state.hasSkin()) {
            renderPart(model.head, MUSCLES, poseStack, buffers, packedLight, overlay);
            renderPart(model.body, MUSCLES, poseStack, buffers, packedLight, overlay);
        }
        if (state.hasLeftArm()) renderPart(model.leftArm, getLimbTexture(state.leftArm(), state.hasSkin()), poseStack, buffers, packedLight, overlay);
        if (state.hasRightArm()) renderPart(model.rightArm, getLimbTexture(state.rightArm(), state.hasSkin()), poseStack, buffers, packedLight, overlay);
        if (state.hasLeftLeg()) renderPart(model.leftLeg, getLimbTexture(state.leftLeg(), state.hasSkin()), poseStack, buffers, packedLight, overlay);
        if (state.hasRightLeg()) renderPart(model.rightLeg, getLimbTexture(state.rightLeg(), state.hasSkin()), poseStack, buffers, packedLight, overlay);
    }

    private static void renderPart(ModelPart part, @Nullable ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (texture == null) return;
        boolean visible = part.visible;
        part.visible = true;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        part.render(poseStack, buffer, light, overlay);
        part.visible = visible;
    }
}
