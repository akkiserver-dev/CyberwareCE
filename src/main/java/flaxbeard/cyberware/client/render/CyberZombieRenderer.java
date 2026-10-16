package flaxbeard.cyberware.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.entity.CyberZombie;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * A zombie with cyberware textures and glowing highlights. Brutes are scaled up by their scale attribute.
 */
public class CyberZombieRenderer extends AbstractZombieRenderer<CyberZombie, ZombieModel<CyberZombie>> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/entity/cyberzombie.png");
    private static final ResourceLocation HIGHLIGHT = CyberwareAPI.id("textures/entity/cyberzombie_highlight.png");
    private static final ResourceLocation BRUTE_TEXTURE = CyberwareAPI.id("textures/entity/cyberzombie_brute.png");
    private static final ResourceLocation BRUTE_HIGHLIGHT = CyberwareAPI.id("textures/entity/cyberzombie_brute_highlight.png");

    public CyberZombieRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
        addLayer(new HighlightLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return zombie instanceof CyberZombie cyberZombie && cyberZombie.isBrute() ? BRUTE_TEXTURE : TEXTURE;
    }

    /**
     * The glowing parts of the cyberware, rendered at full brightness.
     */
    private static class HighlightLayer extends RenderLayer<CyberZombie, ZombieModel<CyberZombie>> {
        private HighlightLayer(RenderLayerParent<CyberZombie, ZombieModel<CyberZombie>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, CyberZombie zombie,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (zombie.isInvisible()) return;
            ResourceLocation texture = zombie.isBrute() ? BRUTE_HIGHLIGHT : HIGHLIGHT;
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(texture)), 0xF00000, OverlayTexture.NO_OVERLAY);
        }
    }
}
