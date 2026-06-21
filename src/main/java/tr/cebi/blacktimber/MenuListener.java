package tr.cebi.blacktimber;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Routes inventory events to the {@link Menu} that owns the inventory. Identity
 * comes from the holder, never the title, so look-alike inventories cannot drive
 * our menus.
 */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) {
            menu.handleClose(event);
        }
    }
}
