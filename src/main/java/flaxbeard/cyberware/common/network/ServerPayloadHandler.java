package flaxbeard.cyberware.common.network;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.BodyRegion;
import flaxbeard.cyberware.api.item.HotkeyHelper;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.block.entity.EngineeringTableBlockEntity;
import flaxbeard.cyberware.common.block.entity.SurgeryBlockEntity;
import flaxbeard.cyberware.common.menu.EngineeringTableMenu;
import flaxbeard.cyberware.common.menu.FineManipulatorsMenu;
import flaxbeard.cyberware.common.menu.SurgeryMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Handles the payloads sent by clients. Every request is checked against the menu the player has open.
 */
final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    static void handleSurgeryRemove(SurgeryRemovePayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player.containerMenu instanceof SurgeryMenu menu) || payload.slot() < 0 || payload.slot() >= SurgeryBlockEntity.SLOT_COUNT) return;
        SurgeryBlockEntity surgery = menu.getSurgery();
        if (!surgery.getBlockPos().equals(payload.pos())) return;

        int slot = payload.slot();
        BodyRegion region = BodyRegion.values()[slot / CyberwareConstants.WARE_PER_SLOT];
        int slotInRegion = slot % CyberwareConstants.WARE_PER_SLOT;
        ItemStack installed = surgery.slotsPlayer.getStackInSlot(slot);
        surgery.discardSlots[slot] = payload.discard();
        if (payload.discard()) {
            surgery.disableDependants(installed, region, slotInRegion);
        } else {
            surgery.enableDependsOn(installed, region, slotInRegion);
        }
        surgery.updateEssential(region);
        surgery.updateEssence();
        surgery.setChanged();
    }

    static void handleEngineeringDestroy(EngineeringDestroyPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof EngineeringTableMenu menu) {
            EngineeringTableBlockEntity table = menu.getTable();
            if (table.getBlockPos().equals(payload.pos())) {
                table.smash();
            }
        }
    }

    static void handleEngineeringSwitch(EngineeringSwitchPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof EngineeringTableMenu menu) {
            if (payload.isComponentBox()) {
                menu.switchBox(payload.forward());
            } else {
                menu.switchArchive(payload.forward());
            }
        }
    }

    static void handleSyncHotkey(SyncHotkeyPayload payload, IPayloadContext context) {
        CyberwareUserData data = CyberwareAPI.getUserData(context.player());
        if (data == null) return;
        List<ItemStack> activeItems = data.getActiveItems();
        if (payload.selectedPart() < 0 || payload.selectedPart() >= activeItems.size()) return;

        ItemStack stack = activeItems.get(payload.selectedPart());
        if (payload.key() == SyncHotkeyPayload.REMOVE) {
            HotkeyHelper.removeHotkey(data, stack);
        } else {
            HotkeyHelper.removeHotkey(data, payload.key());
            HotkeyHelper.assignHotkey(data, stack, payload.key());
        }
    }

    static void handleTriggerActiveAbility(TriggerActiveAbilityPayload payload, IPayloadContext context) {
        Player player = context.player();
        CyberwareUserData data = CyberwareAPI.getUserData(player);
        if (data != null) {
            CyberwareAPI.useActiveItem(player, data.getCyberware(payload.stack()));
        }
    }

    static void handleSyncHudData(SyncHudDataPayload payload, IPayloadContext context) {
        CyberwareUserData data = CyberwareAPI.getUserData(context.player());
        if (data != null) {
            data.setHudData(payload.data());
        }
    }

    static void handleOpenRadialMenu(IPayloadContext context) {
        CyberwareUserData data = CyberwareAPI.getUserData(context.player());
        if (data != null) {
            data.setOpenedRadialMenu(true);
        }
    }

    static void handleHudColor(HudColorPayload payload, IPayloadContext context) {
        CyberwareUserData data = CyberwareAPI.getUserData(context.player());
        if (data != null) {
            data.setHudColor(payload.color());
        }
    }

    static void handleOpenFineManipulators(IPayloadContext context) {
        FineManipulatorsMenu.open(context.player());
    }
}
