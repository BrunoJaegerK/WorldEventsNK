package com.brunojaeger.worldevents.core;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.event.meteor.MeteorFallEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.statistics.StatisticsManager;
import com.brunojaeger.worldevents.storage.EventStorage;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class EventManager {
    private final WorldEventsNK plugin;
    private final SafeLocationFinder locations;
    private final EconomyManager economy;
    private final RewardManager rewards;
    private final Config eventDefinitions;
    private final EventRegistry registry = new EventRegistry();
    private WorldEvent activeEvent;
    private int automaticCountdown;

    public EventManager(WorldEventsNK plugin, ProtectionManager protection, EventStorage storage, StatisticsManager statistics) {
        this.plugin = plugin;
        this.locations = new SafeLocationFinder(plugin, protection);
        this.economy = new EconomyManager(plugin);
        this.rewards = new RewardManager(plugin, economy);
        this.eventDefinitions = new Config(new File(plugin.getDataFolder(), "events.yml"), Config.YAML);
        registry.register("meteorfall", () -> new MeteorFallEvent(plugin, locations));
        registry.register("losttreasure", () -> new LostTreasureEvent(plugin, locations, rewards, storage, statistics));
        resetAutomaticCountdown();
    }

    public boolean start(String id) {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) return false;
        if (activeEvent != null && !activeEvent.isFinished()) return false;
        String normalized = id.toLowerCase(Locale.ROOT);
        if ("random".equals(normalized)) normalized = chooseRandomEvent();
        if (normalized == null || !registry.contains(normalized)
                || !eventDefinitions.getBoolean(normalized + ".enabled", true)) return false;

        WorldEvent event = registry.create(normalized);
        if (event == null || !event.start()) {
            plugin.getLogger().warning("No se pudo iniciar " + normalized + ": no se encontró una ubicación válida.");
            return false;
        }
        activeEvent = event;
        return true;
    }

    private String chooseRandomEvent() {
        List<String> candidates = new ArrayList<>();
        int totalWeight = 0;
        for (String id : registry.getFactories().keySet()) {
            if (!eventDefinitions.getBoolean(id + ".enabled", true)) continue;
            int weight = Math.max(0, eventDefinitions.getInt(id + ".weight", 50));
            if (weight == 0) continue;
            candidates.add(id);
            totalWeight += weight;
        }
        if (totalWeight <= 0) return null;
        int selection = ThreadLocalRandom.current().nextInt(totalWeight);
        for (String id : candidates) {
            selection -= eventDefinitions.getInt(id + ".weight", 50);
            if (selection < 0) return id;
        }
        return candidates.get(candidates.size() - 1);
    }

    public void tick() {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) {
            if (activeEvent != null) {
                activeEvent.stop();
                activeEvent = null;
                resetAutomaticCountdown();
            }
            return;
        }
        if (activeEvent != null) {
            activeEvent.tick();
            if (activeEvent.isFinished()) {
                plugin.getServer().broadcastMessage(plugin.getMessages().format(
                        "event-finished", "{event}", activeEvent.getId()));
                activeEvent = null;
                resetAutomaticCountdown();
            }
            return;
        }
        if (!plugin.getConfig().getBoolean("events.automatic", true)) return;
        if (plugin.getServer().getOnlinePlayers().size()
                < plugin.getConfig().getInt("events.minimum-online-players", 3)) return;
        if (--automaticCountdown <= 0 && !start("random")) resetAutomaticCountdown();
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
        return activeEvent instanceof LostTreasureEvent && ((LostTreasureEvent) activeEvent).claim(player);
    }

    public boolean isTreasureBlock(Level level, int x, int y, int z) {
        return activeEvent instanceof LostTreasureEvent treasure && treasure.ownsChest(level, x, y, z);
    }

    public WorldEvent getActiveEvent() { return activeEvent; }
    public boolean hasActiveEvent() { return activeEvent != null; }
    public EventRegistry getRegistry() { return registry; }

    public void reload() {
        plugin.reloadConfig();
        eventDefinitions.reload();
        rewards.reload();
        plugin.getMessages().reload();
        resetAutomaticCountdown();
    }

    public void shutdown() {
        if (activeEvent != null) {
            activeEvent.stop();
            activeEvent = null;
        }
    }
}
