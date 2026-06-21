package tr.cebi.blacktimber;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers which logs a player placed by hand, so the feller can refuse to
 * topple a tree the player has built on.
 *
 * Positions are kept per chunk in the chunk PersistentDataContainer, packed into
 * an int array of chunk relative keys. Chunk PDC is written to disk with the
 * chunk, so the memory survives restarts with no database, and the data is owned
 * by the chunk's region thread. Every method here must run on that thread, which
 * is the case inside block place and break events.
 */
public final class PlacedLogStore {

    private static final int[] EMPTY = new int[0];

    private final BlackTimber plugin;
    private final NamespacedKey key;

    public PlacedLogStore(BlackTimber plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "placed");
    }

    public void add(Block block) {
        Chunk chunk = block.getChunk();
        int packed = pack(block.getX(), block.getY(), block.getZ());
        int[] data = read(chunk);
        for (int value : data) {
            if (value == packed) {
                return;
            }
        }
        if (data.length >= plugin.settings().maxTrackedPerChunk()) {
            return;
        }
        int[] next = Arrays.copyOf(data, data.length + 1);
        next[data.length] = packed;
        write(chunk, next);
    }

    public void remove(Block block) {
        Chunk chunk = block.getChunk();
        int packed = pack(block.getX(), block.getY(), block.getZ());
        int[] data = read(chunk);
        int index = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == packed) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        int[] next = new int[data.length - 1];
        System.arraycopy(data, 0, next, 0, index);
        System.arraycopy(data, index + 1, next, index, data.length - index - 1);
        write(chunk, next);
    }

    /**
     * Whole cluster veto: true if any of these logs was placed by a player.
     * Each chunk's array is read once and reused across the logs it owns.
     */
    public boolean anyPlaced(List<Block> logs) {
        Map<Long, int[]> cache = new HashMap<>();
        for (Block log : logs) {
            long chunkKey = (((long) (log.getX() >> 4)) << 32) | (((long) (log.getZ() >> 4)) & 0xFFFFFFFFL);
            int[] data = cache.computeIfAbsent(chunkKey, k -> read(log.getChunk()));
            if (data.length == 0) {
                continue;
            }
            int packed = pack(log.getX(), log.getY(), log.getZ());
            for (int value : data) {
                if (value == packed) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[] read(Chunk chunk) {
        int[] data = chunk.getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return data == null ? EMPTY : data;
    }

    private void write(Chunk chunk, int[] data) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (data.length == 0) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.INTEGER_ARRAY, data);
        }
    }

    // Chunk relative key: 4 bits local x, 4 bits local z, 12 bits y (offset +2048).
    private static int pack(int x, int y, int z) {
        return ((x & 15) << 16) | ((z & 15) << 12) | ((y + 2048) & 0xFFF);
    }
}
