package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.math.Vector3;
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

        remainingSeconds = Math.max(30, plugin.getConfig().getInt("losttreasure.duration-seconds",
                plugin.getConfig().getInt("events.duration-seconds", 600)));
        state = EventState.ACTIVE;
        if (visualEffectsEnabled()) {
            location.getLevel().addSound(chestPosition(), Sound.RANDOM_CHESTOPEN, 0.8f, 1.0f);
            renderTreasureMarker();
        }
        plugin.getServer().broadcastMessage(plugin.getMessages().format("treasure-announcement",
                "{world}", location.getLevel().getName()));
        return true;
    }

    public boolean claim(Player player) {
        if (state != EventState.ACTIVE || claimed || !chestPlaced || location == null
                || storage.isTreasureClaimed()) return false;
        if (player.getLevel() != location.getLevel()) return false;
        int radius = Math.max(2, Math.min(1000, plugin.getConfig().getInt("losttreasure.claim-radius", 5)));
        double dx = player.getX() - (location.getX() + 0.5);
        double dy = player.getY() - (location.getY() + 1);
        double dz = player.getZ() - (location.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > (double) radius * radius) return false;

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
            storage.markTreasureCleanupPending();
            // Mantener el estado de propiedad en memoria hasta que el listener de carga
            // pueda retirar el cofre y limpiar el registro persistente.
        }
        if (visualEffectsEnabled()) {
            location.getLevel().addSound(chestPosition(), Sound.RANDOM_CHESTCLOSED, 0.8f, 1.0f);
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

    private boolean visualEffectsEnabled() {
        return plugin.getConfig().getBoolean("losttreasure.visual-effects", true);
    }

    private Vector3 chestPosition() {
        return new Vector3(location.getX() + 0.5, location.getY() + 1.2, location.getZ() + 0.5);
    }

    /** Partículas discretas alrededor del cofre; no modifica bloques ni chunks. */
    private void renderTreasureMarker() {
        if (location == null || location.getLevel() == null) return;
        double centerX = location.getX() + 0.5;
        double centerY = location.getY() + 1.35;
        double centerZ = location.getZ() + 0.5;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vector3 point = new Vector3(centerX + Math.cos(angle) * 0.8,
                    centerY + (i % 2 == 0 ? 0.25 : 0.0), centerZ + Math.sin(angle) * 0.8);
            location.getLevel().addParticle(new HappyVillagerParticle(point));
        }
    }

    private void sendProgressiveHint() {
        int interval = Math.max(15, Math.min(86_400,
                plugin.getConfig().getInt("losttreasure.hint-interval-seconds", 120)));
        int maximum = Math.max(0, Math.min(3, plugin.getConfig().getInt("losttreasure.maximum-hints", 3)));
        if (hintsSent >= maximum || elapsedSeconds < (hintsSent + 1) * interval) return;

        int radius = switch (hintsSent) {
            case 0 -> Math.max(0, Math.min(1_000_000,
                    plugin.getConfig().getInt("losttreasure.hint-radius-first", 96)));
            case 1 -> Math.max(0, Math.min(1_000_000,
                    plugin.getConfig().getInt("losttreasure.hint-radius-second", 32)));
            default -> Math.max(0, Math.min(1_000_000,
                    plugin.getConfig().getInt("losttreasure.hint-radius-third", 0)));
        };
        String hint;
        if (radius == 0) {
            hint = "X " + location.getX() + ", Z " + location.getZ();
        } else {
            long minX = (long) location.getX() - radius;
            long maxX = (long) location.getX() + radius;
            long minZ = (long) location.getZ() - radius;
            long maxZ = (long) location.getZ() + radius;
            hint = "X " + minX + " a " + maxX + ", Z " + minZ + " a " + maxZ;
        }
        plugin.getServer().broadcastMessage(plugin.getMessages().format("treasure-hint", "{hint}", hint));
        if (visualEffectsEnabled()) {
            renderTreasureMarker();
            location.getLevel().addSound(chestPosition(), Sound.RANDOM_ORB, 0.7f, 1.2f);
        }
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
            storage.markTreasureCleanupPending();
        }
    }

    @Override public void stop() {
        removeChestIfUnchanged();
        state = EventState.COMPLETED;
    }

    @Override public boolean isFinished() {
        return state == EventState.COMPLETED || state == EventState.FAILED;
    }

    @Override public int getRemainingSeconds() { return Math.max(0, remainingSeconds); }

    public boolean ownsChest(cn.nukkit.level.Level level, int x, int y, int z) {
        return chestPlaced && !claimed && location != null
                && location.getLevel() == level && location.getX() == x
                && location.getY() + 1 == y && location.getZ() == z
                && storage.ownsTreasureChest(level, x, y, z, storageMarker);
    }

    public boolean isClaimed() { return claimed || storage.isTreasureClaimed(); }
    @Override public EventLocation getLocation() { return location; }
}
