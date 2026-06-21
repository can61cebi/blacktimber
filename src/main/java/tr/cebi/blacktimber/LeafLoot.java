package tr.cebi.blacktimber;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bonus loot that broken leaves can drop when a player has leaf breaking on.
 * Chances are themed by biome and scaled by tree species and size. Vanilla
 * sapling, stick and apple drops are produced separately and stack on top.
 *
 * Two layers feed the roll: a built in table keyed by biome id (the auto logic),
 * and an admin list edited from the loot menu and stored in config.yml. Biomes
 * are keyed by their namespaced id string, because in 26.1.2 Biome is an
 * interface and cannot be used in a switch.
 */
public final class LeafLoot {

    private record Entry(Material material, int amount, double baseChance) { }

    public record CustomEntry(ItemStack item, double chance, String biome) { }

    private final BlackTimber plugin;
    private final Map<String, List<Entry>> byBiome = new HashMap<>();
    private final List<Entry> universal = new ArrayList<>();
    private final List<CustomEntry> custom = new ArrayList<>();

    public LeafLoot(BlackTimber plugin) {
        this.plugin = plugin;
        buildDefaults();
        reloadCustom();
    }

    /** Roll both layers for one broken leaf and append any wins to out. */
    public void roll(String biomeId, Material species, int logCount, List<ItemStack> out) {
        BlackTimberConfig cfg = plugin.settings();
        if (!cfg.leafLootEnabled()) {
            return;
        }
        double multiplier = cfg.leafLootMultiplier() * sizeMultiplier(logCount) * speciesMultiplier(species);
        double max = cfg.leafLootMaxChance();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        rollList(byBiome.get(biomeId), multiplier, max, random, out);
        rollList(universal, multiplier, max, random, out);
        for (CustomEntry entry : custom) {
            if (!entry.biome().equals("*") && !entry.biome().equals(biomeId)) {
                continue;
            }
            double chance = Math.min(max, entry.chance() * multiplier);
            if (random.nextDouble() < chance) {
                out.add(entry.item().clone());
            }
        }
    }

    private void rollList(List<Entry> entries, double multiplier, double max,
                          ThreadLocalRandom random, List<ItemStack> out) {
        if (entries == null) {
            return;
        }
        for (Entry entry : entries) {
            double chance = Math.min(max, entry.baseChance() * multiplier);
            if (random.nextDouble() < chance) {
                out.add(new ItemStack(entry.material(), entry.amount()));
            }
        }
    }

    // ---- admin custom loot -------------------------------------------------

    public List<CustomEntry> customEntries() {
        return List.copyOf(custom);
    }

    public void addCustom(ItemStack item, double chance) {
        custom.add(new CustomEntry(item.clone(), chance, "*"));
        persistCustom();
    }

    public void removeCustom(int index) {
        if (index >= 0 && index < custom.size()) {
            custom.remove(index);
            persistCustom();
        }
    }

    public void adjustChance(int index, double delta, double max) {
        if (index < 0 || index >= custom.size()) {
            return;
        }
        CustomEntry entry = custom.get(index);
        double next = Math.max(0.0, Math.min(max, entry.chance() + delta));
        next = Math.round(next * 1000.0) / 1000.0;
        custom.set(index, new CustomEntry(entry.item(), next, entry.biome()));
        persistCustom();
    }

    public void reloadCustom() {
        custom.clear();
        for (Map<?, ?> raw : plugin.getConfig().getMapList("leaf-loot-custom")) {
            if (!(raw.get("item") instanceof String encoded)) {
                continue;
            }
            ItemStack item;
            try {
                item = ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
            } catch (RuntimeException e) {
                continue;
            }
            double chance = raw.get("chance") instanceof Number number ? number.doubleValue() : 0.0;
            String biome = raw.get("biome") instanceof String value ? value : "*";
            custom.add(new CustomEntry(item, chance, biome));
        }
    }

