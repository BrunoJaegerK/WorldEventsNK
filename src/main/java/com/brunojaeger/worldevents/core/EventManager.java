package com.brunojaeger.worldevents.core;

import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.event.meteor.MeteorFallEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class EventManager {
    private final WorldEventsNK plugin;
    private final Map<String, WorldEvent> events = new LinkedHashMap<>();

    public EventManager(WorldEventsNK plugin, SafeLocationFinder finder) {
        this.plugin = plugin;
        register(new MeteorFallEvent(plugin, finder));
        register(new LostTreasureEvent(plugin, finder));
    }
    public void register(WorldEvent event) { events.put(event.getId().toLowerCase(), event); }
    public Collection<WorldEvent> getEvents() { return events.values(); }
    public WorldEvent getEvent(String id) { return id == null ? null : events.get(id.toLowerCase()); }

    public WorldEvent getActiveEvent() {
        for (WorldEvent event : events.values()) {
            if (event.getState() == EventState.ACTIVE || event.getState() == EventState.PREPARING) return event;
        }
        return null;
    }

    public boolean start(String id) {
        WorldEvent event = getEvent(id);
        if (event == null || !event.canStart() || getActiveEvent() != null) return false;
        if (!plugin.getEventsConfig().getBoolean(id + ".enabled", true)) return false;
        return event.start();
    }

    public boolean startRandom() {
        int meteor = plugin.getEventsConfig().getBoolean("meteorfall.enabled", true)
                ? Math.max(0, plugin.getEventsConfig().getInt("meteorfall.weight", 50)) : 0;
        int treasure = plugin.getEventsConfig().getBoolean("losttreasure.enabled", true)
                ? Math.max(0, plugin.getEventsConfig().getInt("losttreasure.weight", 50)) : 0;
        int total = meteor + treasure;
        if (total <= 0 || getActiveEvent() != null) return false;
        int roll = ThreadLocalRandom.current().nextInt(total);
        return roll < meteor ? start("meteorfall") : start("losttreasure");
    }

    public void tick() { for (WorldEvent event : events.values()) event.tick(); }
    public void stopActive() { WorldEvent event = getActiveEvent(); if (event != null) event.stop(); }
    public boolean isActive() { return getActiveEvent() != null; }
}
