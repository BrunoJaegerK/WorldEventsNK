package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;
import java.lang.reflect.Method;

public final class EconomyManager {
    private final WorldEventsNK plugin;
    private final Config integrations;
    private Object economy;
    private Method addMoney;
    private volatile EconomyProvider externalProvider;

    public EconomyManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.integrations = new Config(new File(plugin.getDataFolder(), "integrations.yml"), Config.YAML);
        connect();
    }

    public boolean registerProvider(EconomyProvider provider) {
        if (provider == null || externalProvider != null) return false;
        externalProvider = provider;
        plugin.getLogger().info("Proveedor económico externo registrado: " + provider.getName());
        return true;
    }

    public boolean unregisterProvider(EconomyProvider provider) {
        if (provider == null || externalProvider != provider) return false;
        externalProvider = null;
        return true;
    }

    public void reload() {
        integrations.reload();
        economy = null;
        addMoney = null;
        connect();
    }

    private boolean enabled() {
        return integrations.getBoolean("economy.enabled",
                plugin.getConfig().getBoolean("economy.enabled", true));
    }

    private String providerName() {
        return integrations.getString("economy.provider",
                plugin.getConfig().getString("economy.provider", "EconomyAPI"));
    }

    private void connect() {
        if (!enabled()) {
            plugin.getLogger().info("La integración económica está desactivada en integrations.yml.");
            return;
        }
        String provider = providerName();
        String className = integrations.getString("economy.class", "me.onebone.economyapi.EconomyAPI");
        try {
            Plugin economyPlugin = plugin.getServer().getPluginManager().getPlugin(provider);
            if (economyPlugin == null || !economyPlugin.isEnabled()) {
                plugin.getLogger().info(provider + " no está disponible; las recompensas monetarias se omiten.");
                return;
            }
            Class<?> type = Class.forName(className, true, economyPlugin.getClass().getClassLoader());
            Object instance = type.getMethod("getInstance").invoke(null);
            Method method = type.getMethod("addMoney", Player.class, double.class);
            this.economy = instance;
            this.addMoney = method;
            plugin.getLogger().info("Conexión económica establecida con " + provider + ".");
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("No se pudo conectar con " + provider + ": " + error.getMessage());
            this.economy = null;
            this.addMoney = null;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (player == null || !player.isOnline() || !Double.isFinite(amount) || amount <= 0) return false;
        EconomyProvider provider = externalProvider;
        if (provider != null) {
            try {
                if (provider.isAvailable()) return provider.deposit(player, amount);
            } catch (RuntimeException error) {
                // No se hace fallback tras una excepción: el proveedor podría haber
                // aplicado el depósito antes de fallar y un segundo pago duplicaría dinero.
                plugin.getLogger().warning("El proveedor económico " + provider.getName()
                        + " falló; se cancela el depósito para evitar una posible duplicación: "
                        + error.getMessage());
                return false;
            }
        }
        if (economy == null || addMoney == null) return false;
        try {
            Object result = addMoney.invoke(economy, player, amount);
            if (result instanceof Number number) return number.intValue() == 1;
            // Algunas versiones de EconomyAPI devuelven void al completar la operación.
            return addMoney.getReturnType() == Void.TYPE;
        } catch (ReflectiveOperationException | RuntimeException error) {
            plugin.getLogger().warning("Error al entregar recompensa económica: " + error.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        EconomyProvider provider = externalProvider;
        if (provider != null) {
            try {
                if (provider.isAvailable()) return true;
            } catch (RuntimeException ignored) {
                // La disponibilidad del proveedor externo no debe romper comandos/eventos.
            }
        }
        return economy != null && addMoney != null;
    }
}