    private void persistCustom() {
        List<Map<String, Object>> list = new ArrayList<>(custom.size());
        for (CustomEntry entry : custom) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("item", Base64.getEncoder().encodeToString(entry.item().serializeAsBytes()));
            map.put("chance", entry.chance());
            map.put("biome", entry.biome());
            list.add(map);
        }
        plugin.editConfig("leaf-loot-custom", list);
    }

    // ---- multipliers -------------------------------------------------------

    private static double sizeMultiplier(int logCount) {
        if (logCount <= 6) {
            return 0.75;
        }
        if (logCount <= 20) {
            return 1.0;
        }
        if (logCount <= 50) {
            return 1.25;
        }
        return 1.5;
    }

    private static double speciesMultiplier(Material log) {
        String name = log.name();
        if (name.startsWith("PALE_OAK")) {
            return 1.5;
        }
        if (name.startsWith("MANGROVE") || name.startsWith("CHERRY")) {
            return 1.3;
        }
        if (name.startsWith("DARK_OAK")) {
            return 1.25;
        }
        if (name.startsWith("JUNGLE")) {
            return 1.15;
        }
        if (name.startsWith("ACACIA")) {
            return 1.1;
        }
        return 1.0;
    }

    // ---- built in defaults -------------------------------------------------

    private void add(String biomeId, String material, double chance) {
        Material resolved = Material.matchMaterial(material);
        if (resolved == null) {
            return;
        }
        byBiome.computeIfAbsent("minecraft:" + biomeId, key -> new ArrayList<>())
                .add(new Entry(resolved, 1, chance));
    }

    private void addUniversal(String material, double chance) {
        Material resolved = Material.matchMaterial(material);
        if (resolved != null) {
            universal.add(new Entry(resolved, 1, chance));
        }
    }

    // Themed defaults verified against 26.1.2. Unknown materials are dropped.
    private void buildDefaults() {
        add("cherry_grove", "PINK_PETALS", 0.030);
        add("cherry_grove", "CHERRY_SAPLING", 0.010);

        for (String jungle : List.of("jungle", "sparse_jungle")) {
            add(jungle, "COCOA_BEANS", 0.025);
            add(jungle, "MELON_SLICE", 0.010);
            add(jungle, "GLOW_BERRIES", 0.008);
        }
        add("bamboo_jungle", "BAMBOO", 0.030);
        add("bamboo_jungle", "COCOA_BEANS", 0.020);

        add("dark_forest", "RED_MUSHROOM", 0.015);
        add("dark_forest", "BROWN_MUSHROOM", 0.015);
        add("dark_forest", "LEAF_LITTER", 0.020);

        add("pale_garden", "RESIN_CLUMP", 0.025);
        add("pale_garden", "PALE_MOSS_CARPET", 0.020);
        add("pale_garden", "PALE_HANGING_MOSS", 0.015);
        add("pale_garden", "OPEN_EYEBLOSSOM", 0.006);

        for (String taiga : List.of("taiga", "snowy_taiga", "old_growth_pine_taiga",
                "old_growth_spruce_taiga", "grove")) {
            add(taiga, "SWEET_BERRIES", 0.030);
            add(taiga, "FERN", 0.025);
            add(taiga, "LARGE_FERN", 0.008);
        }
        add("snowy_taiga", "SNOWBALL", 0.020);

        for (String savanna : List.of("savanna", "savanna_plateau", "windswept_savanna")) {
            add(savanna, "TALL_GRASS", 0.030);
            add(savanna, "SHORT_DRY_GRASS", 0.025);
            add(savanna, "TORCHFLOWER_SEEDS", 0.003);
        }

        add("swamp", "LILY_PAD", 0.025);
        add("swamp", "SLIME_BALL", 0.010);
        add("swamp", "FIREFLY_BUSH", 0.010);
        add("mangrove_swamp", "MUD", 0.025);
        add("mangrove_swamp", "SLIME_BALL", 0.010);

        for (String badlands : List.of("badlands", "wooded_badlands", "eroded_badlands")) {
            add(badlands, "DEAD_BUSH", 0.030);
            add(badlands, "CACTUS_FLOWER", 0.015);
            add(badlands, "GOLD_NUGGET", 0.005);
        }

        for (String plains : List.of("plains", "sunflower_plains", "meadow")) {
            add(plains, "SHORT_GRASS", 0.030);
            add(plains, "DANDELION", 0.020);
            add(plains, "HONEYCOMB", 0.004);
        }
        add("meadow", "WILDFLOWERS", 0.015);

        for (String forest : List.of("forest", "flower_forest", "birch_forest", "old_growth_birch_forest")) {
            add(forest, "BUSH", 0.025);
            add(forest, "LEAF_LITTER", 0.020);
        }
        add("birch_forest", "WILDFLOWERS", 0.015);
        add("old_growth_birch_forest", "WILDFLOWERS", 0.015);

        // A universal ultra rare so every tree has a small surprise.
        addUniversal("GOLDEN_DANDELION", 0.001);
    }
}
