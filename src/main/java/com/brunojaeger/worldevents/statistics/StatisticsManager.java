package com.brunojaeger.worldevents.statistics;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;

public final class StatisticsManager {
    private final WorldEventsNK plugin;
    private final Config statistics;

    public StatisticsManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.statistics = new Config(new File(plugin.getDataFolder(), "statistics.yml"), Config.YAML);
    }

    public void recordTreasureClaim(Player player) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return;
        String path = "players." + player.getUniqueId();
        statistics.set(path + ".name", player.getName());
        statistics.set(path + ".treasure-claims", getTreasureClaims(player) + 1);
        statistics.set("totals.treasure-claims", getTotalTreasureClaims() + 1);
        if (!statistics.save()) {
            plugin.getLogger().warning("No se pudieron guardar las estadísticas de reclamación de " + player.getName() + ".");
        }
    }

    public int getTreasureClaims(Player player) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return 0;
        return statistics.getInt("players." + player.getUniqueId() + ".treasure-claims", 0);
    }

    public int getTotalTreasureClaims() {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return 0;
        return statistics.getInt("totals.treasure-claims", 0);
    }
}
