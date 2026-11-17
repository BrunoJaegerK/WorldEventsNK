package com.brunojaeger.worldevents.world;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {
    private final WorldEventsNK plugin;
    private final ProtectionManager protection;

    public SafeLocationFinder(WorldEventsNK plugin, ProtectionManager protection) {
        this.plugin = plugin;
        this.protection = protection;
    }

    public EventLocation find(String eventId) {
        List<Level> worlds = new ArrayList<>();
        List<String> excluded = plugin.getConfig().getStringList("worlds.excluded");
        for (Level level : Server.getInstance().getLevels().values()) {
            if (level == null) continue;
            boolean skip = false;
            for (String name : excluded) if (level.getFolderName().equalsIgnoreCase(name)) skip = true;
            if (!skip) worlds.add(level);
        }
        if (worlds.isEmpty()) return null;

        int attempts = Math.max(1, plugin.getConfig().getInt("worlds.max-location-attempts", 40));
        for (int attempt = 0; attempt < attempts; attempt++) {
            Level level = worlds.get(ThreadLocalRandom.current().nextInt(worlds.size()));
            int min = Math.max(0, plugin.getConfig().getInt("worlds.minimum-distance-from-spawn", 300));
            int max = Math.max(min + 1, plugin.getConfig().getInt("worlds.maximum-distance-from-spawn", 3000));
            int radius = ThreadLocalRandom.current().nextInt(min, max);
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            int x = (int) Math.floor(level.getSpawnLocation().getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(level.getSpawnLocation().getZ() + Math.sin(angle) * radius);

            // Never generate/load a chunk just to search for an event location.
            if (!level.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = level.getHighestBlockAt(x, z) + 1;
            if (y <= level.getMinBlockY() || y + 1 > level.getMaxBlockY()) continue;
            Block floor = level.getBlock(x, y - 1, z, false);
            Block feet = level.getBlock(x, y, z, false);
            Block head = level.getBlock(x, y + 1, z, false);
            if (floor == null || feet == null || head == null || !floor.isSolid()) continue;
            if (isLiquid(floor.getId()) || feet.getId() != 0 || head.getId() != 0) continue;

            int minPlayer = Math.max(0, plugin.getConfig().getInt("worlds.minimum-distance-from-player", 100));
            boolean close = false;
            for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                if (player.getLevel() != level) continue;
                double dx = player.getX() - (x + 0.5);
                double dy = player.getY() - y;
                double dz = player.getZ() - (z + 0.5);
                if (dx * dx + dy * dy + dz * dz < (double) minPlayer * minPlayer) { close = true; break; }
            }
            if (close) continue;
            if (protection.isProtected(level, x, y - 1, z) || protection.isProtected(level, x, y, z)) continue;
            return new EventLocation(level, x, y, z);
        }
        return null;
    }

    private boolean isLiquid(int id) { return id == 8 || id == 9 || id == 10 || id == 11; }
}
