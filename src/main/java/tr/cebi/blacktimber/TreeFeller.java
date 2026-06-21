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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core felling logic.
 *
 * When a tree qualifies, the feller takes over the break entirely (the listener
 * cancels the event) and removes every connected log, plus the attached natural
 * leaves when the player has leaf breaking on. Drops either fall in the world or
 * go straight to the player inventory, and broken leaves can roll bonus loot.
 * Small trees are handled in place; large fells are spread across ticks with the
 * region scheduler so a single region never stalls.
 */
public final class TreeFeller {

    private static final int[][] FACES = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] AROUND = buildAround();

    private record FellContext(String biomeId, Material species, int logCount, boolean autoPickup) { }

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

        boolean autoPickup = plugin.userSettings().get(player, UserSettings.Option.PICKUP);
        boolean breakLeaves = plugin.userSettings().get(player, UserSettings.Option.LEAVES);

        ItemStack dropTool = tool.clone();
        applyDurability(player, tool, logs.size(), cfg);

        Block base = origin;
        ArrayDeque<Block> work = new ArrayDeque<>(logs.size() * 2);
        for (Block log : logs) {
            work.add(log);
            if (log.getY() < base.getY()) {
                base = log;
            }
        }
        if (breakLeaves) {
            work.addAll(collectLeaves(logs, cfg.leafSearchRadius()));
        }

        FellContext ctx = new FellContext(
                origin.getBiome().getKey().toString(),
                origin.getType(),
                logs.size(),
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

    private List<Block> collectLeaves(List<Block> logs, int radius) {
        List<Block> leaves = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Block log : logs) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block neighbour = log.getRelative(dx, dy, dz);
                        if (!Tag.LEAVES.isTagged(neighbour.getType())) {
                            continue;
                        }
                        if (!(neighbour.getBlockData() instanceof Leaves data) || data.isPersistent()) {
                            continue;
                        }
                        if (seen.add(key(neighbour.getX(), neighbour.getY(), neighbour.getZ()))) {
                            leaves.add(neighbour);
                        }
                    }
                }
            }
        }
        return leaves;
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
