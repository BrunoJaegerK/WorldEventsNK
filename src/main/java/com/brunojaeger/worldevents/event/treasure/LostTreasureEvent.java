package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

public final class LostTreasureEvent implements WorldEvent {
    private static final String[] HINTS = {
            "El tesoro está en uno de los mundos habilitados.",
            "Busca en el mundo anunciado y revisa la zona de exploración.",
            "El cofre está sobre una superficie sólida, en las coordenadas aproximadas del anuncio."
    };

    private final WorldEventsNK plugin;
    private final SafeLocationFinder finder;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private long endsAt;
    private long nextHintAt;
    private int hintIndex;
    private boolean claimed;

    public LostTreasureEvent(WorldEventsNK plugin, SafeLocationFinder finder) {
        this.plugin = plugin;
        this.finder = finder;
    }

    @Override public String getId() { return "losttreasure"; }
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
        if (location == null || plugin.getProtectionManager().isProtected(
                location.getLevel(), location.getX(), location.getY(), location.getZ())) {
            state = EventState.FAILED;
            plugin.getLogger().warning("LostTreasure cancelado: no se encontró una ubicación segura.");
            return false;
        }
        int chestId = plugin.getConfig().getInt("losttreasure.chest-block-id", 54);
        location.getLevel().setBlock(location.getX(), location.getY(), location.getZ(), Block.get(chestId, 0), false, true);
        claimed = false;
        hintIndex = 0;
        long now = System.currentTimeMillis();
        endsAt = now + Math.max(60, plugin.getConfig().getInt("losttreasure.duration", 900)) * 1000L;
        nextHintAt = now + Math.max(30, plugin.getConfig().getInt("losttreasure.hint-interval", 180)) * 1000L;
        state = EventState.ACTIVE;
        location.getLevel().addSound(new Vector3(location.getX(), location.getY(), location.getZ()), Sound.RANDOM_CHESTOPEN);
        plugin.broadcast(plugin.message("treasure-announcement").replace("{world}", location.getLevel().getFolderName())
                .replace("{x}", String.valueOf(location.getX())).replace("{z}", String.valueOf(location.getZ())));
        return true;
    }

    public boolean handleInteract(Player player, Block block) {
        if (location == null || block == null) return false;
        if (block.getLevel() != location.getLevel() || block.getFloorX() != location.getX()
                || block.getFloorY() != location.getY() || block.getFloorZ() != location.getZ()) return false;
        return claim(player);
    }

    public boolean claim(Player player) {
        if (state != EventState.ACTIVE || claimed || location == null || player == null) return false;
        if (player.getLevel() != location.getLevel()) return false;
        double dx = player.getX() - (location.getX() + 0.5);
        double dy = player.getY() - location.getY();
        double dz = player.getZ() - (location.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > 25) return false;

        Block current = location.getLevel().getBlock(location.getX(), location.getY(), location.getZ(), false);
        if (current == null || current.getId() != plugin.getConfig().getInt("losttreasure.chest-block-id", 54)) return false;
        // Claim lock is set before any external reward call to prevent re-entrant duplicate claims.
        claimed = true;
        location.getLevel().setBlock(location.getX(), location.getY(), location.getZ(), Block.get(0, 0), false, true);
        plugin.getRewardManager().give(getId(), player);
        player.sendMessage(plugin.message("treasure-reward"));
        location.getLevel().addSound(new Vector3(location.getX(), location.getY(), location.getZ()), Sound.RANDOM_LEVELUP);
        state = EventState.COMPLETED;
        plugin.broadcast(plugin.message("event-expired").replace("{event}", getId()));
        return true;
    }

    @Override
    public void tick() {
        if (state != EventState.ACTIVE) return;
        long now = System.currentTimeMillis();
        if (now >= endsAt) { expire(); return; }
        if (now >= nextHintAt && hintIndex < HINTS.length) {
            plugin.broadcast(plugin.message("treasure-hint").replace("{hint}", HINTS[hintIndex++]));
            nextHintAt = now + Math.max(30, plugin.getConfig().getInt("losttreasure.hint-interval", 180)) * 1000L;
        }
    }

    private void expire() {
        if (location != null && !claimed) {
            Block current = location.getLevel().getBlock(location.getX(), location.getY(), location.getZ(), false);
            if (current != null && current.getId() == plugin.getConfig().getInt("losttreasure.chest-block-id", 54)) {
                location.getLevel().setBlock(location.getX(), location.getY(), location.getZ(), Block.get(0, 0), false, true);
            }
        }
        state = EventState.COMPLETED;
        plugin.broadcast(plugin.message("event-expired").replace("{event}", getId()));
    }

    @Override
    public void stop() {
        if (state == EventState.ACTIVE || state == EventState.PREPARING) {
            expire();
            state = EventState.STOPPED;
            plugin.broadcast(plugin.message("event-stopped"));
        }
    }

    public EventLocation getLocation() { return location; }
}
