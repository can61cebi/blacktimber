package tr.cebi.blacktimber;

import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reacts to a player breaking a log. On Folia this runs on the region thread
 * that owns the block, so the fell and the placed log bookkeeping both happen
 * in place.
 */
public final class BlockBreakListener implements Listener {

    private final BlackTimber plugin;

    public BlockBreakListener(BlackTimber plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!Tag.LOGS.isTagged(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        BlackTimberConfig cfg = plugin.settings();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (shouldFell(player, tool, cfg)) {
            plugin.feller().fell(player, block, tool, cfg);
        }

        // A placed log that is now broken is no longer placed. Run this after the
        // fell decision so the feller still sees this block as player placed.
        if (cfg.protectPlayerBuilt()) {
            plugin.placedLogs().remove(block);
        }
    }

    private boolean shouldFell(Player player, ItemStack tool, BlackTimberConfig cfg) {
        if (!player.hasPermission("blacktimber.use")) {
            return false;
        }
        if (cfg.survivalOnly() && player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        }
        if (!isEnabledFor(player, cfg)) {
            return false;
        }
        if (cfg.requireAxe() && !isAxe(tool)) {
            return false;
        }
        return switch (cfg.sneakRequirement()) {
            case REQUIRED -> player.isSneaking();
            case FORBIDDEN -> !player.isSneaking();
            case IGNORE -> true;
        };
    }

    private boolean isEnabledFor(Player player, BlackTimberConfig cfg) {
        Byte state = player.getPersistentDataContainer().get(plugin.toggleKey(), PersistentDataType.BYTE);
        return state == null ? cfg.defaultEnabled() : state == (byte) 1;
    }

    private static boolean isAxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }
}
