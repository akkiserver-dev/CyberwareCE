package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.block.entity.ScannerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Shows the scanned item and the scanning head moving over it.
 */
public class ScannerRenderer implements BlockEntityRenderer<ScannerBlockEntity> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/models/scanner.png");

    private final ModelPart bar;
    private final ModelPart scanner;
    private final ModelPart beam;

    public ScannerRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(CyberwareModelLayers.SCANNER);
        this.bar = root.getChild("bar");
        this.scanner = root.getChild("scanner");
        this.beam = root.getChild("beam");
    }

    private static float getProgress(int from, int to, float time) {
        int difference = Math.abs(to - from);
        if (difference == 0) return 1.0F;
        float timeToTake = difference * 3;
        return (float) Math.cos((Math.PI / 2) * (1F - Math.max(0, Math.min(timeToTake, time)) / timeToTake));
    }

    @Override
    public void render(ScannerBlockEntity scannerEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(.5, .5, .5);

        ItemStack stack = scannerEntity.slots.getStackInSlot(ScannerBlockEntity.SLOT_INPUT);
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0F, -1.6F / 16F, 0F);
            poseStack.scale(.8F, .8F, .8F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffers,
                    scannerEntity.getLevel(), 0);
            poseStack.popPose();
        }

        float time = scannerEntity.ticks + partialTick - scannerEntity.ticksMove;
        float progressX = getProgress(scannerEntity.lastX, scannerEntity.x, time);
        poseStack.translate(0F, 0F, ((scannerEntity.lastX + (scannerEntity.x - scannerEntity.lastX) * progressX) + 1.5F) / 16F);
        bar.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay);

        float progressZ = getProgress(scannerEntity.lastZ, scannerEntity.z, time);
        poseStack.translate(((scannerEntity.lastZ + (scannerEntity.z - scannerEntity.lastZ) * progressZ) + .5F) / 16F, 0F, 0F);
        scanner.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay);

        if (scannerEntity.ticks > 0 && progressX >= 1F && progressZ >= 1F && ((int) (scannerEntity.ticks + partialTick)) % 2 == 0) {
            beam.render(poseStack, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), packedLight, packedOverlay);
        }
        poseStack.popPose();
    }
}
