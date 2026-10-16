package flaxbeard.cyberware.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.client.render.CyberwareModelLayers;
import flaxbeard.cyberware.common.CyberwareConfig;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.menu.SurgeryMenu;
import flaxbeard.cyberware.common.menu.SurgeryMenu.SurgerySlot;
import flaxbeard.cyberware.common.network.SurgeryRemovePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Robosurgeon screen. The main page shows the patient's body; clicking a body part zooms into it and its
 * locations show the slots of a body region. The upper (red) row of a page shows what is installed, the lower
 * (blue) row what will be installed. The index page lists all changes.
 */
public class SurgeryScreen extends AbstractContainerScreen<SurgeryMenu> {
    private static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/surgery.png");
    private static final ResourceLocation BOX_TEXTURE = CyberwareAPI.id("textures/models/skin.png");
    private static final int TEXT_COLOR = 0x1DA9C1;
    private static final int GREY = 0xFF8D8D8D;
    private static final int BLUE = 0xFF1DA9C1;

    private static final int PAGE_MAIN = 0;
    private static final int BUTTON_BACK = 8;
    private static final int PAGE_INDEX = 9;

    private static final int INDEX_SIZE = 5 * 8;

    /** Inventory of the patient when the screen was last opened, to only play the scan animation after changes. */
    private static List<ItemStack> lastLastInventory = new ArrayList<>();
    private static List<ItemStack> lastInventory = new ArrayList<>();

    private final SurgeryBlockEntity surgery;
    private final PageConfiguration[] configs = new PageConfiguration[25];
    private PageConfiguration current;
    private PageConfiguration target;
    private PageConfiguration ease;

    @Nullable
    private Skeleton skeleton;
    @Nullable
    private ModelPart box;

    private final SurgeryButton[] bodyIcons = new SurgeryButton[7];
    private final LocationButton[] headIcons = new LocationButton[3];
    private final LocationButton[] torsoIcons = new LocationButton[4];
    private final LocationButton[] crossSectionIcons = new LocationButton[3];
    private final LocationButton[] armIcons = new LocationButton[2];
    private final LocationButton[] legIcons = new LocationButton[2];
    private InterfaceButton back;
    private InterfaceButton index;

    private final List<ItemStack> indexStacks = new ArrayList<>();
    private final int[] indexPages = new int[INDEX_SIZE];
    private final int[] indexChanges = new int[INDEX_SIZE];

    private float partialTick;
    private float lastTicks;
    private float addedRotate;
    private float oldRotate;
    private float transitionStart = 0;
    private float operationTime = 0;
    private float amountDone = 1;
    private float openTime = 0;
    private int page = PAGE_MAIN;
    private int parent;
    private boolean mouseDown;
    private int mouseDownX;
    private final float[] lastDownX = new float[5];
    private float rotateVelocity = 0;
    private final List<SurgerySlot> visibleSlots = new ArrayList<>();

