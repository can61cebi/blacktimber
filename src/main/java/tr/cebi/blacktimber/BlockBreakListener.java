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
 * Reacts to a player breaking a log. On Folia this handler runs on the region
 * thread that owns the broken block, so the fell can be done in place.
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

        if (!player.hasPermission("blacktimber.use")) {
            return;
        }
        if (cfg.survivalOnly() && player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        if (!isEnabledFor(player, cfg)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (cfg.requireAxe() && !isAxe(tool)) {
            return;
        }

        switch (cfg.sneakRequirement()) {
            case REQUIRED -> { if (!player.isSneaking()) return; }
            case FORBIDDEN -> { if (player.isSneaking()) return; }
            case IGNORE -> { }
        }

        plugin.feller().fell(player, block, tool, cfg);
    }

    private boolean isEnabledFor(Player player, BlackTimberConfig cfg) {
        Byte state = player.getPersistentDataContainer().get(plugin.toggleKey(), PersistentDataType.BYTE);
        return state == null ? cfg.defaultEnabled() : state == (byte) 1;
    }

    private static boolean isAxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_AXE");
    }
}
