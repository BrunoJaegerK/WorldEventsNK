package com.brunojaeger.worldevents.event.treasure;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.world.EventLocation;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

public final class LostTreasureEvent implements WorldEvent {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final RewardManager rewards;
    private EventState state = EventState.IDLE;
    private EventLocation location;
    private int remainingSeconds;
    private boolean claimed;

    public LostTreasureEvent(WorldEventsNK plugin, SafeLocationFinder locations, RewardManager rewards) {
        this.plugin = plugin;
        this.locations = locations;
        this.rewards = rewards;
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
        if (!plugin.getProtectionManager().canModify(location.getLevel(), location.getX(), chestY,
                location.getZ(), getId())) {
            state = EventState.FAILED;
            return false;
        }
        location.getLevel().setBlock(location.getX(), chestY, location.getZ(),
                Block.get(Block.CHEST), false, true);
        remainingSeconds = Math.max(30, plugin.getConfig().getInt("events.duration-seconds", 600));
        state = EventState.ACTIVE;
        plugin.getServer().broadcastMessage("§6[WorldEvents] §e¡Se ha escondido un tesoro en "
                + location.getLevel().getName() + "§e! Busca pistas con §f/we claim§e.");
        return true;
    }

    public boolean claim(Player player) {
        if (state != EventState.ACTIVE || claimed || location == null) return false;
        if (player.getLevel() != location.getLevel()) return false;
        int radius = Math.max(2, plugin.getConfig().getInt("losttreasure.claim-radius", 5));
        double dx = player.getX() - (location.getX() + 0.5);
        double dy = player.getY() - (location.getY() + 1);
        double dz = player.getZ() - (location.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz > radius * radius) return false;

        // Marcar antes de entregar para impedir doble reclamación en llamadas consecutivas.
        claimed = true;
        rewards.give(getId(), player);
        player.sendMessage("§aHas reclamado el tesoro perdido.");
        player.getLevel().setBlock(location.getX(), location.getY() + 1, location.getZ(),
                Block.get(Block.AIR), false, true);
        state = EventState.COMPLETED;
        return true;
    }

    @Override
    public void tick() {
        if (state != EventState.ACTIVE) return;
        if (--remainingSeconds <= 0) {
            removeChestIfUnchanged();
            state = EventState.COMPLETED;
        }
    }

    private void removeChestIfUnchanged() {
        if (location == null) return;
        int y = location.getY() + 1;
        if (location.getLevel().getBlock(location.getX(), y, location.getZ()).getId() == Block.CHEST
                && plugin.getProtectionManager().canModify(location.getLevel(), location.getX(), y,
                location.getZ(), getId())) {
            location.getLevel().setBlock(location.getX(), y, location.getZ(),
                    Block.get(Block.AIR), false, true);
        }
    }

    @Override public void stop() { removeChestIfUnchanged(); state = EventState.COMPLETED; }
    @Override public boolean isFinished() { return state == EventState.COMPLETED || state == EventState.FAILED; }
    public boolean isClaimed() { return claimed; }
    public EventLocation getLocation() { return location; }
}
