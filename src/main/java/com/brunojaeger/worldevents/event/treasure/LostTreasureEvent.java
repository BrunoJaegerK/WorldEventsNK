package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.storage.EventPersistence;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

public final class LostTreasureEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final RewardManager rewards;
    private final EventPersistence persistence;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private int remainingSeconds;
    private int elapsedSeconds;
    private int hintsSent;
    private boolean claimed;
    private boolean chestPlaced;

    public LostTreasureEvent(WorldEventsNK plugin, SafeLocationFinder locations,
                             RewardManager rewards, EventPersistence persistence) {
        this.plugin = plugin;
        this.locations = locations;
        this.rewards = rewards;
        this.persistence = persistence;
    }

    @Override public String getId() { return "losttreasure"; }
    @Override public EventState getState() { return state; }

    @Override
    public boolean start() {
        state = EventState.PREPARING;
        location = locations.find(getId());
        if (location == null) {
            state = EventState.FAILED;
            return false;
        }
        int chestY = location.getY() + 1;
        Block target = location.getLevel().getBlock(location.getX(), chestY, location.getZ(), false);
        if (target == null || target.getId() != Block.AIR
                || !plugin.getProtectionManager().canModify(location.getLevel(), location.getX(), chestY,
                location.getZ(), getId())) {
            state = EventState.FAILED;
            return false;
        }
        if (!persistence.beginTreasure(location.getLevel(), location.getX(), chestY, location.getZ())) {
            state = EventState.FAILED;
            plugin.getLogger().warning("LostTreasure cancelado: no se pudo guardar el registro de recuperación.");
            return false;
        }
        chestPlaced = location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                Block.get(Block.CHEST), false, true);
        if (!chestPlaced) {
            persistence.clearTreasure();
            state = EventState.FAILED;
            return false;
        }
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        elapsedSeconds = 0;
        hintsSent = 0;
        claimed = false;
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage("§6[WorldEvents] §e¡Se ha escondido un tesoro en "
                + location.getLevel().getName() + "§e! Busca pistas con §f/we claim§e.");
        return true;
    }

    public boolean claim(Player player) {
        if (state != EventState.ACTIVE || claimed || !chestPlaced || location == null) return false;
        if (player.getLevel() != location.getLevel()) return false;
        int radius = Math.max(2, plugin.getConfig().getInt("losttreasure.claim-radius", 5));
        double dx = player.getX() - (location.getX() + 0.5);
        double dy = player.getY() - (location.getY() + 1);
        double dz = player.getZ() - (location.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > radius * radius) return false;

        int chestY = location.getY() + 1;
        Block current = location.getLevel().getBlock(location.getX(), chestY, location.getZ(), false);
        if (current == null || current.getId() != Block.CHEST) return false;

        claimed = true;
        boolean removed = location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                Block.get(Block.AIR), false, true);
        if (!removed) {
            claimed = false;
            return false;
        }
        chestPlaced = false;
        persistence.clearTreasure();
        // The chest and recovery journal are cleared before payout to prevent replay after a crash.
        rewards.give(getId(), player);
        player.sendMessage(plugin.getMessages().get("treasure-claimed"));
        state = EventState.COMPLETED;
        return true;
    }

    @Override
    public void tick() {
        if (state != EventState.ACTIVE) return;
        elapsedSeconds++;
        sendProgressiveHint();
        if (--remainingSeconds <= 0) {
            removeChestIfUnchanged();
            state = EventState.COMPLETED;
            plugin.getServer().broadcastMessage("§7El tesoro perdido ha desaparecido sin ser reclamado.");
        }
    }

    private void sendProgressiveHint() {
        int interval = Math.max(15, plugin.getConfig().getInt("losttreasure.hint-interval-seconds", 120));
        int maximum = Math.max(0, plugin.getConfig().getInt("losttreasure.maximum-hints", 3));
        if (hintsSent >= maximum || elapsedSeconds < (hintsSent + 1) * interval) return;
        int offset = switch (hintsSent) { case 0 -> 64; case 1 -> 24; default -> 0; };
        String hint = offset == 0 ? location.getX() + ", " + location.getZ()
                : "X " + (location.getX() - offset) + " a " + (location.getX() + offset)
                + ", Z " + (location.getZ() - offset) + " a " + (location.getZ() + offset);
        plugin.getServer().broadcastMessage(plugin.getMessages().get("treasure-hint").replace("{hint}", hint));
        hintsSent++;
    }

    private void removeChestIfUnchanged() {
        if (location == null || !chestPlaced) {
            persistence.clearTreasure();
            return;
        }
        int y = location.getY() + 1;
        Block current = location.getLevel().getBlock(location.getX(), y, location.getZ(), false);
        if (current != null && current.getId() == Block.CHEST) {
            boolean removed = location.getLevel().setBlock(location.getX(), y, location.getZ(),
                    Block.get(Block.AIR), false, true);
            if (!removed) {
                persistence.recover();
                return;
            }
        }
        chestPlaced = false;
        persistence.clearTreasure();
    }

    @Override public void stop() {
        removeChestIfUnchanged();
        state = EventState.COMPLETED;
    }

    @Override public boolean isFinished() {
        return state == EventState.COMPLETED || state == EventState.FAILED;
    }

    public boolean isClaimed() { return claimed; }
    public EventLocation getLocation() { return location; }
}
