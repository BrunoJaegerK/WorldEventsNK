package com.brunojaeger.worldevents.event.meteor;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MeteorFallEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder finder;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private long endsAt;
    private boolean coreClaimed;

    public MeteorFallEvent(WorldEventsNK plugin, SafeLocationFinder finder) {
        this.plugin = plugin;
        this.finder = finder;
    }

    @Override public String getId() { return "meteorfall"; }
    @Override public EventState getState() { return state; }

    @Override
    public boolean canStart() {
        return state == EventState.IDLE || state == EventState.COMPLETED
                || state == EventState.STOPPED || state == EventState.FAILED;
    }

    @Override
    public boolean start() {
        if (!canStart()) return false;
        state = EventState.PREPARING;
        location = finder.find(getId());
        if (location == null || !generatePermanentImpact()) {
            state = EventState.FAILED;
            plugin.getLogger().warning("MeteorFall cancelado: no se encontró una ubicación segura con terreno natural.");
            return false;
        }
        coreClaimed = false;
        endsAt = System.currentTimeMillis() + Math.max(60, plugin.getConfig().getInt("meteorfall.duration", 600)) * 1000L;
        state = EventState.ACTIVE;
        Level level = location.getLevel();
        Vector3 impact = new Vector3(location.getX() + 0.5, location.getY(), location.getZ() + 0.5);
        level.addSound(impact, Sound.RANDOM_EXPLODE, 1.0f, 0.7f);
        plugin.broadcast(plugin.message("meteor-announcement")
                .replace("{world}", level.getFolderName())
                .replace("{x}", String.valueOf(location.getX()))
                .replace("{y}", String.valueOf(location.getY()))
                .replace("{z}", String.valueOf(location.getZ())));
        return true;
    }

    private boolean generatePermanentImpact() {
        Level level = location.getLevel();
        int radius = Math.max(2, Math.min(6, plugin.getConfig().getInt("meteorfall.radius", 4)));
        int depth = Math.max(1, Math.min(4, plugin.getConfig().getInt("meteorfall.depth", 2)));
        Set<Integer> natural = new HashSet<>();
        for (Object id : plugin.getConfig().getList("meteorfall.natural-block-whitelist")) {
            if (id instanceof Number) natural.add(((Number) id).intValue());
            else try { natural.add(Integer.parseInt(String.valueOf(id))); } catch (NumberFormatException ignored) { }
        }
        if (natural.isEmpty()) return false;

        // Validate the complete impact footprint before changing any block.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -depth; dy <= 1; dy++) {
                    int x = location.getX() + dx, y = location.getY() + dy, z = location.getZ() + dz;
                    if (!level.isChunkLoaded(x >> 4, z >> 4)) return false;
                    Block old = level.getBlock(x, y, z, false);
                    if (old == null) return false;
                    int id = old.getId();
                    // The two air layers are expected; all existing terrain must be whitelisted.
                    if (id != 0 && !natural.contains(id)) return false;
                    if (plugin.getProtectionManager().isProtected(level, x, y, z)) return false;
                }
            }
        }

        int coreId = plugin.getConfig().getInt("meteorfall.core-block-id", 49);
        int changes = 0;
        int maxChanges = Math.max(1, plugin.getConfig().getInt("performance.max-block-changes-per-event", 150));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;
                for (int dy = -depth; dy <= 0; dy++) {
                    if (++changes > maxChanges) return false;
                    int x = location.getX() + dx, y = location.getY() + dy, z = location.getZ() + dz;
                    int id;
                    if (dy == 0 && distance <= 1.2) id = coreId;
                    else if (dy == 0 && distance >= radius - 1) id = 14; // gold ore ring
                    else if (dy == -1 && distance <= radius - 1) id = ThreadLocalRandom.current().nextBoolean() ? 15 : 56;
                    else id = 1;
                    level.setBlock(x, y, z, Block.get(id, 0), false, true);
                }
            }
        }
        return true;
    }

    public boolean handleCoreInteract(Player player, Block block) {
        if (state != EventState.ACTIVE || coreClaimed || location == null || block == null) return false;
        if (block.getLevel() != location.getLevel() || block.getFloorX() != location.getX()
                || block.getFloorY() != location.getY() || block.getFloorZ() != location.getZ()) return false;
        coreClaimed = true;
        plugin.getRewardManager().give(getId(), player);
        player.sendMessage(plugin.message("meteor-reward"));
        return true;
    }

    @Override
    public void tick() {
        if (state == EventState.ACTIVE && System.currentTimeMillis() >= endsAt) {
            state = EventState.COMPLETED;
            plugin.broadcast(plugin.message("event-expired").replace("{event}", getId()));
        }
    }

    @Override
    public void stop() {
        if (state == EventState.ACTIVE || state == EventState.PREPARING) {
            state = EventState.STOPPED;
            plugin.broadcast(plugin.message("event-stopped"));
        }
    }

    public EventLocation getLocation() { return location; }
}
