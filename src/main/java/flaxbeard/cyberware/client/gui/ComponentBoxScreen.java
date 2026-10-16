package flaxbeard.cyberware.client.gui;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.common.menu.ComponentBoxMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ComponentBoxScreen extends ChestLikeScreen<ComponentBoxMenu> {
    public static final ResourceLocation TEXTURE = CyberwareAPI.id("textures/gui/component_box.png");

    public ComponentBoxScreen(ComponentBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TEXTURE);
    }
}
