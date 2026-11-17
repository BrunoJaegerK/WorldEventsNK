package com.brunojaeger.worldevents.world;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {
    private final WorldEventsNK plugin;
    private final ProtectionManager protection;

    public SafeLocationFinder(WorldEventsNK plugin, ProtectionManager protection) {
        this.plugin = plugin;
        this.protection = protection;
    }

    /**
     * Busca exclusivamente en chunks que ya estan cargados. Nunca fuerza la generacion
     * de chunks como efecto secundario de un evento automatico.
     */
    public EventLocation find(String eventId) {
        List<Level> worlds = new ArrayList<>(plugin.getServer().getLevels().values());
        String excludedRaw = plugin.getConfig().getString("worlds.excluded", "lobby,minigames");
        List<String> excluded = new ArrayList<>();
        for (String name : excludedRaw.split(",")) {
            excluded.add(name.trim().toLowerCase(Locale.ROOT));
        }
        worlds.removeIf(level -> excluded.contains(level.getName().toLowerCase(Locale.ROOT)));

        int minDistance = Math.max(0, plugin.getConfig().getInt("worlds.minimum-distance-from-spawn", 300));
        int maxDistance = Math.max(minDistance + 1,
                plugin.getConfig().getInt("worlds.maximum-distance-from-spawn", 3000));
        int attempts = Math.max(1, plugin.getConfig().getInt("worlds.location-attempts-per-world", 40));
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Level level : worlds) {
            Vector3 spawn = level.getProvider().getSpawn();
            if (spawn == null) continue;
            List<FullChunk> loadedChunks = new ArrayList<>(level.getChunks().values());
            if (loadedChunks.isEmpty()) continue;

            for (int i = 0; i < attempts; i++) {
                FullChunk chunk = loadedChunks.get(random.nextInt(loadedChunks.size()));
                int x = (chunk.getX() << 4) + random.nextInt(16);
                int z = (chunk.getZ() << 4) + random.nextInt(16);
                double distanceSquared = Math.pow(x - spawn.x, 2) + Math.pow(z - spawn.z, 2);
                if (distanceSquared < (double) minDistance * minDistance
                        || distanceSquared > (double) maxDistance * maxDistance) continue;

                int y = chunk.getHighestBlockAt(x & 15, z & 15, false);
                if (y <= level.getMinBlockY() || y + 2 > level.getMaxBlockY()) continue;

                Block surface = level.getBlock(x, y, z, false);
                Block feet = level.getBlock(x, y + 1, z, false);
                Block head = level.getBlock(x, y + 2, z, false);
                if (surface == null || feet == null || head == null || !isNaturalSurface(surface.getId())
                        || feet.getId() != Block.AIR || head.getId() != Block.AIR) continue;

                if (!protection.canModify(level, x, y, z, eventId)) continue;
                return new EventLocation(level, x, y, z);
            }
        }
        return null;
    }

    private boolean isNaturalSurface(int id) {
        return id == Block.GRASS || id == Block.DIRT || id == Block.STONE
                || id == Block.SAND || id == Block.GRAVEL || id == Block.NETHERRACK
                || id == Block.END_STONE;
    }
}
