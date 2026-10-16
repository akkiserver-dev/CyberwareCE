package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.block.EngineeringTableBlock;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The press of the Engineering Table and the item placed under it.
 */
public class EngineeringTableRenderer implements BlockEntityRenderer<EngineeringTableBlockEntity> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/models/engineering.png");

    private final ModelPart head;

    public EngineeringTableRenderer(BlockEntityRendererProvider.Context context) {
        this.head = context.bakeLayer(CyberwareModelLayers.ENGINEERING_PRESS).getChild("head");
    }

    @Override
    public void render(EngineeringTableBlockEntity table, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = table.getBlockState();
        Minecraft minecraft = Minecraft.getInstance();
        if (!(state.getBlock() instanceof EngineeringTableBlock) || minecraft.player == null) return;

        // the press goes down quickly when smashing, then slowly back up; the item is hidden while crushed
        float elapsed = Math.clamp(minecraft.player.tickCount + partialTick - table.clickedTime, 0, 22);
        boolean showItem = true;
        float amount;
        if (elapsed < 2) {
            amount = elapsed / 2F;
        } else {
            elapsed -= 2;
            if (elapsed < 15) {
                showItem = false;
            }
            amount = 1F - elapsed / 20F;
        }

        poseStack.pushPose();
        poseStack.translate(.5, .5, .5);

        poseStack.pushPose();
        poseStack.translate(0F, amount * (-6F / 16F), 0F);
        head.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay);
        poseStack.popPose();

        ItemStack stack = table.slots.getStackInSlot(EngineeringTableBlockEntity.SLOT_DESTROY);
        if (!stack.isEmpty() && showItem) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(SurgeryChamberRenderer.getFacingRotation(state.getValue(EngineeringTableBlock.FACING))));
            poseStack.translate(0F, -7.6F / 16F, -1.8F / 16F);
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffers, table.getLevel(), 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