    public SurgeryScreen(SurgeryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.surgery = menu.getSurgery();
        this.imageHeight = 222;

        configs[0] = new PageConfiguration(0, 0, 0, 50, 35, 35, -50, 10);
        configs[1] = new PageConfiguration(50, 0, 210, 150, 0, 0, -150, 0);
        configs[2] = new PageConfiguration(15, 0, 100, 130, 0, 0, -150, 0);
        configs[3] = new PageConfiguration(-50, 0, 100, 130, 0, 0, -150, 0);
        configs[4] = new PageConfiguration(50, 0, 100, 130, 0, 0, -150, 0);
        configs[5] = new PageConfiguration(-70, 0, 10, 130, 0, 0, -150, 0);
        configs[6] = new PageConfiguration(70, 0, 10, 130, 0, 0, -150, 0);
        configs[7] = new PageConfiguration(0, 0, 0, 50, 170, 125, 0, 0);

        configs[11] = new PageConfiguration(160, 0, 300, 200);
        configs[12] = new PageConfiguration(5, 0, 330, 220);
        configs[13] = new PageConfiguration(5, 0, 330, 220);
        configs[14] = new PageConfiguration(-20, 0, 220, 210);
        configs[15] = new PageConfiguration(0, 0, 180, 180);
        configs[16] = new PageConfiguration(0, 0, 180, 180);
        configs[17] = new PageConfiguration(0, 0, 125, 180);
        configs[18] = new PageConfiguration(0, 0, 0, 50, 190, 180, 0, 0);
        configs[19] = new PageConfiguration(0, 0, 0, 50, 170, 180, 0, 0);
        configs[20] = new PageConfiguration(0, 0, 0, 50, 170, 180, 0, 0);
        configs[21] = new PageConfiguration(-70, 0, 180, 200);
        configs[22] = new PageConfiguration(-70, 0, 120, 220);
        configs[23] = new PageConfiguration(10, 0, 20, 200);
        configs[24] = new PageConfiguration(10, 0, -30, 220);

        current = ease = target = configs[0].copy();
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos;
        int top = topPos;
        bodyIcons[0] = addRenderableWidget(new SurgeryButton(1, left + imageWidth / 2 - 18, top + 8, 36, 27));
        bodyIcons[1] = addRenderableWidget(new SurgeryButton(2, left + imageWidth / 2 - 13, top + 35, 26, 38));
        bodyIcons[2] = addRenderableWidget(new SurgeryButton(3, left + imageWidth / 2 - 8 + 21, top + 35, 16, 38));
        bodyIcons[3] = addRenderableWidget(new SurgeryButton(4, left + imageWidth / 2 - 8 - 21, top + 35, 16, 38));
        bodyIcons[4] = addRenderableWidget(new SurgeryButton(5, left + imageWidth / 2 - 6 + 7, top + 73, 12, 39));
        bodyIcons[5] = addRenderableWidget(new SurgeryButton(6, left + imageWidth / 2 - 6 - 7, top + 73, 12, 39));
        back = addRenderableWidget(new InterfaceButton(BUTTON_BACK, left + imageWidth - 25, top + 5, InterfaceType.BACK));
        index = addRenderableWidget(new InterfaceButton(PAGE_INDEX, left + imageWidth - 22, top + 5, InterfaceType.INDEX));
        back.visible = false;
        bodyIcons[6] = addRenderableWidget(new SurgeryButton(7,
                left + (int) (imageWidth / 2 + configs[0].boxX - configs[0].boxWidth / 2),
                top + (int) (125F / 2F + 3F + configs[0].boxY - configs[0].boxHeight / 2),
                (int) configs[0].boxWidth, (int) configs[0].boxHeight));

        headIcons[0] = addRenderableWidget(new LocationButton(11, -2F, 19, 0));
        headIcons[1] = addRenderableWidget(new LocationButton(12, 4F, 21, 2.F));
        headIcons[2] = addRenderableWidget(new LocationButton(13, 4F, 21, -2F));
        torsoIcons[0] = addRenderableWidget(new LocationButton(14, 1F, 8, -1F));
        torsoIcons[1] = addRenderableWidget(new LocationButton(15, 0F, 9, -2F));
        torsoIcons[2] = addRenderableWidget(new LocationButton(16, 0F, 9, 2F));
        torsoIcons[3] = addRenderableWidget(new LocationButton(17, 0F, 13, 0F));
        crossSectionIcons[0] = addRenderableWidget(new LocationButton(18, -12F, -8, -1F));
        crossSectionIcons[1] = addRenderableWidget(new LocationButton(19, 12F, -1, 2F));
        crossSectionIcons[2] = addRenderableWidget(new LocationButton(20, 3F, 5, 12F));
        armIcons[0] = addRenderableWidget(new LocationButton(21, 0F, 10, -5.3F));
        armIcons[1] = addRenderableWidget(new LocationButton(22, 0F, 16, -6.0F));
        legIcons[0] = addRenderableWidget(new LocationButton(23, 0F, 1, -2.2F));
        legIcons[1] = addRenderableWidget(new LocationButton(24, 0F, 6.4F, -2.2F));

        showHideRelevantButtons(page == PAGE_MAIN || transitionStart == 0);
    }

    // ---- page navigation ----

    private float ticksExisted() {
        return minecraft.player != null ? minecraft.player.tickCount : 0;
    }

    private void prepTransition(int time, int targetPage) {
        if (page == PAGE_INDEX) {
            if (targetPage == PAGE_MAIN) {
                back.visible = false;
                page = PAGE_MAIN;
                showHideRelevantButtons(true);
                ease = current = configs[0].copy();
                return;
            }
            if ((targetPage >= 18 && targetPage <= 20) || time == 0) {
                ease = current = configs[targetPage].copy();
                page = targetPage;
                showHideRelevantButtons(true);
                return;
            }
            ease = current = configs[0].copy();
        }

        if (targetPage == PAGE_INDEX) {
            showHideRelevantButtons(false);
            page = PAGE_INDEX;
            parent = PAGE_MAIN;
            back.visible = true;
            index.visible = false;
            buildIndex();
            return;
        }

        transitionStart = ticksExisted() + partialTick;
        current = ease;
        operationTime = amountDone * time;
        showHideRelevantButtons(false);
        page = targetPage;
        target = configs[page].copy();
        if (page == PAGE_MAIN) {
            back.visible = false;
        } else {
            back.visible = true;
            index.visible = false;
        }
    }

