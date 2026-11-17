package com.brunojaeger.worldevents.core;

import cn.nukkit.scheduler.TaskHandler;
import com.brunojaeger.worldevents.WorldEventsNK;

public final class EventScheduler {
    private final WorldEventsNK plugin;
    private final EventManager manager;
    private TaskHandler task;

    public EventScheduler(WorldEventsNK plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        stop();
        task = plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, manager::tick, 20);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
