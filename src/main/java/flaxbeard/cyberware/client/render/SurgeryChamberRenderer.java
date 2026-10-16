package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.block.SurgeryChamberBlock;
import flaxbeard.cyberware.common.block.entity.SurgeryChamberBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The animated double doors of the Surgery Chamber, rendered from the upper half.
 */
public class SurgeryChamberRenderer implements BlockEntityRenderer<SurgeryChamberBlockEntity> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/models/surgery_chamber_door.png");

    private final ModelPart left;
    private final ModelPart right;

    public SurgeryChamberRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(CyberwareModelLayers.SURGERY_CHAMBER_DOOR);
        this.left = root.getChild("left");
        this.right = root.getChild("right");
    }

    /**
     * @return the rotation around Y of a model facing the given direction, as the original renderers did
     */
    static float getFacingRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case NORTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }

    @Override
    public void render(SurgeryChamberBlockEntity chamber, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = chamber.getBlockState();
        if (!(state.getBlock() instanceof SurgeryChamberBlock) || chamber.getLevel() == null) return;
        float ticks = chamber.getLevel().getGameTime() + partialTick;

        boolean isOpen = state.getValue(SurgeryChamberBlock.OPEN);
        if (isOpen != chamber.lastOpen) {
            chamber.lastOpen = isOpen;
            chamber.openTicks = ticks;
        }
        float ticksPassed = Math.min(10, ticks - chamber.openTicks);
        float rotate = (float) (Math.sin(ticksPassed * (Math.PI / 2) / 10F) * 90F);
        if (!isOpen) {
            rotate = 90F - rotate;
        }

        poseStack.pushPose();
        poseStack.translate(.5, .5, .5);
        poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotation(state.getValue(SurgeryChamberBlock.FACING))));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));

        poseStack.pushPose();
        poseStack.translate(-6F / 16F, 0F, -6F / 16F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotate));
        left.render(poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(6F / 16F, 0F, -6F / 16F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotate));
        right.render(poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }
}