    /**
     * Lists every installed or planned cyberware: 0 unchanged, 1 added, 2 removed.
     */
    private void buildIndex() {
        indexStacks.clear();
        for (int slot = 0; slot < SurgeryBlockEntity.SLOT_COUNT && indexStacks.size() < INDEX_SIZE; slot++) {
            ItemStack playerStack = surgery.slotsPlayer.getStackInSlot(slot);
            ItemStack surgeryStack = surgery.slots.getStackInSlot(slot);
            int pageId = BodyRegion.values()[slot / CyberwareConstants.WARE_PER_SLOT].getPageId();

            int change = 0;
            ItemStack draw = ItemStack.EMPTY;
            if (!surgeryStack.isEmpty()) {
                draw = surgeryStack.copy();
                if (!playerStack.isEmpty()) {
                    if (CyberwareAPI.areCyberwareStacksEqual(playerStack, surgeryStack)) {
                        draw.grow(playerStack.getCount());
                    } else {
                        addIndexEntry(playerStack.copy(), pageId, 2);
                        if (indexStacks.size() >= INDEX_SIZE) break;
                    }
                }
                change = 1;
            } else if (!playerStack.isEmpty()) {
                draw = playerStack.copy();
                change = surgery.discardSlots[slot] ? 2 : 0;
            }
            if (!draw.isEmpty()) {
                addIndexEntry(draw, pageId, change);
            }
        }
    }

    private void addIndexEntry(ItemStack stack, int pageId, int change) {
        indexPages[indexStacks.size()] = pageId;
        indexChanges[indexStacks.size()] = change;
        indexStacks.add(stack);
    }

    private void onButtonPressed(int id) {
        if (id == BUTTON_BACK) {
            if (page != PAGE_MAIN || ease.rotation != 0) {
                prepTransition(20, page <= 10 ? PAGE_MAIN : parent);
            }
            return;
        }
        openTime = 1;
        if (id > 10) {
            parent = page;
        }
        int targetPage = switch (id) {
            case 4 -> 3;
            case 6 -> 5;
            case 13 -> 12;
            case 16 -> 15;
            default -> id;
        };
        prepTransition(20, targetPage);
    }

    private AbstractButton[] getPageButtons() {
        return switch (page) {
            case 0 -> bodyIcons;
            case 1 -> headIcons;
            case 2 -> torsoIcons;
            case 3 -> armIcons;
            case 5 -> legIcons;
            case 7 -> crossSectionIcons;
            default -> new AbstractButton[0];
        };
    }

    private void showHideRelevantButtons(boolean show) {
        for (AbstractButton button : getPageButtons()) {
            if (button != null) button.visible = show;
        }
        updateSurgerySlotsVisibility(show);
    }

    private boolean isSlotAccessible(SurgerySlot slot) {
        return page == slot.region.getPageId();
    }

    private void updateSurgerySlotsVisibility(boolean show) {
        visibleSlots.clear();
        for (Slot slot : menu.slots) {
            if (slot instanceof SurgerySlot surgerySlot) {
                surgerySlot.visible = show && isSlotAccessible(surgerySlot);
                if (surgerySlot.isActive()) {
                    visibleSlots.add(surgerySlot);
                }
            }
        }
    }

    private void updateLocationButtons(float rotation, float scale, float yOffset) {
        if (page == PAGE_MAIN) {
            index.visible = true;
        }
        LocationButton[] buttons = switch (page) {
            case 1 -> headIcons;
            case 2 -> torsoIcons;
            case 3 -> armIcons;
            case 5 -> legIcons;
            case 7 -> crossSectionIcons;
            default -> new LocationButton[0];
        };
        if (page == 7) {
            rotation += addedRotate;
        }
        float radians = (float) Math.toRadians(rotation);
        float sin = (float) Math.sin(radians);
        float cos = -(float) Math.cos(radians);
        float upDown = page == 7 ? (float) Math.sin(Math.toRadians(10)) : 0;
        for (LocationButton button : buttons) {
            button.xPos = leftPos + sin * scale * button.x3 * 0.065F + cos * scale * button.z3 * 0.065F + imageWidth / 2F - 2.0F - button.getWidth() / 2F;
            button.yPos = -upDown * cos * scale * button.x3 * 0.065F + upDown * sin * scale * button.z3 * 0.065F
                    + topPos + 2 - yOffset + scale * button.y3 * 0.065F + 130 / 2F - button.getHeight() / 2F;
            button.setX(Math.round(button.xPos));
            button.setY(Math.round(button.yPos));
        }
    }

    // ---- rendering ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // the tooltip of the hovered slot item (inventory and planned cyberware)
        renderTooltip(graphics, mouseX, mouseY);
        renderSurgeryTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        this.partialTick = partialTick;
        float time = ticksExisted() + partialTick;

