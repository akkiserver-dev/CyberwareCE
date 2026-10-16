package flaxbeard.cyberware.api;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.api.item.Cyberware;
import flaxbeard.cyberware.api.item.Deconstructable;
import flaxbeard.cyberware.api.item.MenuItem;
import flaxbeard.cyberware.api.item.Quality;
import flaxbeard.cyberware.common.entity.CyberZombie;
import flaxbeard.cyberware.common.network.CyberwareSyncPayload;
import flaxbeard.cyberware.common.registry.CyberwareAttachments;
import flaxbeard.cyberware.common.registry.CyberwareDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CyberwareAPI {
    /**
     * Quality for cyberware scavenged from mobs
     */
    public static final Quality QUALITY_SCAVENGED = new Quality("cyberware.quality.scavenged", "cyberware.quality.scavenged.name_modifier", "scavenged");

    /**
     * Quality for cyberware built at the Engineering Table
     */
    public static final Quality QUALITY_MANUFACTURED = new Quality("cyberware.quality.manufactured");

    private static final Map<Item, Cyberware> LINKED_WARE = new IdentityHashMap<>();

    private CyberwareAPI() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CyberwareMod.MOD_ID, path);
    }

    // ---- function data ----

    /**
     * Gets a copy of the functional data of a piece of cyberware (power storage, oxygen, toggles...).
     * This data is removed when the cyberware is removed from a body and is ignored when comparing stacks.
     * Use {@link #updateFunctionData} to change it.
     */
    public static CompoundTag getFunctionData(ItemStack stack) {
        CustomData data = stack.get(CyberwareDataComponents.FUNCTION_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static void updateFunctionData(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack.isEmpty()) return;
        CustomData.update(CyberwareDataComponents.FUNCTION_DATA.get(), stack, updater);
    }

    /**
     * Clears all functional data so removed cyberware stacks again. Only call on cyberware being removed or reset.
     */
    public static ItemStack sanitize(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.remove(CyberwareDataComponents.FUNCTION_DATA);
        }
        return stack;
    }

    /**
     * @return whether the two stacks are the same cyberware, ignoring count and functional data
     */
    public static boolean areCyberwareStacksEqual(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(sanitize(first.copy()), sanitize(second.copy()));
    }

    // ---- quality ----

    public static ItemStack writeQuality(ItemStack stack, Quality quality) {
        if (!stack.isEmpty()) {
            stack.set(CyberwareDataComponents.QUALITY, quality);
        }
        return stack;
    }

    @Nullable
    public static Quality readQuality(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(CyberwareDataComponents.QUALITY);
    }

    // ---- cyberware lookup ----

    /**
     * Links an item to an instance of {@link Cyberware}. Implementing the interface on the item is preferred,
     * but this allows foreign items (such as the vanilla spider eye) to be installed.
     */
    public static void linkCyberware(Item item, Cyberware link) {
        LINKED_WARE.put(item, link);
    }

    public static boolean isCyberware(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof Cyberware || LINKED_WARE.containsKey(stack.getItem()));
    }

    public static Cyberware getCyberware(ItemStack stack) {
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof Cyberware cyberware) {
                return cyberware;
            }
            Cyberware linked = LINKED_WARE.get(stack.getItem());
            if (linked != null) {
                return linked;
            }
        }
        throw new IllegalArgumentException("Cannot call getCyberware on a non-cyberware item: " + stack);
    }

    public static boolean canDeconstruct(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof Deconstructable deconstructable
                && deconstructable.canDestroy(stack);
    }

    public static List<ItemStack> getComponents(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof Deconstructable deconstructable) {
            List<ItemStack> components = new ArrayList<>();
            for (ItemStack component : deconstructable.getComponents(stack)) {
                components.add(component.copy());
            }
            return components;
        }
        throw new IllegalArgumentException("Cannot call getComponents on a non-deconstructable item: " + stack);
    }

    // ---- user data ----

    /**
     * @return whether this entity is able to have cyberware installed
     */
    public static boolean canHaveCyberware(@Nullable Entity entity) {
        return entity instanceof Player
                || entity instanceof CyberZombie
                || (entity != null && entity.hasData(CyberwareAttachments.USER_DATA));
    }

    /**
     * @return the cyberware of an entity, or {@code null} if the entity can't have cyberware
     */
    @Nullable
    public static CyberwareUserData getUserData(@Nullable Entity entity) {
        if (!canHaveCyberware(entity)) return null;
        return entity.getData(CyberwareAttachments.USER_DATA);
    }

    /**
     * Sends the cyberware data of an entity to its client and all clients tracking it. Server side only.
     */
    public static void updateData(Entity entity) {
        if (entity.level().isClientSide()) return;
        CyberwareUserData data = getUserData(entity);
        if (data == null) return;
        CyberwareSyncPayload payload = new CyberwareSyncPayload(entity.getId(), data.serializeNBT(entity.registryAccess()));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
    }

    public static void sendDataTo(Entity entity, ServerPlayer target) {
        CyberwareUserData data = getUserData(entity);
        if (data == null) return;
        PacketDistributor.sendToPlayer(target, new CyberwareSyncPayload(entity.getId(), data.serializeNBT(entity.registryAccess())));
    }

    public static void useActiveItem(Entity entity, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof MenuItem menuItem) {
            menuItem.use(entity, stack);
        }
    }
}
