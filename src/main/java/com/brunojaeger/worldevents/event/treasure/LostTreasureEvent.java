package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntityChest;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.statistics.StatisticsManager;
import com.brunojaeger.worldevents.storage.EventStorage;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

public final class LostTreasureEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final RewardManager rewards;
    private final EventStorage storage;
    private final StatisticsManager statistics;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private String storageMarker;
    private int remainingSeconds;
    private int elapsedSeconds;
    private int hintsSent;
    private boolean claimed;
    private boolean chestPlaced;

    public LostTreasureEvent(WorldEventsNK plugin, SafeLocationFinder locations, RewardManager rewards,
                             EventStorage storage, StatisticsManager statistics) {
        this.plugin = plugin;
        this.locations = locations;
        this.rewards = rewards;
        this.storage = storage;
        this.statistics = statistics;
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
        if (location.getLevel().getBlock(location.getX(), chestY, location.getZ(), false).getId() != Block.AIR
                || !plugin.getProtectionManager().canModify(location.getLevel(), location.getX(), chestY,
                location.getZ(), getId())) {
            state = EventState.FAILED;
            return false;
        }

        storageMarker = storage.beginTreasure(location.getLevel(), location.getX(), chestY, location.getZ());
        if (storageMarker == null) {
            state = EventState.FAILED;
            return false;
        }
        chestPlaced = location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                Block.get(Block.CHEST), false, true);
        if (!chestPlaced) {
            storage.clearTreasure();
            state = EventState.FAILED;
            return false;
        }

        BlockEntityChest chestEntity = storage.createOrGetChestEntity(
                location.getLevel(), location.getX(), chestY, location.getZ());
        if (chestEntity == null || !storage.markChestEntity(chestEntity, storageMarker)) {
            if (chestEntity != null) chestEntity.close();
            location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                    Block.get(Block.AIR), false, true);
            storage.clearTreasure();
            chestPlaced = false;
            state = EventState.FAILED;
            plugin.getLogger().warning("No se pudo marcar el cofre temporal de LostTreasure.");
            return false;
        }
        if (!storage.markTreasureActive()) {
            chestEntity.close();
            location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                    Block.get(Block.AIR), false, true);
            storage.clearTreasure();
            chestPlaced = false;
            state = EventState.FAILED;
            return false;
        }

        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage(plugin.getMessages().format("treasure-announcement",
                "{world}", location.getLevel().getName()));
        return true;
    }

    public boolean claim(Player player) {
        if (state != EventState.ACTIVE || claimed || !chestPlaced || location == null
                || storage.isTreasureClaimed()) return false;
        if (player.getLevel() != location.getLevel()) return false;
        int radius = Math.max(2, plugin.getConfig().getInt("losttreasure.claim-radius", 5));
        double dx = player.getX() - (location.getX() + 0.5);
        double dy = player.getY() - (location.getY() + 1);
        double dz = player.getZ() - (location.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > radius * radius) return false;

        int chestY = location.getY() + 1;
        if (!storage.ownsTreasureChest(location.getLevel(), location.getX(), chestY,
                location.getZ(), storageMarker)) return false;

        // Persistir la reclamación antes de pagar: ante un cierre abrupto no se duplicará.
        if (!storage.markTreasureClaimed()) return false;
        claimed = true;
        statistics.recordTreasureClaim(player);
        rewards.give(getId(), player);
        player.sendMessage(plugin.getMessages().get("treasure-claimed"));
        boolean cleaned = storage.removeOwnedTreasureChest(location.getLevel(), location.getX(), chestY,
                location.getZ(), storageMarker);
        if (cleaned) {
            chestPlaced = false;
            storage.clearTreasure();
        } else {
            plugin.getLogger().warning("La recompensa fue reclamada, pero la limpieza del cofre queda pendiente hasta cargar su chunk.");
            // Mantener el estado de propiedad en memoria hasta que el listener de carga
            // pueda retirar el cofre y limpiar el registro persistente.
        }
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

        int offset = switch (hintsSent) {
            case 0 -> 64;
            case 1 -> 24;
            default -> 0;
        };
        String hint;
        if (offset == 0) {
            hint = location.getX() + ", " + location.getZ();
        } else {
            hint = "X " + (location.getX() - offset) + " a " + (location.getX() + offset)
                    + ", Z " + (location.getZ() - offset) + " a " + (location.getZ() + offset);
        }
        plugin.getServer().broadcastMessage(plugin.getMessages().format("treasure-hint", "{hint}", hint));
        hintsSent++;
    }

    private void removeChestIfUnchanged() {
        if (location == null || !chestPlaced) {
            storage.clearTreasure();
            return;
        }
        int y = location.getY() + 1;
        boolean cleaned = storage.removeOwnedTreasureChest(location.getLevel(), location.getX(), y,
                location.getZ(), storageMarker);
        if (cleaned) {
            chestPlaced = false;
            storage.clearTreasure();
        } else {
            plugin.getLogger().warning("La limpieza del cofre queda pendiente hasta que vuelva a cargarse su chunk.");
        }
    }

    @Override public void stop() {
        removeChestIfUnchanged();
        state = EventState.COMPLETED;
    }

    @Override public boolean isFinished() {
        return state == EventState.COMPLETED || state == EventState.FAILED;
    }

    public boolean ownsChest(cn.nukkit.level.Level level, int x, int y, int z) {
        return chestPlaced && !claimed && location != null
                && location.getLevel() == level && location.getX() == x
                && location.getY() + 1 == y && location.getZ() == z
                && storage.ownsTreasureChest(level, x, y, z, storageMarker);
    }

    public boolean isClaimed() { return claimed || storage.isTreasureClaimed(); }
    public EventLocation getLocation() { return location; }
}
