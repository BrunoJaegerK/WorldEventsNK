package com.brunojaeger.worldevents.core;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.history.EventHistoryManager;
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
    private final EventHistoryManager history;
    private WorldEvent activeEvent;
    private int automaticCountdown;

    public EventManager(WorldEventsNK plugin, ProtectionManager protection, EventStorage storage, StatisticsManager statistics, EventHistoryManager history) {
        this.plugin = plugin;
        this.history = history;
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
        if (event == null) return false;
        try {
            if (!event.start()) {
                history.record(normalized, "FAILED", event.getLocation());
                plugin.getLogger().warning("No se pudo iniciar " + normalized + ": no se encontró una ubicación válida.");
                return false;
            }
        } catch (RuntimeException error) {
            try {
                event.stop();
            } catch (RuntimeException cleanupError) {
                plugin.getLogger().error("Falló la limpieza tras un error al iniciar " + normalized + ".", cleanupError);
            }
            history.record(normalized, "FAILED", event.getLocation());
            plugin.getLogger().error("Excepción al iniciar el evento " + normalized + ".", error);
            return false;
        }
        activeEvent = event;
        history.record(normalized, "STARTED", event.getLocation());
        return true;
    }

    private String chooseRandomEvent() {
        List<String> candidates = new ArrayList<>();
        long totalWeight = 0L;
        for (String id : registry.getFactories().keySet()) {
            if (!eventDefinitions.getBoolean(id + ".enabled", true) || getCooldownRemainingSeconds(id) > 0) continue;
            int weight = Math.max(0, eventDefinitions.getInt(id + ".weight", 50));
            if (weight == 0) continue;
            candidates.add(id);
            totalWeight += (long) weight;
        }
        if (totalWeight <= 0) return null;
        if (plugin.getConfig().getBoolean("events.avoid-consecutive-same-event", true)
                && candidates.size() > 1) {
            String previous = history.getMostRecentlyStartedEvent();
            if (previous != null && candidates.remove(previous)) {
                totalWeight = 0L;
                for (String id : candidates) {
                    totalWeight += Math.max(0, eventDefinitions.getInt(id + ".weight", 50));
                }
            }
        }
        if (totalWeight <= 0) return null;
        long selection = ThreadLocalRandom.current().nextLong(totalWeight);
        for (String id : candidates) {
            selection -= (long) Math.max(0, eventDefinitions.getInt(id + ".weight", 50));
            if (selection < 0) return id;
        }
        return candidates.get(candidates.size() - 1);
    }

    public void tick() {
        if (!plugin.getConfig().getBoolean("events.enabled", true)) {
            if (activeEvent != null) stop();
            return;
        }
        if (activeEvent != null) {
            WorldEvent current = activeEvent;
            try {
                current.tick();
            } catch (RuntimeException error) {
                plugin.getLogger().error("Error durante tick del evento " + current.getId() + ".", error);
                try {
                    current.stop();
                } catch (RuntimeException cleanupError) {
                    plugin.getLogger().error("No se pudo completar la limpieza del evento " + current.getId() + ".", cleanupError);
                }
                history.record(current.getId(), "FAILED", current.getLocation());
                activeEvent = null;
                resetAutomaticCountdown();
                return;
            }
            if (current.isFinished()) {
                history.record(current.getId(), current.getState() == EventState.FAILED ? "FAILED" : "COMPLETED", current.getLocation());
                String resultKey = current.getState() == EventState.FAILED ? "event-failed" : "event-finished";
                plugin.getServer().broadcastMessage(plugin.getMessages().format(
                        resultKey, "{event}", current.getId()));
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
        int min = Math.max(60, Math.min(86_400,
                plugin.getConfig().getInt("events.minimum-interval-seconds", 1800)));
        int max = Math.max(min, Math.min(86_400,
                plugin.getConfig().getInt("events.maximum-interval-seconds", 3600)));
        automaticCountdown = ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public boolean stop() {
        if (activeEvent == null) return false;
        WorldEvent current = activeEvent;
        try {
            current.stop();
            history.record(current.getId(), "STOPPED", current.getLocation());
        } catch (RuntimeException error) {
            history.record(current.getId(), "FAILED", current.getLocation());
            plugin.getLogger().error("Error al detener el evento " + current.getId() + ".", error);
        } finally {
            activeEvent = null;
            resetAutomaticCountdown();
        }
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

    public boolean isEventEnabled(String id) {
        return plugin.getConfig().getBoolean("events.enabled", true)
                && registry.contains(id)
                && eventDefinitions.getBoolean(id + ".enabled", true);
    }

    public int getEventWeight(String id) {
        return Math.max(0, eventDefinitions.getInt(id + ".weight", 50));
    }

    public int getCooldownRemainingSeconds(String id) {
        int cooldown = Math.max(0, Math.min(604_800,
                eventDefinitions.getInt(id + ".cooldown-seconds", 0)));
        if (cooldown == 0) return 0;
        long lastStart = history.getLastStartTimestamp(id);
        if (lastStart <= 0L) return 0;
        long elapsed = Math.max(0L, (System.currentTimeMillis() - lastStart) / 1000L);
        return (int) Math.max(0L, cooldown - elapsed);
    }

    public int getAutomaticCountdown() {
        return Math.max(0, automaticCountdown);
    }

    public void reload() {
        plugin.reloadConfig();
        eventDefinitions.reload();
        rewards.reload();
        economy.reload();
        plugin.getMessages().reload();
        resetAutomaticCountdown();
    }

    public void shutdown() {
        if (activeEvent != null) {
            WorldEvent current = activeEvent;
            try {
                current.stop();
                history.record(current.getId(), "SERVER_STOP", current.getLocation());
            } catch (RuntimeException error) {
                history.record(current.getId(), "FAILED", current.getLocation());
                plugin.getLogger().error("Error durante la limpieza final del evento " + current.getId() + ".", error);
            } finally {
                activeEvent = null;
            }
        }
    }
}
