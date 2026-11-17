package com.brunojaeger.worldevents.core;

import cn.nukkit.scheduler.TaskHandler;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.util.concurrent.ThreadLocalRandom;

public final class EventScheduler implements Runnable {
    private final WorldEventsNK plugin;
    private final EventManager manager;
    private TaskHandler handler;
    private int elapsed;
    private int next;

    public EventScheduler(WorldEventsNK plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        scheduleNext();
    }
    public void start() {
        if (handler == null) handler = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this, 20);
    }
    public void stop() {
        if (handler != null) { handler.cancel(); handler = null; }
    }
    public void refreshSchedule() { scheduleNext(); }
    private void scheduleNext() {
        int min = Math.max(60, plugin.getConfig().getInt("events.minimum-interval", 1800));
        int max = Math.max(min, plugin.getConfig().getInt("events.maximum-interval", 3600));
        next = ThreadLocalRandom.current().nextInt(min, max + 1);
        elapsed = 0;
    }
    @Override
    public void run() {
        manager.tick();
        if (!plugin.getConfig().getBoolean("events.enabled", true)
                || !plugin.getConfig().getBoolean("events.automatic", true)) return;
        if (manager.isActive()) return;
        if (++elapsed < next) return;
        if (plugin.getServer().getOnlinePlayersCount() < plugin.getConfig().getInt("events.minimum-online-players", 3)) return;
        manager.startRandom();
        scheduleNext();
    }
}
