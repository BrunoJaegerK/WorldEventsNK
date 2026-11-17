package com.brunojaeger.worldevents.event;

import com.brunojaeger.worldevents.core.EventState;
import com.brunojaeger.worldevents.world.EventLocation;

public interface WorldEvent {
    String getId();
    EventState getState();
    boolean start();
    void tick();
    void stop();
    boolean isFinished();
    default int getRemainingSeconds() { return 0; }
    default EventLocation getLocation() { return null; }
}
