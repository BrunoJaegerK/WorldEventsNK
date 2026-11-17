package com.brunojaeger.worldevents.integration.protection;

import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProtectionManager {
    private final WorldEventsNK plugin;
    private final Config integrations;
    private final List<RegionProtectionProvider> providers = new CopyOnWriteArrayList<>();

    public ProtectionManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.integrations = new Config(new File(plugin.getDataFolder(), "integrations.yml"), Config.YAML);
    }

    public void reload() {
        integrations.reload();
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

    private boolean enabled() {
        return integrations.getBoolean("protection.enabled",
                plugin.getConfig().getBoolean("protection.enabled", true));
    }

    private boolean failSafe() {
        return integrations.getBoolean("protection.fail-safe",
                plugin.getConfig().getBoolean("protection.fail-safe", true));
    }

    public boolean canModify(Level level, int x, int y, int z, String eventId) {
        if (!enabled()) return true;
        for (RegionProtectionProvider provider : providers) {
            try {
                if (!provider.isAvailable()) {
                    if (failSafe()) {
                        plugin.getLogger().warning("El proveedor " + provider.getName()
                                + " no está disponible; se deniega la modificación por seguridad.");
                        return false;
                    }
                    continue;
                }
                if (!provider.canModify(level, x, y, z, eventId)) return false;
            } catch (RuntimeException | LinkageError error) {
                plugin.getLogger().warning("El proveedor " + provider.getName()
                        + " falló al comprobar una región: " + error.getMessage());
                if (failSafe()) return false;
            }
        }
        return true;
    }

    public int getProviderCount() { return providers.size(); }
}