        // only play the scan animation if the body changed since the last time
        if (openTime == 0) {
            List<ItemStack> installed = new ArrayList<>();
            boolean equal = lastLastInventory.size() == SurgeryBlockEntity.SLOT_COUNT;
            for (int slot = 0; slot < SurgeryBlockEntity.SLOT_COUNT; slot++) {
                ItemStack stack = surgery.slotsPlayer.getStackInSlot(slot);
                installed.add(stack);
                if (equal && !ItemStack.isSameItem(stack, lastLastInventory.get(slot))) {
                    equal = false;
                }
            }
            if (!equal) {
                openTime = time;
                lastLastInventory = lastInventory;
                lastInventory = installed;
            } else {
                openTime = 1;
            }
        }

        int left = leftPos;
        int top = topPos;
        graphics.blit(TEXTURE, left, top, 0, 0, imageWidth, imageHeight);

        if (skeleton == null || skeleton.isRemoved()) {
            skeleton = EntityType.SKELETON.create(minecraft.level);
        }
        if (box == null) {
            box = minecraft.getEntityModels().bakeLayer(CyberwareModelLayers.SKIN_BOX);
        }

        if (transitionStart != 0) {
            // make sure to rotate the shortest way
            current.rotation = current.rotation % 360;
            if (Math.abs(current.rotation + 360 - target.rotation) < Math.abs(current.rotation - target.rotation)) {
                current.rotation += 360;
            } else if (Math.abs(current.rotation - (target.rotation + 360)) < Math.abs(current.rotation - target.rotation)) {
                current.rotation -= 360;
            }
            amountDone = (time - transitionStart) / operationTime;
            ease = interpolate(amountDone, current, target);
            if (amountDone >= 1.0F) {
                transitionStart = 0;
                ease = target;
                showHideRelevantButtons(true);
            }
        }

        // drag to rotate the body, unless slots are shown
        if (mouseDown && page <= 10) {
            ease.rotation = oldRotate + (mouseX - mouseDownX) % 360;
            System.arraycopy(lastDownX, 0, lastDownX, 1, 4);
            lastDownX[0] = mouseX;
        } else {
            if (page > 10) {
                rotateVelocity = 0;
            }
            ease.rotation += rotateVelocity % 360;
            rotateVelocity *= 0.8F;
        }
        float endRotate = ease.rotation;

