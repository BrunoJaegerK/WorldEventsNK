package com.brunojaeger.worldevents.event;

import com.brunojaeger.worldevents.core.EventState;

public interface WorldEvent {
    String getId();
    EventState getState();
    boolean start();
    void tick();
    void stop();
    boolean isFinished();
}
