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

    public EconomyManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.integrations = new Config(new File(plugin.getDataFolder(), "integrations.yml"), Config.YAML);
        connect();
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
        if (economy == null || addMoney == null || player == null || !player.isOnline()
                || !Double.isFinite(amount) || amount <= 0) return false;
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
        return economy != null && addMoney != null;
    }
}