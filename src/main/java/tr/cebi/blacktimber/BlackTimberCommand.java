package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The /blacktimber command. Players manage their personal switches (tree felling,
 * leaf breaking, auto pickup, replant); admins reload the config. The menu is the
 * nicer front end for the same switches.
 */
public final class BlackTimberCommand implements TabExecutor {

    private static final Component PREFIX =
            Component.text("[BlackTimber] ", NamedTextColor.DARK_GREEN);

    private final BlackTimber plugin;

    public BlackTimberCommand(BlackTimber plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            openUserMenu(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "ui" -> openUserMenu(sender);
            case "admin" -> openAdminMenu(sender);
            case "reload" -> {
                if (!sender.hasPermission("blacktimber.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.reloadSettings();
                send(sender, "Configuration reloaded.", NamedTextColor.GREEN);
            }
            case "on" -> applyOption(sender, UserSettings.Option.TIMBER, Boolean.TRUE);
            case "off" -> applyOption(sender, UserSettings.Option.TIMBER, Boolean.FALSE);
            case "toggle" -> applyOption(sender, UserSettings.Option.TIMBER, null);
            case "timber" -> applyOption(sender, UserSettings.Option.TIMBER, parseState(args));
            case "leaves" -> applyOption(sender, UserSettings.Option.LEAVES, parseState(args));
            case "pickup" -> applyOption(sender, UserSettings.Option.PICKUP, parseState(args));
            case "replant" -> applyOption(sender, UserSettings.Option.REPLANT, parseState(args));
            case "status" -> status(sender);
            default -> send(sender, "Usage: /blacktimber <status|on|off|leaves|pickup|replant|reload>", NamedTextColor.GRAY);
        }
        return true;
    }

    // null means toggle, TRUE or FALSE means set to that value.
    private Boolean parseState(String[] args) {
        if (args.length < 2) {
            return null;
        }
        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable" -> Boolean.TRUE;
            case "off", "false", "disable" -> Boolean.FALSE;
            default -> null;
        };
    }

    private void applyOption(CommandSender sender, UserSettings.Option option, Boolean desired) {
        if (!(sender instanceof Player player)) {
            send(sender, "Only players can change this setting.", NamedTextColor.RED);
            return;
        }
        if (!player.hasPermission("blacktimber.use")) {
            deny(sender);
            return;
        }
        boolean next = desired != null ? desired : !plugin.userSettings().get(player, option);
        plugin.userSettings().set(player, option, next);
        send(sender, label(option) + " " + (next ? "enabled" : "disabled") + " for you.",
                next ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
        if (option == UserSettings.Option.LEAVES && next) {
            send(sender, "Breaking leaves can now drop bonus loot themed to the biome.", NamedTextColor.GRAY);
        }
        if (option == UserSettings.Option.REPLANT && next) {
            send(sender, "A matching sapling will be replanted where each tree stood.", NamedTextColor.GRAY);
        }
    }

    private void openUserMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            status(sender);
            return;
        }
        if (!player.hasPermission("blacktimber.use")) {
            deny(sender);
            return;
        }
        new UserMenu(plugin, player).open(player);
    }

    private void openAdminMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "Open the admin panel in game.", NamedTextColor.GRAY);
            return;
        }
        if (!player.hasPermission("blacktimber.admin")) {
            deny(sender);
            return;
        }
        new AdminMenu(plugin).open(player);
    }

    private void status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "Run this in game to see your settings.", NamedTextColor.GRAY);
            return;
        }
        send(sender, "Your BlackTimber settings:", NamedTextColor.AQUA);
        line(player, "Tree felling", UserSettings.Option.TIMBER);
        line(player, "Break leaves", UserSettings.Option.LEAVES);
        line(player, "Auto pickup", UserSettings.Option.PICKUP);
        line(player, "Replant saplings", UserSettings.Option.REPLANT);
    }

    private void line(Player player, String label, UserSettings.Option option) {
        boolean on = plugin.userSettings().get(player, option);
        player.sendMessage(Component.text("  " + label + ": ", NamedTextColor.GRAY)
                .append(Component.text(on ? "on" : "off", on ? NamedTextColor.GREEN : NamedTextColor.RED)));
    }

    private static String label(UserSettings.Option option) {
        return switch (option) {
            case TIMBER -> "Tree felling";
            case LEAVES -> "Break leaves";
            case PICKUP -> "Auto pickup";
            case REPLANT -> "Replant saplings";
        };
    }

    private void deny(CommandSender sender) {
        send(sender, "You do not have permission for that.", NamedTextColor.RED);
    }

    private void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(PREFIX.append(Component.text(message, color)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            List<String> subs = new ArrayList<>(List.of("menu", "status", "on", "off", "toggle", "timber", "leaves", "pickup", "replant"));
            if (sender.hasPermission("blacktimber.admin")) {
                subs.add("admin");
                subs.add("reload");
            }
            for (String option : subs) {
                if (option.startsWith(start)) {
                    out.add(option);
                }
            }
        } else if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("timber") || first.equals("leaves") || first.equals("pickup") || first.equals("replant")) {
                String start = args[1].toLowerCase(Locale.ROOT);
                for (String option : List.of("on", "off", "toggle")) {
                    if (option.startsWith(start)) {
                        out.add(option);
                    }
                }
            }
        }
        return out;
    }
}
