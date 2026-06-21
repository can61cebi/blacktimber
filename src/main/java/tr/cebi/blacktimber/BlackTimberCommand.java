package tr.cebi.blacktimber;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The single /blacktimber command: per player on/off/toggle/status plus an
 * admin reload. Kept deliberately small.
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
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("blacktimber.admin")) {
                    deny(sender);
                    return true;
                }
                plugin.reloadSettings();
                send(sender, "Configuration reloaded.", NamedTextColor.GREEN);
            }
            case "on" -> setState(sender, Boolean.TRUE);
            case "off" -> setState(sender, Boolean.FALSE);
            case "toggle" -> setState(sender, null);
            case "status" -> status(sender);
            default -> send(sender, "Usage: /blacktimber <on|off|toggle|status|reload>", NamedTextColor.GRAY);
        }
        return true;
    }

    private void setState(CommandSender sender, Boolean desired) {
        if (!(sender instanceof Player player)) {
            send(sender, "Only players can change this setting.", NamedTextColor.RED);
            return;
        }
        if (!player.hasPermission("blacktimber.use")) {
            deny(sender);
            return;
        }
        boolean next = desired != null ? desired : !isEnabledFor(player);
        player.getPersistentDataContainer()
                .set(plugin.toggleKey(), PersistentDataType.BYTE, (byte) (next ? 1 : 0));
        send(sender, "Tree felling " + (next ? "enabled" : "disabled") + " for you.",
                next ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    private void status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "Run this in game to see your felling state.", NamedTextColor.GRAY);
            return;
        }
        boolean on = isEnabledFor(player);
        send(sender, "Tree felling is " + (on ? "enabled" : "disabled") + " for you.",
                on ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    private boolean isEnabledFor(Player player) {
        Byte state = player.getPersistentDataContainer().get(plugin.toggleKey(), PersistentDataType.BYTE);
        return state == null ? plugin.settings().defaultEnabled() : state == (byte) 1;
    }

    private void deny(CommandSender sender) {
        send(sender, "You do not have permission for that.", NamedTextColor.RED);
    }

    private void send(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(PREFIX.append(Component.text(message, color)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String start = args[0].toLowerCase(Locale.ROOT);
        Stream<String> options = Stream.of("on", "off", "toggle", "status");
        if (sender.hasPermission("blacktimber.admin")) {
            options = Stream.concat(options, Stream.of("reload"));
        }
        return options.filter(option -> option.startsWith(start)).toList();
    }
}
