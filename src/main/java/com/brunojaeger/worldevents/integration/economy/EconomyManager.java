package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;
import com.brunojaeger.worldevents.WorldEventsNK;

public final class EconomyManager {
    private final WorldEventsNK plugin;
    private EconomyProvider provider;

    public EconomyManager(WorldEventsNK plugin) { this.plugin = plugin; }

    public void hook() {
        if (!plugin.getConfig().getBoolean("economy.enabled", true)) return;
        EconomyAPIHook hook = new EconomyAPIHook(plugin);
        if (hook.connect()) {
            provider = hook;
            plugin.getLogger().info("Conexión con EconomyAPI establecida correctamente.");
        } else {
            plugin.getLogger().warning("EconomyAPI no está disponible; los eventos continuarán sin recompensas monetarias.");
        }
    }

    public boolean isAvailable() { return provider != null; }
    public boolean deposit(Player player, double amount) {
        return provider != null && amount > 0 && provider.deposit(player, amount);
    }
}
