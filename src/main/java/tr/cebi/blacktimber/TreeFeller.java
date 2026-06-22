package tr.cebi.blacktimber;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundGroup;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core felling logic.
 *
 * When a tree qualifies, the feller takes over the break entirely (the listener
 * cancels the event) and removes the connected logs the axe can still pay for,
 * plus the natural leaves that belong to those logs when the player has leaf
 * breaking on. Leaf removal mirrors vanilla decay: a leaf is cleared only when no
 * surviving log keeps it within range, so neighbouring trees in a forest keep
 * their canopy and the felled tree loses its whole canopy, outer layers included.
 * Drops either fall in the world or go straight to the player inventory, and
 * broken leaves can roll bonus loot. Small trees are handled in place; large fells
 * are spread across ticks with the region scheduler so a single region never
 * stalls.
 */
public final class TreeFeller {

    private static final int[][] FACES = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] AROUND = buildAround();

    // Vanilla leaf rule (Java Edition): a leaf survives while it sits within 6
    // orthogonal steps of a log and decays at distance 7. We flood twice this far
    // so we can still spot the standing logs that keep a felled tree's edge leaves
    // alive in a dense forest.
    private static final int LEAF_RANGE = 6;
    private static final int LEAF_SCAN_RANGE = LEAF_RANGE * 2;

    private record FellContext(String biomeId, Material species, int logCount, boolean autoPickup) { }

    // The slice of a tree the axe can afford this swing, the damage that costs, and
    // whether that damage is allowed to shatter the axe.
    private record DurabilityPlan(int brokenLogs, int damage, boolean breakTool) { }

    private final BlackTimber plugin;
    private final Set<Material> structureBlocks;

    public TreeFeller(BlackTimber plugin) {
        this.plugin = plugin;
        this.structureBlocks = buildStructureBlocks();
    }

    /**
     * Tries to fell the tree the broken log belongs to. Returns true when the
     * feller has taken over (the caller should cancel the break), false when the
     * block should break normally.
     */
    public boolean fell(Player player, Block origin, ItemStack tool, BlackTimberConfig cfg) {
        List<Block> logs = collect(origin, cfg);
        if (logs.size() <= 1) {
            return false;
        }
        if (isCustomized(logs, cfg)) {
            return false;
        }
        if (cfg.requireNaturalLeaves()
                && !hasNaturalLeaves(logs, cfg.leafSearchRadius(), cfg.minNaturalLeaves())) {
            return false;
        }

        // Work out how much of the tree the axe can pay for before it would break
        // (or, with break-tool off, before it would spend its final point of
        // durability). A worn axe fells only the part it can afford and leaves the
        // rest standing instead of toppling whole trees for free.
        DurabilityPlan plan = planDurability(tool, logs.size(), cfg);
        if (plan.brokenLogs() <= 1) {
            // Too worn to take over a multi-log fell. Let vanilla break the single
            // struck log so the player can still chop (and the axe still wears) as
            // normal.
            return false;
        }
        List<Block> felled = plan.brokenLogs() >= logs.size()
                ? logs
                : new ArrayList<>(logs.subList(0, plan.brokenLogs()));

        boolean autoPickup = plugin.userSettings().get(player, UserSettings.Option.PICKUP);
        boolean breakLeaves = plugin.userSettings().get(player, UserSettings.Option.LEAVES);

        ItemStack dropTool = tool.clone();
        applyDamage(player, tool, plan);

        Block base = felled.get(0);
        ArrayDeque<Block> work = new ArrayDeque<>(felled.size() * 2);
        for (Block log : felled) {
            work.add(log);
            if (log.getY() < base.getY()) {
                base = log;
            }
        }
        if (breakLeaves) {
            work.addAll(collectDecayingLeaves(felled, cfg));
        }

        FellContext ctx = new FellContext(
                origin.getBiome().getKey().toString(),
                origin.getType(),
                felled.size(),
                autoPickup);
        List<ItemStack> sink = autoPickup ? new ArrayList<>() : null;
        Block replantBase = base;

        if (work.size() <= cfg.staggerThreshold()) {
            while (!work.isEmpty()) {
                breakAndCollect(work.poll(), dropTool, ctx, sink);
            }
            deliver(player, sink, false);
            finish(replantBase, cfg);
            return true;
        }

        Location anchor = origin.getLocation();
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, anchor, task -> {
            int budget = cfg.logsPerTick();
            while (budget-- > 0 && !work.isEmpty()) {
                breakAndCollect(work.poll(), dropTool, ctx, sink);
            }
            if (work.isEmpty()) {
                deliver(player, sink, true);
                finish(replantBase, cfg);
                task.cancel();
            }
        }, 1L, 1L);
        return true;
    }

    private List<Block> collect(Block origin, BlackTimberConfig cfg) {
        int[][] offsets = cfg.searchDiagonal() ? AROUND : FACES;
        int cap = cfg.maxLogs();

        List<Block> logs = new ArrayList<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();

        queue.add(origin);
        seen.add(key(origin));

        while (!queue.isEmpty() && logs.size() < cap) {
            Block current = queue.poll();
            logs.add(current);
            for (int[] offset : offsets) {
                Block neighbour = current.getRelative(offset[0], offset[1], offset[2]);
                if (!seen.add(key(neighbour))) {
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
     * Finds the leaves that belong to the felled logs and would decay now that
     * those logs are gone. This reproduces vanilla leaf decay instantly:
     * <ol>
     *   <li>Flood out from the felled logs through connected leaves, recording how
     *       far each leaf is from the felled tree and noting any leaf that also
     *       touches a still-standing log.</li>
     *   <li>From those still-supported leaves, walk back through the canopy up to
     *       the vanilla range to mark every leaf a surviving tree keeps alive.</li>
     *   <li>Break the leaves that belonged to the felled tree (within range) and
     *       that no surviving log supports. Player placed (persistent) leaves are
     *       always left alone.</li>
     * </ol>
     * This clears the whole canopy of the felled tree, outer layers included, while
     * never touching the leaves of an adjacent tree whose trunk is still standing.
     */
    private List<Block> collectDecayingLeaves(List<Block> felled, BlackTimberConfig cfg) {
        int budget = cfg.leafClearBudget();

        Set<Long> felledKeys = new HashSet<>(felled.size() * 2);
        for (Block log : felled) {
            felledKeys.add(key(log));
        }

        Map<Long, Block> leaves = new HashMap<>();
        Map<Long, Integer> distFromFelled = new HashMap<>();
        Set<Long> supportSeeds = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depths = new ArrayDeque<>();

        // Seed: leaves orthogonally touching a felled log sit at distance 1.
        for (Block log : felled) {
            for (int[] face : FACES) {
                Block nb = log.getRelative(face[0], face[1], face[2]);
                if (!Tag.LEAVES.isTagged(nb.getType())) {
                    continue;
                }
                long k = key(nb);
                if (distFromFelled.putIfAbsent(k, 1) != null) {
                    continue;
                }
                leaves.put(k, nb);
                queue.add(nb);
                depths.add(1);
            }
        }

        boolean truncated = false;
        while (!queue.isEmpty()) {
            if (leaves.size() > budget) {
                truncated = true;
                break;
            }
            Block current = queue.poll();
            int depth = depths.poll();
            for (int[] face : FACES) {
                Block nb = current.getRelative(face[0], face[1], face[2]);
                Material type = nb.getType();
                if (Tag.LOGS.isTagged(type)) {
                    if (!felledKeys.contains(key(nb))) {
                        // A log we are NOT felling still feeds this leaf.
                        supportSeeds.add(key(current));
                    }
                    continue;
                }
                if (!Tag.LEAVES.isTagged(type) || depth >= LEAF_SCAN_RANGE) {
                    continue;
                }
                long k = key(nb);
                if (distFromFelled.putIfAbsent(k, depth + 1) != null) {
                    continue;
                }
                leaves.put(k, nb);
                queue.add(nb);
                depths.add(depth + 1);
            }
        }

        if (truncated) {
            // The canopy is bigger than the budget, so we cannot prove which leaves a
            // neighbour still supports. Rather than risk eating another tree's leaves,
            // hand cleanup back to vanilla decay for this fell.
            return List.of();
        }

        // Walk back from every still-supported leaf to mark all leaves a surviving
        // log keeps within range. These must not be broken.
        Set<Long> supported = new HashSet<>(supportSeeds);
        ArrayDeque<Block> q2 = new ArrayDeque<>();
        ArrayDeque<Integer> d2 = new ArrayDeque<>();
        for (long seed : supportSeeds) {
            q2.add(leaves.get(seed));
            d2.add(1);
        }
        while (!q2.isEmpty()) {
            Block current = q2.poll();
            int depth = d2.poll();
            if (depth >= LEAF_RANGE) {
                continue;
            }
            for (int[] face : FACES) {
                Block nb = current.getRelative(face[0], face[1], face[2]);
                long k = key(nb);
                if (!leaves.containsKey(k) || !supported.add(k)) {
                    continue;
                }
                q2.add(nb);
                d2.add(depth + 1);
            }
        }

        // Leaves that belonged to the felled tree and that no surviving log supports
        // are exactly the ones vanilla would let decay. Clear them, skipping any the
        // player placed.
        List<Block> decaying = new ArrayList<>();
        for (Map.Entry<Long, Block> entry : leaves.entrySet()) {
            Integer dist = distFromFelled.get(entry.getKey());
            if (dist == null || dist > LEAF_RANGE || supported.contains(entry.getKey())) {
                continue;
            }
            Block leaf = entry.getValue();
            if (leaf.getBlockData() instanceof Leaves data && !data.isPersistent()) {
                decaying.add(leaf);
            }
        }
        return decaying;
    }

    private boolean isCustomized(List<Block> logs, BlackTimberConfig cfg) {
        if (cfg.protectPlayerBuilt() && plugin.placedLogs().anyPlaced(logs)) {
            return true;
        }
        return cfg.protectStructures() && hasStructureAttached(logs, cfg.structureBlockThreshold());
    }

    // True once enough crafted blocks (planks, stairs, fences, doors, glass and
    // the like) touch the logs. These never generate on a wild tree.
    private boolean hasStructureAttached(List<Block> logs, int threshold) {
        int found = 0;
        for (Block log : logs) {
            for (int[] offset : FACES) {
                Block neighbour = log.getRelative(offset[0], offset[1], offset[2]);
                if (structureBlocks.contains(neighbour.getType())) {
                    if (++found >= threshold) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

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

    private void breakAndCollect(Block block, ItemStack dropTool, FellContext ctx, List<ItemStack> sink) {
        Material type = block.getType();
        boolean leaf = Tag.LEAVES.isTagged(type);
        if (!leaf && !Tag.LOGS.isTagged(type)) {
            return;
        }
        if (leaf && (!(block.getBlockData() instanceof Leaves data) || data.isPersistent())) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(block.getDrops(dropTool));
        if (leaf) {
            plugin.leafLoot().roll(ctx.biomeId(), ctx.species(), ctx.logCount(), drops);
        }

        BlockData blockData = block.getBlockData();
        World world = block.getWorld();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.setType(Material.AIR, false);
        world.spawnParticle(Particle.BLOCK, center, 8, 0.25, 0.25, 0.25, 0.0, blockData);
        SoundGroup sound = blockData.getSoundGroup();
        world.playSound(center, sound.getBreakSound(), sound.getVolume(), sound.getPitch());

        if (ctx.autoPickup()) {
            sink.addAll(drops);
        } else {
            for (ItemStack drop : drops) {
                world.dropItemNaturally(center, drop);
            }
        }
    }

    private void deliver(Player player, List<ItemStack> picked, boolean deferred) {
        if (picked == null || picked.isEmpty()) {
            return;
        }
        ItemStack[] items = picked.toArray(new ItemStack[0]);
        if (deferred) {
            player.getScheduler().run(plugin, task -> giveDirect(player, items), null);
        } else {
            giveDirect(player, items);
        }
    }

    private void giveDirect(Player player, ItemStack[] items) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(items);
        if (overflow.isEmpty()) {
            return;
        }
        Location loc = player.getLocation();
        World world = player.getWorld();
        for (ItemStack left : overflow.values()) {
            world.dropItemNaturally(loc, left);
        }
    }

    /**
     * Plans how many of the collected logs the axe can fell this swing without
     * over-spending its durability, and how much damage that costs. With break-tool
     * on, the plan may spend the axe's last point and shatter it on its final log;
     * with break-tool off, the plan stops one point early so the axe survives at 1
     * durability. Unbreaking is honoured per log, exactly like vanilla.
     */
    private DurabilityPlan planDurability(ItemStack tool, int logs, BlackTimberConfig cfg) {
        if (!cfg.applyDurability() || tool == null || logs <= 0) {
            return new DurabilityPlan(logs, 0, false);
        }
        short max = tool.getType().getMaxDurability();
        if (max <= 0 || !(tool.getItemMeta() instanceof Damageable damageable)) {
            return new DurabilityPlan(logs, 0, false);
        }

        int current = damageable.getDamage();
        boolean breakTool = cfg.breakTool();
        // Damage points we may spend this fell.
        int allowance = breakTool ? (max - current) : (max - 1 - current);
        if (allowance <= 0) {
            return new DurabilityPlan(0, 0, breakTool);
        }

        int unbreaking = cfg.respectUnbreaking() ? tool.getEnchantmentLevel(Enchantment.UNBREAKING) : 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spent = 0;
        int broken = 0;
        for (int i = 0; i < logs; i++) {
            // Vanilla rule: an Unbreaking tool has a 1/(level+1) chance to wear per use.
            boolean wears = unbreaking <= 0 || random.nextInt(unbreaking + 1) == 0;
            if (wears && spent >= allowance) {
                break; // cannot pay for this log; the rest of the tree stays standing
            }
            if (wears) {
                spent++;
            }
            broken++;
            if (breakTool && current + spent >= max) {
                break; // the axe shatters on this log; nothing more can be felled
            }
        }
        return new DurabilityPlan(broken, spent, breakTool);
    }

    private void applyDamage(Player player, ItemStack tool, DurabilityPlan plan) {
        if (plan.damage() <= 0 || tool == null) {
            return;
        }
        short max = tool.getType().getMaxDurability();
        if (max <= 0 || !(tool.getItemMeta() instanceof Damageable damageable)) {
            return;
        }
        int newDamage = damageable.getDamage() + plan.damage();
        if (plan.breakTool() && newDamage >= max) {
            tool.setAmount(0);
            player.getInventory().setItemInMainHand(null);
            return;
        }
        if (newDamage >= max) {
            newDamage = max - 1; // safety net; break-tool off never spends its last point
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
        player.getInventory().setItemInMainHand(tool);
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

    private static long key(Block block) {
        return key(block.getX(), block.getY(), block.getZ());
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

    private Set<Material> buildStructureBlocks() {
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        set.addAll(Tag.PLANKS.getValues());
        set.addAll(Tag.WOODEN_STAIRS.getValues());
        set.addAll(Tag.WOODEN_SLABS.getValues());
        set.addAll(Tag.WOODEN_FENCES.getValues());
        set.addAll(Tag.FENCE_GATES.getValues());
        set.addAll(Tag.WOODEN_DOORS.getValues());
        set.addAll(Tag.WOODEN_TRAPDOORS.getValues());
        set.addAll(Tag.WALLS.getValues());
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_GLASS") || name.endsWith("_GLASS_PANE")
                    || material == Material.GLASS || material == Material.GLASS_PANE
                    || material == Material.LADDER || material == Material.SCAFFOLDING) {
                set.add(material);
            }
        }
        return set;
    }
}
