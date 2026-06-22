package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin config panel. Boolean options toggle on click; numeric options step up
 * on left click and down on right click, with shift for a larger step. Every
 * change is written through {@link BlackTimber#editConfig}.
 */
public final class AdminMenu extends Menu {

    private record Opt(String key, boolean numeric, boolean decimal, String label, Material icon,
                       double min, double max, double step) {
        static Opt flag(String key, String label, Material icon) {
            return new Opt(key, false, false, label, icon, 0, 0, 0);
        }

        static Opt count(String key, String label, Material icon, double min, double max, double step) {
            return new Opt(key, true, false, label, icon, min, max, step);
        }

        static Opt decimal(String key, String label, Material icon, double min, double max, double step) {
            return new Opt(key, true, true, label, icon, min, max, step);
        }
    }

    private static final List<Opt> OPTIONS = List.of(
            Opt.flag("default-enabled", "Default tree felling", Material.IRON_AXE),
            Opt.flag("default-break-leaves", "Default break leaves", Material.OAK_LEAVES),
            Opt.flag("default-auto-pickup", "Default auto pickup", Material.HOPPER),
            Opt.flag("default-replant", "Default replant saplings", Material.OAK_SAPLING),
            Opt.flag("require-natural-leaves", "Require natural leaves", Material.AZALEA_LEAVES),
            Opt.count("min-natural-leaves", "Min natural leaves", Material.SPRUCE_LEAVES, 0, 64, 1),
            Opt.flag("search-diagonal", "Search diagonal", Material.COMPASS),
            Opt.count("max-logs", "Max logs per fell", Material.OAK_LOG, 1, 2000, 10),
            Opt.flag("require-axe", "Require axe", Material.STONE_AXE),
            Opt.flag("survival-only", "Survival only", Material.GRASS_BLOCK),
            Opt.flag("apply-durability", "Apply durability", Material.ANVIL),
            Opt.flag("respect-unbreaking", "Respect Unbreaking", Material.ENCHANTED_BOOK),
            Opt.flag("break-tool", "Allow tool to break", Material.DAMAGED_ANVIL),
            Opt.flag("protect-player-built", "Protect placed logs", Material.CHEST),
            Opt.flag("protect-structures", "Protect structures", Material.OAK_STAIRS),
            Opt.count("structure-block-threshold", "Structure threshold", Material.OAK_PLANKS, 1, 32, 1),
            Opt.flag("leaf-loot-enabled", "Leaf loot", Material.APPLE),
            Opt.decimal("leaf-loot-multiplier", "Leaf loot multiplier", Material.EXPERIENCE_BOTTLE, 0, 20, 0.1)
    );

    private final Map<Integer, Opt> slots = new HashMap<>();

    public AdminMenu(BlackTimber plugin) {
        super(plugin, 6, Buttons.plain("BlackTimber admin", NamedTextColor.DARK_RED));
        redraw();
    }

    private void redraw() {
        slots.clear();
        inventory.clear();
        int index = 0;
        for (int row = 1; row <= 4 && index < OPTIONS.size(); row++) {
            for (int col = 1; col <= 7 && index < OPTIONS.size(); col++) {
                int slot = row * 9 + col;
                Opt opt = OPTIONS.get(index++);
                slots.put(slot, opt);
                inventory.setItem(slot, render(opt));
            }
        }
        inventory.setItem(49, Buttons.button(Material.BARRIER, Buttons.plain("Close", NamedTextColor.RED), null));
        inventory.setItem(53, Buttons.button(Material.WRITABLE_BOOK, Buttons.plain("Leaf loot editor", NamedTextColor.GOLD),
                List.of(Buttons.plain("Add items and set drop rates.", NamedTextColor.GRAY))));
    }

    private ItemStack render(Opt opt) {
        List<Component> lore = new ArrayList<>();
        if (opt.numeric()) {
            String value = opt.decimal()
                    ? String.format(Locale.ROOT, "%.2f", plugin.getConfig().getDouble(opt.key()))
                    : String.valueOf(plugin.getConfig().getInt(opt.key()));
            lore.add(Buttons.plain("Value: " + value, NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(Buttons.plain("Left click  +" + trim(opt.step()), NamedTextColor.GRAY));
            lore.add(Buttons.plain("Right click -" + trim(opt.step()), NamedTextColor.GRAY));
            lore.add(Buttons.plain("Shift for x10", NamedTextColor.DARK_GRAY));
            return Buttons.button(opt.icon(), Buttons.plain(opt.label(), NamedTextColor.WHITE), lore);
        }
        boolean on = plugin.getConfig().getBoolean(opt.key());
        lore.add(Buttons.plain(on ? "ON" : "OFF", on ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Buttons.plain("Click to toggle", NamedTextColor.YELLOW));
        return Buttons.button(opt.icon(), Buttons.plain(opt.label(), on ? NamedTextColor.GREEN : NamedTextColor.RED), lore);
    }

    private static String trim(double step) {
        if (step == Math.floor(step)) {
            return String.valueOf((int) step);
        }
        return String.format(Locale.ROOT, "%.2f", step);
    }

    @Override
    protected void onTopClick(InventoryClickEvent event, int slot) {
        Player player = (Player) event.getWhoClicked();
        if (slot == 49) {
            player.getScheduler().run(plugin, task -> player.closeInventory(), null);
            return;
        }
        if (slot == 53) {
            openLater(player, new AdminLootMenu(plugin));
            return;
        }
        Opt opt = slots.get(slot);
        if (opt == null) {
            return;
        }
        if (!opt.numeric()) {
            plugin.editConfig(opt.key(), !plugin.getConfig().getBoolean(opt.key()));
        } else {
            double current = plugin.getConfig().getDouble(opt.key());
            double step = opt.step() * (event.isShiftClick() ? 10 : 1);
            double next = event.isRightClick() ? current - step : current + step;
            next = Math.max(opt.min(), Math.min(opt.max(), next));
            if (opt.decimal()) {
                plugin.editConfig(opt.key(), Math.round(next * 100.0) / 100.0);
            } else {
                plugin.editConfig(opt.key(), (int) Math.round(next));
            }
        }
        redraw();
        player.updateInventory();
    }
}
