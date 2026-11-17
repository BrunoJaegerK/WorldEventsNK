package com.brunojaeger.worldevents.event.meteor;

import cn.nukkit.block.Block;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

public final class MeteorFallEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private int remainingSeconds;

    public MeteorFallEvent(WorldEventsNK plugin, SafeLocationFinder locations) {
        this.plugin = plugin;
        this.locations = locations;
    }

    @Override public String getId() { return "meteorfall"; }
    @Override public EventState getState() { return state; }

    @Override
    public boolean start() {
        state = EventState.PREPARING;
        location = locations.find(getId());
        if (location == null) {
            state = EventState.FAILED;
            return false;
        }
        if (!createPermanentImpact()) {
            state = EventState.FAILED;
            return false;
        }
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage("§6[WorldEvents] §e¡Un meteorito ha impactado en "
                + location.getLevel().getName() + " §7(X: " + location.getX() + ", Z: " + location.getZ() + ")!");
        return true;
    }

    private boolean createPermanentImpact() {
        int radius = Math.max(2, Math.min(6, plugin.getConfig().getInt("meteorfall.radius", 4)));
        int changed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;
                int x = location.getX() + dx;
                int z = location.getZ() + dz;
                int y;
                try {
                    y = location.getLevel().getHighestBlockAt(x, z);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (y <= location.getLevel().getMinBlockY() + 2) continue;
                int id = location.getLevel().getBlock(x, y, z).getId();
                if (!isNatural(id)) continue;
                if (!plugin.getProtectionManager().canModify(location.getLevel(), x, y, z, getId())) continue;
                if (changed >= plugin.getConfig().getInt("performance.max-block-changes-per-event", 150)) break;
                if (distance <= radius - 1) {
                    location.getLevel().setBlock(x, y, z, Block.get(Block.AIR), false, true);
                } else {
                    location.getLevel().setBlock(x, y, z, Block.get(Block.COBBLESTONE), false, true);
                }
                changed++;
            }
        }
        int centerY = location.getLevel().getHighestBlockAt(location.getX(), location.getZ());
        if (plugin.getProtectionManager().canModify(location.getLevel(), location.getX(), centerY,
                location.getZ(), getId())) {
            location.getLevel().setBlock(location.getX(), Math.max(location.getLevel().getMinBlockY(), centerY - 1),
                    location.getZ(), Block.get(Block.OBSIDIAN), false, true);
        }
        return changed > 0;
    }

    private boolean isNatural(int id) {
        return id == Block.GRASS || id == Block.DIRT || id == Block.STONE
                || id == Block.SAND || id == Block.GRAVEL || id == Block.NETHERRACK
                || id == Block.END_STONE;
    }

    @Override public void tick() {
        if (state != EventState.ACTIVE) return;
        if (--remainingSeconds <= 0) state = EventState.COMPLETED;
    }

    @Override public void stop() { if (state == EventState.ACTIVE) state = EventState.COMPLETED; }
    @Override public boolean isFinished() { return state == EventState.COMPLETED || state == EventState.FAILED; }
}
