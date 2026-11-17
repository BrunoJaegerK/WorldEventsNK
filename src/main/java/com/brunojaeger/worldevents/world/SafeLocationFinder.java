package com.brunojaeger.worldevents.world;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {
    private final WorldEventsNK plugin;
    private final ProtectionManager protection;

    public SafeLocationFinder(WorldEventsNK plugin, ProtectionManager protection) {
        this.plugin = plugin;
        this.protection = protection;
    }

    public EventLocation find(String eventId) {
        List<Level> worlds = new ArrayList<>(plugin.getServer().getLevels().values());
        String excludedRaw = plugin.getConfig().getString("worlds.excluded", "lobby,minigames");
        List<String> excluded = new ArrayList<>();
        for (String name : excludedRaw.split(",")) excluded.add(name.trim().toLowerCase(Locale.ROOT));
        worlds.removeIf(level -> excluded.contains(level.getName().toLowerCase(Locale.ROOT)));
        if (worlds.isEmpty()) return null;

        int minDistance = Math.max(0, plugin.getConfig().getInt("worlds.minimum-distance-from-spawn", 300));
        int maxDistance = Math.max(minDistance + 1,
                plugin.getConfig().getInt("worlds.maximum-distance-from-spawn", 3000));
        int attempts = Math.max(1, plugin.getConfig().getInt("worlds.location-attempts-per-world", 20));

        for (Level level : worlds) {
            Vector3 spawn = level.getProvider().getSpawn();
            if (spawn == null) continue;
            for (int i = 0; i < attempts; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                int distance = ThreadLocalRandom.current().nextInt(minDistance, maxDistance + 1);
                int x = (int) Math.floor(spawn.x + Math.cos(angle) * distance);
                int z = (int) Math.floor(spawn.z + Math.sin(angle) * distance);
                int y;
                try {
                    y = level.getHighestBlockAt(x, z);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (y <= level.getMinBlockY() || y + 2 > level.getMaxBlockY()) continue;
                Block surface = level.getBlock(x, y, z);
                if (!isNaturalSurface(surface.getId())) continue;
                if (level.getBlock(x, y + 1, z).getId() != Block.AIR
                        || level.getBlock(x, y + 2, z).getId() != Block.AIR) continue;
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
