package flaxbeard.cyberware.common.menu;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.common.item.HandUpgradeItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/**
 * Crafting Hands give access to a 3x3 crafting grid anywhere. The client uses the vanilla crafting screen.
 */
public class FineManipulatorsMenu extends CraftingMenu {
    public FineManipulatorsMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    /**
     * @return whether the player has Crafting Hands on the arm of their main hand
     */
    public static boolean canUse(Player player) {
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        return data != null
                && HandUpgradeItem.hasPrimaryCyberarm(player, data)
                && data.isCyberwareInstalled(CyberwareItems.CRAFT_HANDS.get());
    }

    public static void open(Player player) {
        if (!canUse(player)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, menuPlayer) -> new FineManipulatorsMenu(id, inventory, ContainerLevelAccess.create(menuPlayer.level(), menuPlayer.blockPosition())),
                Component.translatable("cyberware.gui.fine_manipulators")));
    }

    @Override
    public boolean stillValid(Player player) {
        return canUse(player);
    }
}