        if (page != PAGE_INDEX) {
            float scanned = Math.min(1.0F, (time - openTime) / 40F);
            if (scanned < 1.0F) {
                ease.rotation = (float) (Math.sin(Math.PI * scanned / 2F) * 360F);
            }

            graphics.enableScissor(left + 3, top + 3, left + 3 + 170, top + 3 + 125);

            // scan line
            if (scanned < 0.9F) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 90);
                graphics.blit(TEXTURE, left + imageWidth / 2 - 40, top + (int) (scanned * 125F) + 2, 176, 110, 80, 1);
                graphics.pose().popPose();
            }

            if (ease.boxHeight >= 35) {
                renderCrossSection(graphics, left, top, endRotate);
            }
            graphics.disableScissor();

            if (skeleton != null) {
                int split = (int) (scanned * 125);
                graphics.enableScissor(left + 3, top + 3, left + 3 + 170, top + 3 + split);
                renderEntity(graphics, skeleton, left + imageWidth / 2F + ease.x, top + 110 + ease.y, ease.scale, endRotate);
                graphics.disableScissor();

                graphics.enableScissor(left + 3, top + 3 + split, left + 3 + 170, top + 3 + 125);
                renderPatient(graphics, left, top, time);
                graphics.disableScissor();
            }
        }

        updateLocationButtons(endRotate, ease.scale, ease.y);
        drawSlots(graphics, left, top);
    }

    /**
     * The box showing a cross section of the skin, muscles and bones.
     */
    private void renderCrossSection(GuiGraphics graphics, int left, int top, float endRotate) {
        float boxHeight = Math.min(125, ease.boxHeight);
        float boxWidth = Math.min(170, ease.boxWidth);
        float boxLeft = left + imageWidth / 2F + ease.boxX - boxWidth / 2F;
        float boxTop = top + 125F / 2F + 3F + ease.boxY - boxHeight / 2F;

        graphics.pose().pushPose();
        graphics.pose().translate(boxLeft, boxTop, page == PAGE_MAIN ? 90 : 70);
        graphics.fill(0, 0, (int) boxWidth, (int) boxHeight, GREY);
        graphics.fill(0, 0, (int) boxWidth, 1, BLUE);
        graphics.fill(0, (int) boxHeight - 1, (int) boxWidth, (int) boxHeight, BLUE);
        graphics.fill(0, 0, 1, (int) boxHeight, BLUE);
        graphics.fill((int) boxWidth - 1, 0, (int) boxWidth, (int) boxHeight, BLUE);
        if (ease.boxHeight == 35) {
            // the connector to the body
            int connectorX = (int) (configs[0].boxWidth / 2F);
            graphics.fill(connectorX, -12, connectorX + 1, 0, BLUE);
            graphics.fill(connectorX + 1, -12, connectorX + 26, -11, BLUE);
        }
        graphics.pose().popPose();

        if (!mouseDown && page < 10) {
            addedRotate = (addedRotate + (ticksExisted() + partialTick - lastTicks) * 2f) % 360;
        }
        lastTicks = ticksExisted() + partialTick;

        if (box != null) {
            float scale = ease.boxHeight / 50F * 40F;
            graphics.pose().pushPose();
            graphics.pose().translate(left + imageWidth / 2F + ease.boxX, top + 125F / 2F + 3F + ease.boxY, 120F);
            graphics.pose().scale(scale, scale, -scale);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
            graphics.pose().mulPose(Axis.XP.rotationDegrees(-10.0F));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(endRotate + addedRotate + 180F));
            Lighting.setupForEntityInInventory();
            box.render(graphics.pose(), graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(BOX_TEXTURE)), 0xF000F0, OverlayTexture.NO_OVERLAY);
            graphics.flush();
            graphics.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

    private void renderPatient(GuiGraphics graphics, int left, int top, float time) {
        LocalPlayer player = minecraft.player;
        if (player == null || skeleton == null) return;
        float bodyRot = player.yBodyRot;
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float headRotO = player.yHeadRotO;
        float headRot = player.yHeadRot;
        float attackAnim = player.attackAnim;

        player.yBodyRot = 0;
        player.setYRot(skeleton.getYRot());
        player.setXRot(0);
        player.yHeadRotO = 0;
        player.yHeadRot = skeleton.getYHeadRot();
        player.attackAnim = 0F;

        renderEntity(graphics, player, left + imageWidth / 2F + ease.x, top + 115 + ease.y * (60F / 63F), ease.scale * (57F / 50F),
                ease.rotation + (float) (5F * Math.sin(time / 25F)));

        player.attackAnim = attackAnim;
        player.yBodyRot = bodyRot;
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.yHeadRotO = headRotO;
        player.yHeadRot = headRot;
    }

    private void renderEntity(GuiGraphics graphics, LivingEntity entity, float x, float y, float scale, float rotation) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 50.0F);
        graphics.pose().scale(scale, scale, -scale);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        graphics.pose().mulPose(Axis.YP.rotationDegrees(rotation + 180.0F));
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0, 0, 0, 0.0F, 1.0F, graphics.pose(), graphics.bufferSource(), 0xF000F0));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        graphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private List<Component> getWarnings() {
        List<Component> warnings = new ArrayList<>();
        if (surgery.essence < 0) {
            warnings.add(Component.translatable("cyberware.gui.no_essence"));
        } else if (surgery.essence < CyberwareConfig.get(minecraft.level).criticalEssence()) {
            warnings.add(Component.translatable("cyberware.gui.critical_essence"));
        }
        if (surgery.missingPower) {
            warnings.add(Component.translatable("cyberware.gui.no_power"));
        }
        for (int index = 0; index < surgery.isEssentialMissing.length; index++) {
            if (!surgery.isEssentialMissing[index]) continue;
            BodyRegion region = BodyRegion.values()[index / 2];
            if (region.isSided()) {
                warnings.add(Component.translatable("cyberware.gui.missing_essential." + region.getName() + (index % 2 == 0 ? ".left" : ".right")));
            } else if (index % 2 == 0) {
                warnings.add(Component.translatable("cyberware.gui.missing_essential." + region.getName()));
            }
        }
        return warnings;
    }

    private void drawSlots(GuiGraphics graphics, int left, int top) {
        int maxEssence = Math.max(1, surgery.maxEssence);
        int essence = (int) ((surgery.essence * 1F / maxEssence) * 49);
        int criticalEssence = (int) ((CyberwareConfig.get(minecraft.level).criticalEssence() * 1F / maxEssence) * 49);
        int warningEssence = criticalEssence;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        RenderSystem.enableBlend();
        // drawn above the body model, but must not hide the items that are rendered later at a lower depth
        RenderSystem.depthMask(false);

        if (surgery.wrongSlot != -1 && surgery.getLevel() != null) {
            float alpha = 1.0F - (surgery.getLevel().getGameTime() + partialTick - surgery.ticksWrong) / 10F;
            if (alpha > 0) {
                Slot slot = menu.slots.get(surgery.wrongSlot);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                graphics.blit(TEXTURE, left + slot.x - 5, top + slot.y - 5, 185, 61, 26, 26);
            } else {
                surgery.wrongSlot = -1;
            }
        }

        // the less transparent slot borders
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        for (SurgerySlot slot : visibleSlots) {
            graphics.blit(TEXTURE, left + slot.x - 1, top + slot.y - 1, 176, 43, 18, 18);
            graphics.blit(TEXTURE, left + slot.x - 1, top + slot.y - 1 - 26, 176, 18, 18, 25);
        }

        if (page != PAGE_INDEX) {
            // the essence bar: normal, warning and critical parts, then the used up essence in grey
            essence = Math.max(0, Math.min(49, essence));
            blitIfPositive(graphics, left + 5, top + 5 + (49 - essence), 176, 61 + (49 - essence), 9, essence - warningEssence);
            int warning = Math.min(warningEssence, essence);
            blitIfPositive(graphics, left + 5, top + 5 + (49 - warning), 229, 61 + (49 - warning), 9, warning - criticalEssence);
            int critical = Math.min(criticalEssence, essence);
            blitIfPositive(graphics, left + 5, top + 5 + (49 - critical), 220, 61 + (49 - critical), 9, critical);
            blitIfPositive(graphics, left + 5, top + 5, 211, 61, 9, 49 - essence);
        } else {
            graphics.pose().translate(0, 0, -150);
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 5; row++) {
                    int entry = column + row * 8;
                    int v = entry < indexStacks.size() && indexChanges[entry] == 2 ? 18 : 43;
                    graphics.blit(TEXTURE, left + 20 * column + 9 - 1, top + 20 * row + 50 - 1 - 26, 176, v, 18, 18);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.2F);
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 5; row++) {
                    int entry = column + row * 8;
                    int v = entry < indexStacks.size() && indexChanges[entry] == 2 ? 18 : 43;
                    graphics.blit(TEXTURE, left + 20 * column + 9 - 1, top + 20 * row + 50 - 1 - 26, 176 + 18, v, 18, 18);
                }
            }
            graphics.pose().translate(0, 0, 150);
        }

        // the more transparent slot backgrounds
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.2F);
        for (SurgerySlot slot : visibleSlots) {
            graphics.blit(TEXTURE, left + slot.x - 1, top + slot.y - 1, 176 + 18, 43, 18, 18);
            graphics.blit(TEXTURE, left + slot.x - 1, top + slot.y - 1 - 26, 176 + 18, 18, 18, 18);
        }

        if (page != PAGE_INDEX && !getWarnings().isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
            graphics.blit(TEXTURE, left + imageWidth - 23, top + 20, 212, 43, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        graphics.pose().popPose();
    }

    private static void blitIfPositive(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        if (width > 0 && height > 0) {
            graphics.blit(TEXTURE, x, y, u, v, width, height);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400F);
        if (page == PAGE_MAIN && transitionStart == 0) {
            String name = "_" + minecraft.player.getName().getString().toUpperCase();
            graphics.drawString(font, name, imageWidth / 2 - font.width(name) / 2, 115, TEXT_COLOR, false);
        }
        if (page == PAGE_INDEX) {
            graphics.drawString(font, Component.translatable("cyberware.gui.installed"), 8, 9, TEXT_COLOR, false);
        } else {
            graphics.drawString(font, surgery.essence + " / " + surgery.maxEssence, 18, 6, TEXT_COLOR, false);
        }
        graphics.pose().popPose();

        // red (installed) items above the slots, and ghost items of installed cyberware that stays
        for (SurgerySlot slot : visibleSlots) {
            ItemStack playerStack = slot.getPlayerStack();
            graphics.renderItem(playerStack, slot.x, slot.y - 26);
            graphics.renderItemDecorations(font, playerStack, slot.x, slot.y - 26);

            if (slot.getItem().isEmpty() && !slot.isDiscarded()) {
                graphics.renderFakeItem(playerStack, slot.x, slot.y);
                graphics.fill(RenderType.guiGhostRecipeOverlay(), slot.x, slot.y, slot.x + 16, slot.y + 16, 0x30FFFFFF);
                graphics.renderItemDecorations(font, playerStack, slot.x, slot.y);
            } else if (CyberwareAPI.areCyberwareStacksEqual(playerStack, slot.getItem())) {
                int count = slot.getItem().getCount();
                String text = count == 1 ? "+1" : "+";
                int width = count == 1 ? 0 : font.width(Integer.toString(count));
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 200);
                graphics.renderItemDecorations(font, playerStack, slot.x - width, slot.y, text);
                graphics.pose().popPose();
            }
        }

        if (page == PAGE_INDEX) {
            for (int entry = 0; entry < indexStacks.size(); entry++) {
                ItemStack stack = indexStacks.get(entry);
                int x = (entry % 8) * 20 + 9;
                int y = (entry / 8) * 20 + 24;
                graphics.renderFakeItem(stack, x, y);
                graphics.renderItemDecorations(font, stack, x, y);
                if (indexChanges[entry] == 1) {
                    graphics.renderItemDecorations(font, stack, x - 10, y - 10, "+");
                } else if (indexChanges[entry] == 2) {
                    graphics.renderItemDecorations(font, stack, x - 10, y - 10, "-");
                }
            }
        }
    }

    private void renderSurgeryTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (page != PAGE_INDEX) {
            boolean ghost = false;
            boolean add = false;
            // see if a red slot is hovered
            Slot slot = getSlotAt(mouseX, mouseY + 26);
            if (!(slot instanceof SurgerySlot)) {
                // otherwise, see if a blue slot with a ghost item is hovered
                ghost = true;
                slot = getSlotAt(mouseX, mouseY);
                if (slot != null && !slot.getItem().isEmpty()) {
                    slot = null;
                }
                if (slot instanceof SurgerySlot surgerySlot && surgerySlot.isDiscarded() && !surgerySlot.getPlayerStack().isEmpty()) {
                    if (menu.getCarried().isEmpty()) {
                        add = true;
                    } else {
                        slot = null;
                    }
                }
            }

            if (slot instanceof SurgerySlot surgerySlot) {
                ItemStack stack = surgerySlot.getPlayerStack();
                if (add) {
                    graphics.renderTooltip(font, Component.translatable("cyberware.gui.add", stack.getHoverName()), mouseX, mouseY);
                } else if (!stack.isEmpty()) {
                    renderStackTooltip(graphics, stack, mouseX, mouseY, ghost ? 1 : 0);
                }
            }

            if (!getWarnings().isEmpty() && isHovering(imageWidth - 23, 20, 16, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, getWarnings(), mouseX, mouseY);
            }
        } else {
            for (int entry = 0; entry < indexStacks.size(); entry++) {
                int x = (entry % 8) * 20 + 9;
                int y = (entry / 8) * 20 + 24;
                if (isHovering(x - 1, y - 1, 18, 18, mouseX, mouseY)) {
                    renderStackTooltip(graphics, indexStacks.get(entry), mouseX, mouseY, 2 + indexChanges[entry]);
                }
            }
        }

        if (page != PAGE_MAIN && isHovering(imageWidth - 25, 5, back.getWidth(), back.getHeight(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.back"), mouseX, mouseY);
        } else if (page == PAGE_MAIN && isHovering(imageWidth - 22, 5, index.getWidth(), index.getHeight(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("cyberware.gui.index"), mouseX, mouseY);
        }
    }

    /**
     * @param extras 1: removal hint; 2+: click hint, 3: added, 4: removed
     */
    private void renderStackTooltip(GuiGraphics graphics, ItemStack stack, int mouseX, int mouseY, int extras) {
        List<Component> lines = new ArrayList<>(stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player,
                minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
        if (extras == 1) {
            lines.add(1, Component.translatable("cyberware.gui.remove").withStyle(ChatFormatting.GRAY));
        } else if (extras >= 2) {
            lines.add(1, Component.translatable("cyberware.gui.click").withStyle(ChatFormatting.GRAY));
            if (extras == 3) {
                lines.set(0, lines.get(0).copy().append(" ").append(Component.translatable("cyberware.gui.added")));
            } else if (extras == 4) {
                lines.set(0, lines.get(0).copy().append(" ").append(Component.translatable("cyberware.gui.removed")));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Nullable
    private Slot getSlotAt(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == PAGE_INDEX && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int entry = 0; entry < indexStacks.size(); entry++) {
                int x = (entry % 8) * 20 + 9;
                int y = (entry / 8) * 20 + 24;
                if (isHovering(x - 1, y - 1, 18, 18, mouseX, mouseY)) {
                    parent = PAGE_INDEX;
                    prepTransition(hasShiftDown() ? 0 : 30, indexPages[entry]);
                    return true;
                }
            }
        }

        // drag to rotate
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + 130) {
            oldRotate = ease.rotation;
            mouseDown = true;
            mouseDownX = (int) mouseX;
            java.util.Arrays.fill(lastDownX, mouseDownX);
        }

        // right click to go back
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && (page != PAGE_MAIN || ease.rotation != 0)
                && getSlotAt(mouseX, mouseY) == null && mouseY < topPos + 130) {
            prepTransition(20, page <= 10 ? PAGE_MAIN : parent);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseDown) {
            mouseDown = false;
            rotateVelocity = (float) (mouseX - lastDownX[4]);
            if (Math.abs(rotateVelocity) < 5) {
                rotateVelocity = 0;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot instanceof SurgerySlot surgerySlot) {
            if (!isSlotAccessible(surgerySlot)) return;

            // clicking an installed item toggles whether it is removed
            if (surgerySlot.getItem().isEmpty() && !surgerySlot.getPlayerStack().isEmpty() && menu.getCarried().isEmpty()) {
                int number = surgerySlot.getSurgeryIndex();
                int slotInRegion = number % CyberwareConstants.WARE_PER_SLOT;
                ItemStack playerStack = surgerySlot.getPlayerStack();
                if (surgerySlot.isDiscarded()) {
                    if (!surgery.doesItemConflict(playerStack, surgerySlot.region, slotInRegion)) {
                        surgerySlot.setDiscarded(false);
                        surgery.enableDependsOn(playerStack, surgerySlot.region, slotInRegion);
                        PacketDistributor.sendToServer(new SurgeryRemovePayload(surgery.getBlockPos(), number, false));
                    }
                } else if (surgery.canDisableItem(playerStack, surgerySlot.region, slotInRegion)) {
                    surgerySlot.setDiscarded(true);
                    surgery.disableDependants(playerStack, surgerySlot.region, slotInRegion);
                    PacketDistributor.sendToServer(new SurgeryRemovePayload(surgery.getBlockPos(), number, true));
                }
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    // ---- animation helpers ----

    private static PageConfiguration interpolate(float amountDone, PageConfiguration start, PageConfiguration end) {
        float progress = Math.min(1.0F, amountDone);
        return new PageConfiguration(
                ease(progress, start.rotation, end.rotation),
                ease(progress, start.x, end.x),
                ease(progress, start.y, end.y),
                ease(progress, start.scale, end.scale),
                ease(progress, start.boxWidth, end.boxWidth),
                ease(progress, start.boxHeight, end.boxHeight),
                ease(progress, start.boxX, end.boxX),
                ease(progress, start.boxY, end.boxY));
    }

    /**
     * Quadratic ease in/out, see http://stackoverflow.com/a/8317722/1754640
     */
    private static float ease(float percent, float startValue, float endValue) {
        endValue -= startValue;
        float elapsed = percent * 2;
        if (elapsed < 1) {
            return endValue / 2 * elapsed * elapsed + startValue;
        }
        elapsed--;
        return -endValue / 2 * (elapsed * (elapsed - 2) - 1) + startValue;
    }

    private static class PageConfiguration {
        private float rotation;
        private final float x;
        private final float y;
        private final float scale;
        private final float boxWidth;
        private final float boxHeight;
        private final float boxX;
        private final float boxY;

        private PageConfiguration(float rotation, float x, float y, float scale) {
            this(rotation, x, y, scale, 0, 0, 0, 0);
        }

        private PageConfiguration(float rotation, float x, float y, float scale, float boxWidth, float boxHeight, float boxX, float boxY) {
            this.rotation = rotation;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.boxX = boxX;
            this.boxY = boxY;
        }

        private PageConfiguration copy() {
            return new PageConfiguration(rotation, x, y, scale, boxWidth, boxHeight, boxX, boxY);
        }
    }

    // ---- buttons ----

    private abstract class PageButton extends AbstractButton {
        protected final int id;

        private PageButton(int id, int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            this.id = id;
        }

        @Override
        public void onPress() {
            onButtonPressed(id);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /**
     * An invisible clickable body part on the main page.
     */
    private class SurgeryButton extends PageButton {
        private SurgeryButton(int id, int x, int y, int width, int height) {
            super(id, x, y, width, height);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // no visuals
        }
    }

    /**
     * A marker on a 3D location of the zoomed body, opening the page of a body region.
     */
    private class LocationButton extends PageButton {
        private static final int SIZE = 16;
        private final float x3;
        private final float y3;
        private final float z3;
        private float xPos;
        private float yPos;

        private LocationButton(int id, float x3, float y3, float z3) {
            super(id, 0, 0, SIZE, SIZE);
            this.x3 = x3;
            this.y3 = y3;
            this.z3 = z3;
            this.visible = false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, isHovered() ? 0.6F : 0.4F);
            graphics.pose().pushPose();
            graphics.pose().translate(xPos, yPos, 300);
            graphics.blit(TEXTURE, 0, 0, 194, 0, SIZE, SIZE);
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private enum InterfaceType {
        BACK(176, 111, 18, 10),
        INDEX(176, 122, 12, 11);

        private final int u;
        private final int v;
        private final int width;
        private final int height;

        InterfaceType(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
    }

    private class InterfaceButton extends PageButton {
        private final InterfaceType type;

        private InterfaceButton(int id, int x, int y, InterfaceType type) {
            super(id, x, y, type.width, type.height);
            this.type = type;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float alpha = isHovered() ? 0.6F : 0.4F;
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(TEXTURE, getX(), getY(), type.u + type.width, type.v, type.width, type.height);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 2F);
            graphics.blit(TEXTURE, getX(), getY(), type.u, type.v, type.width, type.height);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            graphics.pose().popPose();
        }

        @Override
        public void onPress() {
            if (type == InterfaceType.INDEX) {
                openTime = 1;
                prepTransition(20, PAGE_INDEX);
            } else {
                super.onPress();
            }
        }
    }
}
