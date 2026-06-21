package tr.cebi.blacktimber;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per player on and off switches, stored on the player in persistent data so
 * they survive restarts. Each option falls back to a config default until the
 * player changes it from the menu.
 */
public final class UserSettings {

    public enum Option {
        TIMBER("enabled"),
        LEAVES("break_leaves"),
        PICKUP("auto_pickup");

        private final String pdcKey;

        Option(String pdcKey) {
            this.pdcKey = pdcKey;
        }
    }

    private final BlackTimber plugin;
    private final Map<Option, NamespacedKey> keys = new EnumMap<>(Option.class);

    public UserSettings(BlackTimber plugin) {
        this.plugin = plugin;
        for (Option option : Option.values()) {
            keys.put(option, new NamespacedKey(plugin, option.pdcKey));
        }
    }

    public boolean get(Player player, Option option) {
        Byte stored = player.getPersistentDataContainer().get(keys.get(option), PersistentDataType.BYTE);
        return stored == null ? defaultOf(option) : stored == (byte) 1;
    }

    public void set(Player player, Option option, boolean value) {
        player.getPersistentDataContainer().set(keys.get(option), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    public boolean toggle(Player player, Option option) {
        boolean next = !get(player, option);
        set(player, option, next);
        return next;
    }

    private boolean defaultOf(Option option) {
        BlackTimberConfig cfg = plugin.settings();
        return switch (option) {
            case TIMBER -> cfg.defaultEnabled();
            case LEAVES -> cfg.defaultBreakLeaves();
            case PICKUP -> cfg.defaultAutoPickup();
        };
    }
}
