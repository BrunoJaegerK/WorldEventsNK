package com.brunojaeger.worldevents.integration.protection;

import cn.nukkit.level.Level;

/**
 * API de extensión para plugins de protección de regiones.
 * Un adaptador debe devolver false si no puede verificar con seguridad el permiso.
 */
public interface RegionProtectionProvider {
    String getName();
    boolean isAvailable();
    boolean canModify(Level level, int x, int y, int z, String eventId);
}
