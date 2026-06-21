package tr.cebi.blacktimber;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * BlackTimber entry point.
 *
 * Fells a whole natural tree when a player breaks one of its logs, while leaving
 * player built structures untouched. Players choose per person whether leaves
 * break and whether drops go to their inventory, and broken leaves can roll
 * biome themed bonus loot.
 */
public final class BlackTimber extends JavaPlugin {

    // Read from many region threads, swapped on reload. The record it points to
    // is immutable, so a volatile reference is all the synchronisation needed.
    private volatile BlackTimberConfig settings;

    private UserSettings userSettings;
    private PlacedLogStore placedLogs;
    private LeafLoot leafLoot;
    private TreeFeller feller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = BlackTimberConfig.from(getConfig());
        this.userSettings = new UserSettings(this);
        this.placedLogs = new PlacedLogStore(this);
        this.leafLoot = new LeafLoot(this);
        this.feller = new TreeFeller(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        PluginCommand command = getCommand("blacktimber");
        if (command != null) {
            BlackTimberCommand handler = new BlackTimberCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        new Telemetry(this).start();

        getLogger().info("BlackTimber enabled. Tree felling ready, builds protected.");
    }

    /** Re-reads config.yml from disk and swaps the active settings snapshot. */
    public void reloadSettings() {
        reloadConfig();
        applySettings();
        if (leafLoot != null) {
            leafLoot.reloadCustom();
        }
    }

    /** Rebuilds the settings snapshot from the in-memory config, no file read. */
    public void applySettings() {
        this.settings = BlackTimberConfig.from(getConfig());
    }

    /**
     * Sets one config value, applies it live, then writes config.yml off the
     * region thread so the tick is never blocked on disk I/O.
     */
    public void editConfig(String key, Object value) {
        getConfig().set(key, value);
        applySettings();
        String yaml = getConfig().saveToString();
        java.io.File file = new java.io.File(getDataFolder(), "config.yml");
        org.bukkit.Bukkit.getAsyncScheduler().runNow(this, task -> {
            try {
                java.nio.file.Files.writeString(file.toPath(), yaml);
            } catch (java.io.IOException e) {
                getLogger().warning("Could not save config.yml: " + e.getMessage());
            }
        });
    }

    public BlackTimberConfig settings() {
        return settings;
    }

    public UserSettings userSettings() {
        return userSettings;
    }

    public PlacedLogStore placedLogs() {
        return placedLogs;
    }

    public LeafLoot leafLoot() {
        return leafLoot;
    }

    public TreeFeller feller() {
        return feller;
    }
}
