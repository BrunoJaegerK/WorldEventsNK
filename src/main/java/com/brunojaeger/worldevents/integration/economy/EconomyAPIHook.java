package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.lang.reflect.Method;

public final class EconomyAPIHook implements EconomyProvider {
    private final WorldEventsNK plugin;
    private Object api;
    private Method addMoney;

    public EconomyAPIHook(WorldEventsNK plugin) { this.plugin = plugin; }

    public boolean connect() {
        try {
            Plugin dependency = plugin.getServer().getPluginManager().getPlugin("EconomyAPI");
            if (dependency == null || !dependency.isEnabled()) return false;
            Class<?> type = Class.forName("me.onebone.economyapi.EconomyAPI", true, dependency.getClass().getClassLoader());
            api = type.getMethod("getInstance").invoke(null);
            addMoney = type.getMethod("addMoney", Player.class, double.class);
            return api != null;
        } catch (Throwable error) {
            plugin.getLogger().warning("EconomyAPI hook no disponible: " + error.getMessage());
            api = null;
            addMoney = null;
            return false;
        }
    }

    @Override public String getName() { return "EconomyAPI"; }

    @Override
    public boolean deposit(Player player, double amount) {
        if (api == null || addMoney == null || player == null || amount <= 0) return false;
        try {
            Object result = addMoney.invoke(api, player, amount);
            return result instanceof Number && ((Number) result).intValue() == 1;
        } catch (Throwable error) {
            plugin.getLogger().warning("Error entregando dinero a " + player.getName() + ": " + error.getMessage());
            return false;
        }
    }
}
