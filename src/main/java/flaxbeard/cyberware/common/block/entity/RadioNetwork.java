package flaxbeard.cyberware.common.block.entity;

import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.CyberwareUserData;
import flaxbeard.cyberware.api.item.EnableDisableHelper;
import flaxbeard.cyberware.common.CyberwareConstants;
import flaxbeard.cyberware.common.item.BrainUpgradeItem;
import flaxbeard.cyberware.common.registry.CyberwareItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

/**
 * Tracks the active radio transmitters of each level. Radios attract CyberZombies: zombies spawning in range of a
 * radio have a chance to become CyberZombies, higher tiers having higher chances.
 * <ul>
 *     <li>tier 3: Radio Beacon on a Radio Tower</li>
 *     <li>tier 2: Radio Kit</li>
 *     <li>tier 1: Cranial Broadcaster of a nearby player</li>
 * </ul>
 */
public final class RadioNetwork {
    public static final int TIER_PLAYER = 1;
    public static final int TIER_RADIO = 2;
    public static final int TIER_BEACON = 3;
    public static final int NONE = -1;

    /** Per level: tier (highest first) -> transmitter position -> range. Client and server levels are tracked separately. */
    private static final Map<Level, TreeMap<Integer, Map<BlockPos, Integer>>> TRANSMITTERS = new WeakHashMap<>();

    private RadioNetwork() {
    }

    private static Map<BlockPos, Integer> getTransmitters(Level level, int tier) {
        return TRANSMITTERS.computeIfAbsent(level, key -> new TreeMap<>((a, b) -> Integer.compare(b, a)))
                .computeIfAbsent(tier, key -> new HashMap<>());
    }

    public static void enable(Level level, BlockPos pos, int tier, int range) {
        getTransmitters(level, tier).putIfAbsent(pos.immutable(), range);
    }

    public static void disable(Level level, BlockPos pos, int tier) {
        getTransmitters(level, tier).remove(pos);
    }

    /**
     * @return the highest tier of transmitter in range of a position, or {@link #NONE}
     */
    public static int getTierInRange(Level level, double x, double y, double z) {
        TreeMap<Integer, Map<BlockPos, Integer>> tiers = TRANSMITTERS.get(level);
        if (tiers != null) {
            for (Map.Entry<Integer, Map<BlockPos, Integer>> tier : tiers.entrySet()) {
                for (Map.Entry<BlockPos, Integer> transmitter : tier.getValue().entrySet()) {
                    double dx = x - transmitter.getKey().getX();
                    double dz = z - transmitter.getKey().getZ();
                    int range = transmitter.getValue();
                    if (dx * dx + dz * dz < range * range) {
                        return tier.getKey();
                    }
                }
            }
        }

        double range = CyberwareConstants.BEACON_RANGE_INTERNAL;
        AABB area = new AABB(x - range, level.getMinBuildHeight(), z - range, x + range, level.getMaxBuildHeight(), z + range);
        BrainUpgradeItem radioItem = CyberwareItems.CRANIAL_BROADCASTER.get();
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            if (!BrainUpgradeItem.isRadioWorking(player)) continue;
            CyberwareUserData data = CyberwareAPI.getUserData(player);
            if (data == null) continue;
            ItemStack radio = data.getCyberware(radioItem);
            if (!radio.isEmpty() && EnableDisableHelper.isEnabled(radio)) {
                return TIER_PLAYER;
            }
        }
        return NONE;
    }
}
