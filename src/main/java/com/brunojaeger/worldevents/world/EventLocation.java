package com.brunojaeger.worldevents.world;

import cn.nukkit.level.Level;

public final class EventLocation {
    private final Level level;
    private final int x;
    private final int y;
    private final int z;

    public EventLocation(Level level, int x, int y, int z) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Level getLevel() { return level; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}
