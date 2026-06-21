package tr.cebi.blacktimber;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BlackTimber entry point.
 *
 * The plugin fells a whole natural tree when a player breaks one of its logs,
 * while leaving player made wooden builds untouched. A build is told apart from
 * a tree by looking for natural (non player placed) leaves attached to the logs.
 */
public final class BlackTimber extends JavaPlugin {

    // Read from many region threads, swapped on reload. The record it points to
    // is immutable, so a volatile reference is all the synchronisation needed.
    private volatile BlackTimberConfig settings;

    private NamespacedKey toggleKey;
    private TreeFeller feller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.toggleKey = new NamespacedKey(this, "enabled");
        this.settings = BlackTimberConfig.from(getConfig());
        this.feller = new TreeFeller(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);

        PluginCommand command = getCommand("blacktimber");
        if (command != null) {
            BlackTimberCommand handler = new BlackTimberCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        getLogger().info("BlackTimber enabled. Tree felling ready, builds protected.");
    }

    /** Re-reads config.yml and swaps the active settings snapshot. */
    public void reloadSettings() {
        reloadConfig();
        this.settings = BlackTimberConfig.from(getConfig());
    }

    public BlackTimberConfig settings() {
        return settings;
    }

    public NamespacedKey toggleKey() {
        return toggleKey;
    }

    public TreeFeller feller() {
        return feller;
    }
}
