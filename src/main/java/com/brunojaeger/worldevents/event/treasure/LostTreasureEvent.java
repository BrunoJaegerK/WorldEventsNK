package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
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
    private int remainingSeconds;
    private int elapsedSeconds;
    private int hintsSent;
    private boolean claimed;
    private boolean chestPlaced;
    private String chestMarker;

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

        chestMarker = storage.beginTreasure(location.getLevel(), location.getX(), chestY, location.getZ());
        if (chestMarker == null) {
            state = EventState.FAILED;
            plugin.getLogger().warning("No se pudo guardar el registro de limpieza del tesoro.");
            return false;
        }

        if (!location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                Block.get(Block.CHEST), false, true)) {
            storage.clearTreasure();
            state = EventState.FAILED;
            return false;
        }

        BaseFullChunk chunk = location.getLevel().getChunkIfLoaded(location.getX() >> 4, location.getZ() >> 4);
        if (chunk == null) {
            location.getLevel().setBlock(location.getX(), chestY, location.getZ(), Block.get(Block.AIR), false, true);
            storage.clearTreasure();
            state = EventState.FAILED;
            return false;
        }

        BlockEntity entity = location.getLevel().getBlockEntity(new Vector3(location.getX(), chestY, location.getZ()));
        if (entity == null) {
            CompoundTag nbt = new CompoundTag()
                    .putString("id", BlockEntity.CHEST)
                    .putInt("x", location.getX())
                    .putInt("y", chestY)
                    .putInt("z", location.getZ());
            entity = BlockEntity.createBlockEntity(BlockEntity.CHEST, chunk, nbt);
        }
        if (!(entity instanceof BlockEntityChest chest)) {
            if (entity != null) entity.close();
            location.getLevel().setBlock(location.getX(), chestY, location.getZ(), Block.get(Block.AIR), false, true);
            storage.clearTreasure();
            state = EventState.FAILED;
            plugin.getLogger().warning("PM1E no pudo crear el block entity del cofre del evento.");
            return false;
        }

        chest.setName(chestMarker);
        chest.saveNBT();
        if (!storage.markTreasureActive()) {
            chest.close();
            location.getLevel().setBlock(location.getX(), chestY, location.getZ(), Block.get(Block.AIR), false, true);
            storage.clearTreasure();
            state = EventState.FAILED;
            return false;
        }

        chestPlaced = true;
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage("§6[WorldEvents] §e¡Se ha escondido un tesoro en "
                + location.getLevel().getName() + "§e! Busca pistas con §f/we claim§e.");
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
                location.getZ(), chestMarker)) return false;

        // Persistir la reclamacion antes de entregar premios: evita duplicaciones tras un crash.
        if (!storage.markTreasureClaimed()) {
            plugin.getLogger().warning("No se pudo persistir la reclamacion del tesoro; se cancela el pago.");
            return false;
        }
        claimed = true;
        rewards.give(getId(), player);
        statistics.recordTreasureClaim(player);
        player.sendMessage("§aHas reclamado el tesoro perdido.");
        storage.removeOwnedTreasureChest(location.getLevel(), location.getX(), chestY, location.getZ(), chestMarker);
        chestPlaced = false;
        storage.clearTreasure();
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
        String hint = offset == 0
                ? location.getX() + ", " + location.getZ()
                : "X " + (location.getX() - offset) + " a " + (location.getX() + offset)
                    + ", Z " + (location.getZ() - offset) + " a " + (location.getZ() + offset);
        plugin.getServer().broadcastMessage(plugin.getMessages().get("treasure-hint")
                .replace("{hint}", hint));
        hintsSent++;
    }

    private void removeChestIfUnchanged() {
        if (location == null || !chestPlaced) {
            storage.clearTreasure();
            return;
        }
        int y = location.getY() + 1;
        storage.removeOwnedTreasureChest(location.getLevel(), location.getX(), y, location.getZ(), chestMarker);
        chestPlaced = false;
        storage.clearTreasure();
    }

    public boolean ownsChest(cn.nukkit.level.Level level, int x, int y, int z) {
        return chestPlaced && !claimed && location != null && location.getLevel() == level
                && location.getX() == x && location.getY() + 1 == y && location.getZ() == z
                && storage.ownsTreasureChest(level, x, y, z, chestMarker);
    }

    @Override public void stop() {
        removeChestIfUnchanged();
        state = EventState.COMPLETED;
    }

    @Override public boolean isFinished() {
        return state == EventState.COMPLETED || state == EventState.FAILED;
    }

    public boolean isClaimed() { return claimed || storage.isTreasureClaimed(); }
    public EventLocation getLocation() { return location; }
}
