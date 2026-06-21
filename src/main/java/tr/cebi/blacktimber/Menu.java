package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base for chest GUI menus. The inventory carries this object as its holder, so
 * the shared listener can route events back here. On Folia these events run on
 * the player's region thread, so reads and writes here need no scheduling.
 */
public abstract class Menu implements InventoryHolder {

    protected final BlackTimber plugin;
    protected final Inventory inventory;

    @SuppressWarnings("this-escape")
    protected Menu(BlackTimber plugin, int rows, Component title) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Open for a player. Must be called on the player's region thread. */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getClickedInventory() == inventory) {
            onTopClick(event, event.getSlot());
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void handleClose(InventoryCloseEvent event) {
    }

    protected void onTopClick(InventoryClickEvent event, int slot) {
    }

    // Opening or closing an inventory inside a click handler is forbidden, so
    // hand the switch to the player's scheduler for the next tick.
    protected void openLater(Player player, Menu next) {
        player.getScheduler().run(plugin, task -> next.open(player), null);
    }
}
