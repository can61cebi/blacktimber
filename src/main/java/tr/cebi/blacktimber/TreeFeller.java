package tr.cebi.blacktimber;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core felling logic.
 *
 * Steps: flood fill the connected logs (bounded), confirm the cluster is a real
 * tree by finding natural leaves, then break every log but the one the player is
 * already breaking. Small trees are felled in place; very large fells are spread
 * across ticks with the region scheduler so a single region never stalls.
 */
public final class TreeFeller {

    private static final int[][] FACES = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] AROUND = buildAround();

    private final BlackTimber plugin;

    public TreeFeller(BlackTimber plugin) {
        this.plugin = plugin;
    }

    public void fell(Player player, Block origin, ItemStack tool, BlackTimberConfig cfg) {
        List<Block> logs = collect(origin, cfg);
        if (logs.size() <= 1) {
            return;
        }
        if (cfg.requireNaturalLeaves()
                && !hasNaturalLeaves(logs, cfg.leafSearchRadius(), cfg.minNaturalLeaves())) {
            return;
        }

        ArrayDeque<Block> pending = new ArrayDeque<>(logs.size());
        Block base = origin;
        for (Block log : logs) {
            if (!isSame(log, origin)) {
                pending.add(log);
            }
            if (log.getY() < base.getY()) {
                base = log;
            }
        }
        if (pending.isEmpty()) {
            return;
        }

        ItemStack dropTool = tool.clone();
        applyDurability(player, tool, pending.size(), cfg);

        Block replantBase = base;

        if (pending.size() <= cfg.staggerThreshold()) {
            while (!pending.isEmpty()) {
                breakLog(pending.poll(), dropTool, cfg);
            }
            finish(replantBase, cfg);
            return;
        }

        // Large fell: stay on the region that owns the origin and break a budget
        // of logs each tick. A tree always sits within the region's safe radius.
        Location anchor = origin.getLocation();
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, anchor, task -> {
            int budget = cfg.logsPerTick();
            while (budget-- > 0 && !pending.isEmpty()) {
                breakLog(pending.poll(), dropTool, cfg);
            }
            if (pending.isEmpty()) {
                finish(replantBase, cfg);
                task.cancel();
            }
        }, 1L, 1L);
    }

    private List<Block> collect(Block origin, BlackTimberConfig cfg) {
        int[][] offsets = cfg.searchDiagonal() ? AROUND : FACES;
        int cap = cfg.maxLogs();

        List<Block> logs = new ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();

        queue.add(origin);
        seen.add(key(origin.getX(), origin.getY(), origin.getZ()));

        while (!queue.isEmpty() && logs.size() < cap) {
            Block current = queue.poll();
            logs.add(current);
            for (int[] offset : offsets) {
                Block neighbour = current.getRelative(offset[0], offset[1], offset[2]);
                long k = key(neighbour.getX(), neighbour.getY(), neighbour.getZ());
                if (!seen.add(k)) {
                    continue;
                }
                if (Tag.LOGS.isTagged(neighbour.getType())) {
                    queue.add(neighbour);
                }
            }
        }
        return logs;
    }

    /**
     * A cluster counts as a tree only if natural leaves are attached. Player
     * placed leaves are persistent and ignored, which is what protects builds.
     */
    private boolean hasNaturalLeaves(List<Block> logs, int radius, int min) {
        if (min <= 0) {
            return true;
        }
        int found = 0;
        for (Block log : logs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block neighbour = log.getRelative(dx, dy, dz);
                        if (!Tag.LEAVES.isTagged(neighbour.getType())) {
                            continue;
                        }
                        if (neighbour.getBlockData() instanceof Leaves leaf && !leaf.isPersistent()) {
                            if (++found >= min) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void breakLog(Block block, ItemStack dropTool, BlackTimberConfig cfg) {
        if (!Tag.LOGS.isTagged(block.getType())) {
            return;
        }
        block.breakNaturally(dropTool);
        if (cfg.fellLeaves()) {
            for (int[] offset : FACES) {
                Block neighbour = block.getRelative(offset[0], offset[1], offset[2]);
                if (Tag.LEAVES.isTagged(neighbour.getType())
                        && neighbour.getBlockData() instanceof Leaves leaf && !leaf.isPersistent()) {
                    neighbour.breakNaturally();
                }
            }
        }
    }

    private void applyDurability(Player player, ItemStack tool, int logs, BlackTimberConfig cfg) {
        if (!cfg.applyDurability() || logs <= 0 || tool == null) {
            return;
        }
        short max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int unbreaking = cfg.respectUnbreaking() ? tool.getEnchantmentLevel(Enchantment.UNBREAKING) : 0;
        int damage = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < logs; i++) {
            // Vanilla rule: an Unbreaking tool has a 1/(level+1) chance to wear.
            if (unbreaking > 0 && random.nextInt(unbreaking + 1) != 0) {
                continue;
            }
            damage++;
        }
        if (damage <= 0) {
            return;
        }

        int newDamage = damageable.getDamage() + damage;
        if (newDamage >= max && !cfg.breakTool()) {
            newDamage = max - 1;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);

        if (cfg.breakTool() && newDamage >= max) {
            tool.setAmount(0);
        }
        player.getInventory().setItemInMainHand(tool.getAmount() <= 0 ? null : tool);
    }

    private void finish(Block base, BlackTimberConfig cfg) {
        if (cfg.replantSapling()) {
            tryReplant(base);
        }
    }

    private void tryReplant(Block base) {
        Material sapling = saplingFor(base.getType());
        if (sapling == null) {
            return;
        }
        if (!isSoil(base.getRelative(0, -1, 0).getType())) {
            return;
        }
        if (base.getType().isAir()) {
            base.setType(sapling, true);
        }
    }

    // Single sapling species only. Dark oak and pale oak need a 2x2 of saplings
    // and mangrove grows from a propagule, so those are skipped on purpose.
    private static Material saplingFor(Material log) {
        String species = switch (log) {
            case OAK_LOG -> "OAK";
            case BIRCH_LOG -> "BIRCH";
            case SPRUCE_LOG -> "SPRUCE";
            case JUNGLE_LOG -> "JUNGLE";
            case ACACIA_LOG -> "ACACIA";
            case CHERRY_LOG -> "CHERRY";
            default -> null;
        };
        return species == null ? null : Material.matchMaterial(species + "_SAPLING");
    }

    private static boolean isSoil(Material material) {
        return Tag.DIRT.isTagged(material) || material == Material.FARMLAND;
    }

    private static boolean isSame(Block a, Block b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ()
                && a.getWorld().equals(b.getWorld());
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int[][] buildAround() {
        int[][] all = new int[26][];
        int index = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    all[index++] = new int[]{dx, dy, dz};
                }
            }
        }
        return all;
    }
}
