package com.brunojaeger.worldevents.integration.protection;

import cn.nukkit.level.Level;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProtectionManager {
    private final WorldEventsNK plugin;
    private final List<RegionProtectionProvider> providers = new CopyOnWriteArrayList<>();

    public ProtectionManager(WorldEventsNK plugin) {
        this.plugin = plugin;
    }

    public void registerProvider(RegionProtectionProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            plugin.getLogger().info("Proveedor de protección registrado: " + provider.getName());
        }
    }

    public void unregisterProvider(RegionProtectionProvider provider) {
        providers.remove(provider);
    }

    public boolean canModify(Level level, int x, int y, int z, String eventId) {
        if (!plugin.getConfig().getBoolean("protection.enabled", true)) {
            return true;
        }
        for (RegionProtectionProvider provider : providers) {
            try {
                if (provider.isAvailable() && !provider.canModify(level, x, y, z, eventId)) {
                    return false;
                }
            } catch (Throwable error) {
                plugin.getLogger().warning("El proveedor " + provider.getName()
                        + " falló al comprobar una región; se deniega la modificación por seguridad.");
                if (plugin.getConfig().getBoolean("protection.fail-safe", true)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getProviderCount() {
        return providers.size();
    }
}
