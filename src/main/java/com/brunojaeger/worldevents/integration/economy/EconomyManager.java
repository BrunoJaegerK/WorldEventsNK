package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.lang.reflect.Method;

public final class EconomyManager {
    private final WorldEventsNK plugin;
    private Object economy;
    private Method addMoney;

    public EconomyManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        connect();
    }

    public void reload() {
        economy = null;
        addMoney = null;
        connect();
    }

    private void connect() {
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) return;
        try {
            Plugin economyPlugin = plugin.getServer().getPluginManager().getPlugin(
                    plugin.getConfig().getString("economy.provider", "EconomyAPI"));
            if (economyPlugin == null || !economyPlugin.isEnabled()) {
                plugin.getLogger().info("EconomyAPI no está disponible; las recompensas monetarias se omiten.");
                return;
            }
            Class<?> type = Class.forName("me.onebone.economyapi.EconomyAPI", true,
                    economyPlugin.getClass().getClassLoader());
            Object instance = type.getMethod("getInstance").invoke(null);
            Method method = type.getMethod("addMoney", Player.class, double.class);
            this.economy = instance;
            this.addMoney = method;
            plugin.getLogger().info("Conexión con EconomyAPI establecida.");
        } catch (ReflectiveOperationException | LinkageError error) {
            plugin.getLogger().warning("No se pudo conectar con EconomyAPI: " + error.getMessage());
            this.economy = null;
            this.addMoney = null;
        }
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null || addMoney == null || amount <= 0) return false;
        try {
            Object result = addMoney.invoke(economy, player, amount);
            return result instanceof Number && ((Number) result).intValue() == 1;
        } catch (ReflectiveOperationException error) {
            plugin.getLogger().warning("Error al entregar recompensa económica: " + error.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        return economy != null && addMoney != null;
    }
}
