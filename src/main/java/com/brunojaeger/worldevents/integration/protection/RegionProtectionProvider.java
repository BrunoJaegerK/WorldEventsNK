package com.brunojaeger.worldevents.integration.protection;

import cn.nukkit.level.Level;

/**
 * Bridge implemented by integrations for third-party region plugins.
 * Return true when the queried position is protected.
 */
public interface RegionProtectionProvider {
    String getName();
    boolean isAvailable();
    boolean isProtected(Level level, int x, int y, int z);
}
