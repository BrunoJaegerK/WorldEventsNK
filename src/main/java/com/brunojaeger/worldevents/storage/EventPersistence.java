package com.brunojaeger.worldevents.storage;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.scheduler.TaskHandler;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;

/**
 * Crash-recovery journal for the temporary LostTreasure chest.
 * MeteorFall is permanent by design and is never rolled back here.
 */
public final class EventPersistence {
    private final WorldEventsNK plugin;
    private final Config state;
    private TaskHandler recoveryTask;

    public EventPersistence(WorldEventsNK plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "event-state.yml");
        this.state = new Config(file, Config.YAML);
        if (!file.exists()) {
            state.set("treasure.active", false);
            state.save();
        }
    }

    public boolean beginTreasure(Level level, int x, int y, int z) {
        state.set("treasure.world", level.getName());
        state.set("treasure.x", x);
        state.set("treasure.y", y);
        state.set("treasure.z", z);
        state.set("treasure.block-id", Block.CHEST);
        state.set("treasure.active", true);
        return state.save();
    }

    public void clearTreasure() {
        state.set("treasure.active", false);
        state.save();
    }

    /**
     * If the server crashed while a treasure was active, remove its chest when
     * the recorded world/chunk becomes available. No chunk is force-loaded.
     */
    public void recover() {
        if (!state.getBoolean("treasure.active", false) || recoveryTask != null) return;
        recoveryTask = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, () -> {
            String worldName = state.getString("treasure.world", "");
            Level level = null;
            for (Level candidate : Server.getInstance().getLevels().values()) {
                if (candidate.getName().equalsIgnoreCase(worldName)) {
                    level = candidate;
                    break;
                }
            }
            if (level == null) return;

            int x = state.getInt("treasure.x");
            int y = state.getInt("treasure.y");
            int z = state.getInt("treasure.z");
            if (!level.isChunkLoaded(x >> 4, z >> 4)) return;

            Block current = level.getBlock(x, y, z, false);
            if (current != null && current.getId() == state.getInt("treasure.block-id", Block.CHEST)) {
                level.setBlock(x, y, z, Block.get(Block.AIR), false, true);
                plugin.getLogger().warning("Se limpió un cofre LostTreasure huérfano tras un cierre inesperado.");
            }
            clearTreasure();
            cancelRecovery();
        }, 20);
    }

    public void shutdown() {
        cancelRecovery();
    }

    private void cancelRecovery() {
        if (recoveryTask != null) {
            recoveryTask.cancel();
            recoveryTask = null;
        }
    }
}
