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
        boolean searchDiagonal,
        int maxLogs,
        boolean requireAxe,
        SneakRequirement sneakRequirement,
        boolean survivalOnly,
        boolean defaultEnabled,
        boolean applyDurability,
        boolean respectUnbreaking,
        boolean breakTool,
        boolean fellLeaves,
        boolean replantSapling,
        int staggerThreshold,
        int logsPerTick,
        boolean protectPlayerBuilt,
        boolean protectStructures,
        int structureBlockThreshold,
        int maxTrackedPerChunk
) {
    public enum SneakRequirement { IGNORE, REQUIRED, FORBIDDEN }

    public static BlackTimberConfig from(FileConfiguration c) {
        return new BlackTimberConfig(
                c.getBoolean("require-natural-leaves", true),
                Math.max(0, c.getInt("min-natural-leaves", 1)),
                Math.max(0, c.getInt("leaf-search-radius", 1)),
                c.getBoolean("search-diagonal", true),
                Math.max(1, c.getInt("max-logs", 150)),
                c.getBoolean("require-axe", true),
                parseSneak(c.getString("sneak-requirement", "ignore")),
                c.getBoolean("survival-only", true),
                c.getBoolean("default-enabled", true),
                c.getBoolean("apply-durability", true),
                c.getBoolean("respect-unbreaking", true),
                c.getBoolean("break-tool", false),
                c.getBoolean("fell-leaves", false),
                c.getBoolean("replant-sapling", false),
                Math.max(1, c.getInt("stagger-threshold", 64)),
                Math.max(1, c.getInt("logs-per-tick", 16)),
                c.getBoolean("protect-player-built", true),
                c.getBoolean("protect-structures", true),
                Math.max(1, c.getInt("structure-block-threshold", 1)),
                Math.max(1, c.getInt("max-tracked-per-chunk", 4096))
        );
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
