package com.brunojaeger.worldevents.integration.protection;

import cn.nukkit.level.Level;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProtectionManager {
    private final WorldEventsNK plugin;
    private final List<RegionProtectionProvider> providers = new CopyOnWriteArrayList<>();

    public ProtectionManager(WorldEventsNK plugin) { this.plugin = plugin; }

    public void registerProvider(RegionProtectionProvider provider) {
        if (provider != null && !providers.contains(provider)) providers.add(provider);
    }
    public void unregisterProvider(RegionProtectionProvider provider) { providers.remove(provider); }

    public boolean isProtected(Level level, int x, int y, int z) {
        if (!plugin.getConfig().getBoolean("protection.enabled", true)) return false;
        Object raw = plugin.getConfig().get("protection.regions");
        if (raw instanceof List<?>) {
            for (Object value : (List<?>) raw) {
                if (!(value instanceof Map<?, ?>)) continue;
                Map<?, ?> region = (Map<?, ?>) value;
                if (!level.getFolderName().equalsIgnoreCase(String.valueOf(region.get("world")))) continue;
                if (inside(x, number(region.get("min-x"), Integer.MIN_VALUE), number(region.get("max-x"), Integer.MAX_VALUE))
                        && inside(y, number(region.get("min-y"), Integer.MIN_VALUE), number(region.get("max-y"), Integer.MAX_VALUE))
                        && inside(z, number(region.get("min-z"), Integer.MIN_VALUE), number(region.get("max-z"), Integer.MAX_VALUE))) return true;
            }
        }
        for (RegionProtectionProvider provider : providers) {
            try {
                if (provider.isAvailable() && provider.isProtected(level, x, y, z)) return true;
            } catch (Throwable error) {
                plugin.getLogger().warning("Proveedor de protección " + provider.getName()
                        + " falló; se bloquea la ubicación por seguridad: " + error.getMessage());
                if (plugin.getConfig().getBoolean("protection.fail-safe", true)) return true;
            }
        }
        return false;
    }

    private boolean inside(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }
    private int number(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
}
