package flaxbeard.cyberware.client.render;

import flaxbeard.cyberware.api.CyberwareAPI;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The block entity, armor and attachment models, converted from the original model classes
 * with the same coordinates and texture offsets.
 */
public final class CyberwareModelLayers {
    public static final ModelLayerLocation SKIN_BOX = layer("skin_box");
    public static final ModelLayerLocation SURGERY_CHAMBER_DOOR = layer("surgery_chamber_door");
    public static final ModelLayerLocation ENGINEERING_PRESS = layer("engineering_press");
    public static final ModelLayerLocation SCANNER = layer("scanner");
    public static final ModelLayerLocation RADIO_TOWER = layer("radio_tower");
    public static final ModelLayerLocation RADIO_TOWER_BASE = layer("radio_tower_base");
    public static final ModelLayerLocation CLAWS = layer("claws");
    public static final ModelLayerLocation TRENCH_COAT = layer("trench_coat");

    /** Angle of the four legs of the radio tower. */
    private static final float TOWER_ANGLE = 173.8F;

    private CyberwareModelLayers() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(CyberwareAPI.id(name), "main");
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SKIN_BOX, CyberwareModelLayers::skinBox);
        event.registerLayerDefinition(SURGERY_CHAMBER_DOOR, CyberwareModelLayers::surgeryChamberDoor);
        event.registerLayerDefinition(ENGINEERING_PRESS, CyberwareModelLayers::engineeringPress);
        event.registerLayerDefinition(SCANNER, CyberwareModelLayers::scanner);
        event.registerLayerDefinition(RADIO_TOWER, CyberwareModelLayers::radioTower);
        event.registerLayerDefinition(RADIO_TOWER_BASE, CyberwareModelLayers::radioTowerBase);
        event.registerLayerDefinition(CLAWS, CyberwareModelLayers::claws);
        event.registerLayerDefinition(TRENCH_COAT, CyberwareModelLayers::trenchCoat);
    }

    private static LayerDefinition skinBox() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("box", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -4.5F, -6F, 12, 9, 12), PartPose.ZERO);
        return LayerDefinition.create(mesh, 48, 21);
    }

    private static LayerDefinition surgeryChamberDoor() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(0F, -22F, -1F, 6, 28, 1), PartPose.ZERO);
        root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-6F, -22F, -1F, 6, 28, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 14, 29);
    }

    private static LayerDefinition engineeringPress() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -2F, -3F, 6, 2, 6), PartPose.ZERO);
        head.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 8).addBox(-1F, 0F, -1F, 2, 7, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 24, 17);
    }

    private static LayerDefinition scanner() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bar = root.addOrReplaceChild("bar", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, 7F, -7F, 16, 1, 1), PartPose.ZERO);
        bar.addOrReplaceChild("bar2", CubeListBuilder.create().texOffs(0, 0).addBox(-8F, 5F, -7F, 16, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("scanner", CubeListBuilder.create().texOffs(0, 2).addBox(-7F, 2F, -8F, 3, 5, 3), PartPose.ZERO);
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(12, 2).addBox(-6F, -2F, -7F, 1, 4, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 34, 10);
    }

    private static LayerDefinition radioTower() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float xRot = (float) Math.toRadians(TOWER_ANGLE);
        float[] legAngles = {45F, 135F, -45F, -135F};
        for (int leg = 0; leg < legAngles.length; leg++) {
            root.addOrReplaceChild("bar" + leg, CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 8F, -2F, 3, 163, 3),
                    PartPose.rotation(xRot, (float) Math.toRadians(legAngles[leg]), 0));
        }

        // horizontal crossbars between the legs
        float heightPercent = (float) -Math.cos(Math.toRadians(TOWER_ANGLE));
        float widthPercent = (float) Math.sin(Math.toRadians(TOWER_ANGLE));
        float progressStep = 25F;
        float progress = 10F + progressStep;
        float quarterPi = (float) Math.PI / 4F;
        for (int level = 0; level < 6; level++) {
            float x = (float) Math.ceil(.3F + -widthPercent * progress * quarterPi);
            float z = -1.6F + -widthPercent * progress * (quarterPi - .1F);
            float y = -heightPercent * progress;
            int length = (int) (-x * 2F);
            root.addOrReplaceChild("crossbar" + level + "_0", CubeListBuilder.create().texOffs(12, 0).addBox(x, y, z, length, 2, 2), PartPose.ZERO);
            root.addOrReplaceChild("crossbar" + level + "_1", CubeListBuilder.create().texOffs(12, 0).addBox(x, y, -z - 2F, length, 2, 2), PartPose.ZERO);
            root.addOrReplaceChild("crossbar" + level + "_2", CubeListBuilder.create().texOffs(12, 0).addBox(z, y, x, 2, 2, length), PartPose.ZERO);
            root.addOrReplaceChild("crossbar" + level + "_3", CubeListBuilder.create().texOffs(12, 0).addBox(-z - 2F, y, x, 2, 2, length), PartPose.ZERO);
            progress += progressStep;
        }
        return LayerDefinition.create(mesh, 128, 256);
    }

    private static LayerDefinition radioTowerBase() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-24F, -168F, -24F, 48, 4, 48), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 64);
    }

    private static LayerDefinition claws() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition claw1 = mesh.getRoot().addOrReplaceChild("claw1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 10.0F, -1.8F, 1, 7, 1),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        claw1.addOrReplaceChild("claw2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 10.0F, -0.3F, 1, 7, 1), PartPose.ZERO);
        claw1.addOrReplaceChild("claw3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, 10.0F, 1.2F, 1, 7, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static LayerDefinition trenchCoat() {
        CubeDeformation deformation = new CubeDeformation(0.51F);
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        mesh.getRoot().addOrReplaceChild(TrenchCoatModel.BOTTOM, CubeListBuilder.create().texOffs(16, 0).addBox(-4.0F, 0F, -1.7F, 8, 12, 4, deformation),
                PartPose.offset(0, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
