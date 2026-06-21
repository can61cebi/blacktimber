package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

/**
 * Admin loot editor. Click an item in your own inventory to add it to the leaf
 * loot; click an entry to nudge its drop rate, or shift click to remove it.
 * Items are never consumed, only copied into the loot table.
 */
public final class AdminLootMenu extends Menu {

    private static final double DEFAULT_CHANCE = 0.05;
    private static final double STEP = 0.005;
    private static final int ENTRY_LIMIT = 45;
    private static final int BACK = 45;
    private static final int HINT = 49;
    private static final int CLOSE = 53;

    public AdminLootMenu(BlackTimber plugin) {
        super(plugin, 6, Buttons.plain("Leaf loot editor", NamedTextColor.DARK_RED));
        redraw();
    }

    private void redraw() {
        inventory.clear();
        List<LeafLoot.CustomEntry> entries = plugin.leafLoot().customEntries();
        int shown = Math.min(entries.size(), ENTRY_LIMIT);
        for (int i = 0; i < shown; i++) {
            inventory.setItem(i, render(entries.get(i)));
        }
        inventory.setItem(BACK, Buttons.button(Material.ARROW, Buttons.plain("Back", NamedTextColor.YELLOW), null));
        inventory.setItem(HINT, Buttons.button(Material.LIME_DYE, Buttons.plain("Add an item", NamedTextColor.GREEN),
                List.of(Buttons.plain("Click any item in your inventory", NamedTextColor.GRAY),
                        Buttons.plain("to add it to the leaf loot.", NamedTextColor.GRAY))));
        inventory.setItem(CLOSE, Buttons.button(Material.BARRIER, Buttons.plain("Close", NamedTextColor.RED), null));
    }

    private ItemStack render(LeafLoot.CustomEntry entry) {
        ItemStack icon = entry.item().clone();
        ItemMeta meta = icon.getItemMeta();
        meta.lore(List.of(
                Buttons.plain("Chance: " + pct(entry.chance()) + " per leaf", NamedTextColor.AQUA),
                Component.empty(),
                Buttons.plain("Left click  +" + pct(STEP), NamedTextColor.GRAY),
                Buttons.plain("Right click -" + pct(STEP), NamedTextColor.GRAY),
                Buttons.plain("Shift click to remove", NamedTextColor.RED)));
        icon.setItemMeta(meta);
        return icon;
    }

    private static String pct(double chance) {
        return String.format(Locale.ROOT, "%.1f%%", chance * 100.0);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == inventory) {
            onTopClick(event, event.getSlot());
            return;
        }
        // Click in the player's own inventory adds that item to the loot.
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            plugin.leafLoot().addCustom(clicked, DEFAULT_CHANCE);
            redraw();
            player.updateInventory();
            player.sendMessage(Buttons.plain("Added " + clicked.getType().name().toLowerCase(Locale.ROOT)
                    + " to leaf loot.", NamedTextColor.GREEN));
        }
    }

    @Override
    protected void onTopClick(InventoryClickEvent event, int slot) {
        Player player = (Player) event.getWhoClicked();
        if (slot == CLOSE) {
            player.getScheduler().run(plugin, task -> player.closeInventory(), null);
            return;
        }
        if (slot == BACK) {
            openLater(player, new AdminMenu(plugin));
            return;
        }
        if (slot == HINT) {
            return;
        }
        if (slot < 0 || slot >= ENTRY_LIMIT) {
            return;
        }
        List<LeafLoot.CustomEntry> entries = plugin.leafLoot().customEntries();
        if (slot >= entries.size()) {
            return;
        }
        if (event.isShiftClick()) {
            plugin.leafLoot().removeCustom(slot);
        } else {
            double delta = event.isRightClick() ? -STEP : STEP;
            plugin.leafLoot().adjustChance(slot, delta, plugin.settings().leafLootMaxChance());
        }
        redraw();
        player.updateInventory();
    }
}
