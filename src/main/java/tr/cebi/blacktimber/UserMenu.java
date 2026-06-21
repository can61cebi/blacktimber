package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Per player menu: three toggles plus a note about leaf loot rewards. */
public final class UserMenu extends Menu {

    private final Player player;

    public UserMenu(BlackTimber plugin, Player player) {
        super(plugin, 3, Buttons.plain("BlackTimber", NamedTextColor.DARK_GREEN));
        this.player = player;
        redraw();
    }

    private void redraw() {
        inventory.setItem(10, toggle(UserSettings.Option.TIMBER, Material.IRON_AXE, "Tree felling",
                "Break one log to fell the whole tree."));
        inventory.setItem(13, toggle(UserSettings.Option.LEAVES, Material.OAK_LEAVES, "Break leaves",
                "Also clear the tree's leaves when it falls.",
                "While on, leaves can drop biome themed bonus loot."));
        inventory.setItem(16, toggle(UserSettings.Option.PICKUP, Material.HOPPER, "Auto pickup",
                "Send the drops straight to your inventory."));
    }

    private ItemStack toggle(UserSettings.Option option, Material icon, String label, String... description) {
        boolean on = plugin.userSettings().get(player, option);
        List<Component> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(Buttons.plain(line, NamedTextColor.GRAY));
        }
        lore.add(Component.empty());
        lore.add(Buttons.plain(on ? "Status: ON" : "Status: OFF", on ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Buttons.plain("Click to toggle", NamedTextColor.YELLOW));
        return Buttons.button(icon, Buttons.plain(label, on ? NamedTextColor.GREEN : NamedTextColor.RED), lore);
    }

    @Override
    protected void onTopClick(InventoryClickEvent event, int slot) {
        UserSettings.Option option = switch (slot) {
            case 10 -> UserSettings.Option.TIMBER;
            case 13 -> UserSettings.Option.LEAVES;
            case 16 -> UserSettings.Option.PICKUP;
            default -> null;
        };
        if (option == null) {
            return;
        }
        plugin.userSettings().toggle(player, option);
        redraw();
        player.updateInventory();
    }
}
