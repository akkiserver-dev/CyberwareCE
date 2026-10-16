package flaxbeard.cyberware.client.render;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * Armor model of the trench coat, with an extra skirt that follows the legs.
 */
public class TrenchCoatModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final String BOTTOM = "bottom";

    private final ModelPart bottom;

    public TrenchCoatModel(ModelPart root) {
        super(root);
        this.bottom = root.getChild(BOTTOM);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.<ModelPart>builder().addAll(super.bodyParts()).add(bottom).build();
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // the armor layer only copies the poses of the standard parts, so the skirt is posed from the legs here
        bottom.visible = body.visible;
        bottom.setPos(0, leftLeg.y, leftLeg.z);
        bottom.xRot = Math.max(leftLeg.xRot, rightLeg.xRot) + 0.055F;
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
