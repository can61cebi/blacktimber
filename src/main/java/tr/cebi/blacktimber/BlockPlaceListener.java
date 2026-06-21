package tr.cebi.blacktimber;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Records every log a player places, so the feller knows when a tree has been
 * built on. Runs on the region thread that owns the placed block, where chunk
 * data is safe to touch on Folia. Sapling growth does not fire this event, so
 * grown logs are never marked and stay fellable.
 */
public final class BlockPlaceListener implements Listener {

    private final BlackTimber plugin;

    public BlockPlaceListener(BlackTimber plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.settings().protectPlayerBuilt()) {
            return;
        }
        Block block = event.getBlockPlaced();
        if (Tag.LOGS.isTagged(block.getType())) {
            plugin.placedLogs().add(block);
        }
    }
}
