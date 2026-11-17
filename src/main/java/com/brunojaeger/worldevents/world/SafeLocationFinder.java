package com.brunojaeger.worldevents.world;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Busca ubicaciones en mundos cargados sin solicitar generación de chunks.
 * La distancia se calcula siempre desde el spawn del mundo, no desde un jugador
 * aleatorio; así se respeta el rango configurado de forma consistente.
 */
public final class SafeLocationFinder {
    private final WorldEventsNK plugin;
    private final ProtectionManager protection;

    public SafeLocationFinder(WorldEventsNK plugin, ProtectionManager protection) {
        this.plugin = plugin;
        this.protection = protection;
    }

    public EventLocation find(String eventId) {
        List<Level> worlds = new ArrayList<>(plugin.getServer().getLevels().values());
        String excludedRaw = plugin.getConfig().getString("worlds.excluded", "");
        List<String> excluded = new ArrayList<>();
        for (String name : excludedRaw.split(",")) {
            if (!name.isBlank()) excluded.add(name.trim().toLowerCase(Locale.ROOT));
        }
        worlds.removeIf(level -> excluded.contains(level.getName().toLowerCase(Locale.ROOT)));
        Collections.shuffle(worlds);
        if (worlds.isEmpty()) return null;

        int minDistance = Math.max(0, Math.min(30_000_000,
                plugin.getConfig().getInt("worlds.minimum-distance-from-spawn", 300)));
        int maxDistance = Math.max(minDistance + 1, Math.min(30_000_000,
                plugin.getConfig().getInt("worlds.maximum-distance-from-spawn", 3000)));
        int attempts = Math.max(1, Math.min(500,
                plugin.getConfig().getInt("worlds.location-attempts-per-world", 20)));
        double minDistanceSquared = (double) minDistance * minDistance;
        double maxDistanceSquared = (double) maxDistance * maxDistance;

        for (Level level : worlds) {
            Vector3 spawn = level.getProvider().getSpawn();
            if (spawn == null) continue;

            for (int i = 0; i < attempts; i++) {
                double distance = ThreadLocalRandom.current().nextDouble(minDistance, (double) maxDistance);
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                int x = (int) Math.floor(spawn.x + Math.cos(angle) * distance);
                int z = (int) Math.floor(spawn.z + Math.sin(angle) * distance);

                double spawnDistanceSquared = (x - spawn.x) * (double) (x - spawn.x)
                        + (z - spawn.z) * (double) (z - spawn.z);
                if (spawnDistanceSquared < minDistanceSquared || spawnDistanceSquared > maxDistanceSquared) continue;

                // getChunkIfLoaded evita que una búsqueda de evento genere terreno.
                FullChunk chunk = level.getChunkIfLoaded(x >> 4, z >> 4);
                if (chunk == null) continue;

                int y = chunk.getHighestBlockAt(x & 15, z & 15, false);
                if (y <= level.getMinBlockY() || y + 2 > level.getMaxBlockY()) continue;
                Block surface = level.getBlock(x, y, z, false);
                if (surface == null || !isNaturalSurface(surface.getId())) continue;
                if (level.getBlock(x, y + 1, z, false).getId() != Block.AIR
                        || level.getBlock(x, y + 2, z, false).getId() != Block.AIR) continue;
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
