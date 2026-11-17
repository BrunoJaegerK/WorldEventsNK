package com.brunojaeger.worldevents.core;

import cn.nukkit.Player;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.event.meteor.MeteorFallEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class EventManager {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final EconomyManager economy;
    private final RewardManager rewards;
    private WorldEvent activeEvent;
    private int automaticCountdown;

    public EventManager(WorldEventsNK plugin, ProtectionManager protection) {
        this.plugin = plugin;
        this.locations = new SafeLocationFinder(plugin, protection);
        this.economy = new EconomyManager(plugin);
        this.rewards = new RewardManager(plugin, economy);
        resetAutomaticCountdown();
    }

    public boolean start(String id) {
        if (activeEvent != null && !activeEvent.isFinished()) return false;
        String normalized = id.toLowerCase(Locale.ROOT);
        if ("random".equals(normalized)) normalized = chooseRandomEvent();
        if (normalized == null) return false;

        WorldEvent event;
        switch (normalized) {
            case "meteorfall" -> event = new MeteorFallEvent(plugin, locations);
            case "losttreasure" -> event = new LostTreasureEvent(plugin, locations, rewards);
            default -> { return false; }
        }
        if (!event.start()) {
            plugin.getLogger().warning("No se pudo iniciar el evento " + normalized + ": no hay ubicación válida.");
            return false;
        }
        activeEvent = event;
        return true;
    }

    private String chooseRandomEvent() {
        List<String> enabled = new ArrayList<>();
        String configured = plugin.getConfig().getString("events.enabled-events", "meteorfall,losttreasure");
        for (String id : configured.split(",")) {
            String value = id.trim().toLowerCase(Locale.ROOT);
            if (value.equals("meteorfall") || value.equals("losttreasure")) enabled.add(value);
        }
        if (enabled.isEmpty()) return null;
        return enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()));
    }

    public void tick() {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) return;
        if (activeEvent != null) {
            activeEvent.tick();
            if (activeEvent.isFinished()) {
                plugin.getServer().broadcastMessage("§6[WorldEvents] §aEl evento "
                        + activeEvent.getId() + " ha finalizado.");
                activeEvent = null;
                resetAutomaticCountdown();
            }
            return;
        }
        if (!plugin.getConfig().getBoolean("events.automatic", true)) return;
        if (plugin.getServer().getOnlinePlayers().size()
                < plugin.getConfig().getInt("events.minimum-online-players", 3)) return;
        if (--automaticCountdown <= 0) {
            if (!start("random")) resetAutomaticCountdown();
        }
    }

    private void resetAutomaticCountdown() {
        int min = Math.max(60, plugin.getConfig().getInt("events.minimum-interval-seconds", 1800));
        int max = Math.max(min, plugin.getConfig().getInt("events.maximum-interval-seconds", 3600));
        automaticCountdown = ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public boolean stop() {
        if (activeEvent == null) return false;
        activeEvent.stop();
        activeEvent = null;
        resetAutomaticCountdown();
        return true;
    }

    public boolean claimTreasure(Player player) {
        return activeEvent instanceof LostTreasureEvent
                && ((LostTreasureEvent) activeEvent).claim(player);
    }

    public WorldEvent getActiveEvent() { return activeEvent; }
    public boolean hasActiveEvent() { return activeEvent != null; }

    public void reload() {
        plugin.reloadConfig();
        resetAutomaticCountdown();
    }

    public void shutdown() {
        if (activeEvent != null) {
            activeEvent.stop();
            activeEvent = null;
        }
    }
}
