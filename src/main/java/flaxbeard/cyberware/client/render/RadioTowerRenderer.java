package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.block.RadioPostBlock;
import flaxbeard.cyberware.common.block.entity.RadioPostBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Renders a completed Radio Tower from its master block.
 */
public class RadioTowerRenderer implements BlockEntityRenderer<RadioPostBlockEntity> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/models/radio.png");
    private static final ResourceLocation BASE_TEXTURE = CyberwareAPI.id("textures/models/radio_base.png");

    private final ModelPart tower;
    private final ModelPart base;

    public RadioTowerRenderer(BlockEntityRendererProvider.Context context) {
        this.tower = context.bakeLayer(CyberwareModelLayers.RADIO_TOWER);
        this.base = context.bakeLayer(CyberwareModelLayers.RADIO_TOWER_BASE);
    }

    private static boolean isMaster(RadioPostBlockEntity post) {
        BlockState state = post.getBlockState();
        return state.getBlock() instanceof RadioPostBlock && state.getValue(RadioPostBlock.TRANSFORMED) == RadioPostBlock.MASTER;
    }

    @Override
    public void render(RadioPostBlockEntity post, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!isMaster(post)) return;
        poseStack.pushPose();
        poseStack.translate(0.5, RadioPostBlock.TOWER_HEIGHT + 0.5, 0.5);
        tower.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay);
        base.render(poseStack, buffers.getBuffer(RenderType.entityCutout(BASE_TEXTURE)), packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RadioPostBlockEntity post) {
        return isMaster(post);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(RadioPostBlockEntity post) {
        BlockPos pos = post.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + RadioPostBlock.TOWER_HEIGHT, pos.getZ() + 2);
    }
}
