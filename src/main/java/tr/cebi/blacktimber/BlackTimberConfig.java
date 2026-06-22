package tr.cebi.blacktimber;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * Immutable snapshot of config.yml. Built once on enable and on every reload,
 * then shared across region threads without further locking.
 */
public record BlackTimberConfig(
        boolean requireNaturalLeaves,
        int minNaturalLeaves,
        int leafSearchRadius,
        int leafClearBudget,
        boolean searchDiagonal,
        int maxLogs,
        boolean requireAxe,
        SneakRequirement sneakRequirement,
        boolean survivalOnly,
        boolean defaultEnabled,
        boolean defaultBreakLeaves,
        boolean defaultAutoPickup,
        boolean defaultReplant,
        boolean applyDurability,
        boolean respectUnbreaking,
        boolean breakTool,
        int staggerThreshold,
        int logsPerTick,
        boolean protectPlayerBuilt,
        boolean protectStructures,
        int structureBlockThreshold,
        int maxTrackedPerChunk,
        boolean leafLootEnabled,
        double leafLootMultiplier,
        double leafLootMaxChance
) {
    public enum SneakRequirement { IGNORE, REQUIRED, FORBIDDEN }

    public static BlackTimberConfig from(FileConfiguration c) {
        return new BlackTimberConfig(
                c.getBoolean("require-natural-leaves", true),
                Math.max(0, c.getInt("min-natural-leaves", 1)),
                Math.max(0, c.getInt("leaf-search-radius", 1)),
                Math.max(64, c.getInt("leaf-clear-budget", 8192)),
                c.getBoolean("search-diagonal", true),
                Math.max(1, c.getInt("max-logs", 150)),
                c.getBoolean("require-axe", true),
                parseSneak(c.getString("sneak-requirement", "ignore")),
                c.getBoolean("survival-only", true),
                c.getBoolean("default-enabled", true),
                c.getBoolean("default-break-leaves", false),
                c.getBoolean("default-auto-pickup", false),
                c.getBoolean("default-replant", false),
                c.getBoolean("apply-durability", true),
                c.getBoolean("respect-unbreaking", true),
                c.getBoolean("break-tool", true),
                Math.max(1, c.getInt("stagger-threshold", 64)),
                Math.max(1, c.getInt("logs-per-tick", 16)),
                c.getBoolean("protect-player-built", true),
                c.getBoolean("protect-structures", true),
                Math.max(1, c.getInt("structure-block-threshold", 1)),
                Math.max(1, c.getInt("max-tracked-per-chunk", 4096)),
                c.getBoolean("leaf-loot-enabled", true),
                Math.max(0.0, c.getDouble("leaf-loot-multiplier", 1.0)),
                clamp01(c.getDouble("leaf-loot-max-chance", 0.25))
        );
    }

    private static double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }

    private static SneakRequirement parseSneak(String value) {
        if (value == null) {
            return SneakRequirement.IGNORE;
        }
        try {
            return SneakRequirement.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SneakRequirement.IGNORE;
        }
    }
}
