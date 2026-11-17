package com.brunojaeger.worldevents.statistics;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;

public final class StatisticsManager {
    private final Config statistics;

    public StatisticsManager(WorldEventsNK plugin) {
        this.statistics = new Config(new File(plugin.getDataFolder(), "statistics.yml"), Config.YAML);
    }

    public void recordTreasureClaim(Player player) {
        String path = "players." + player.getUniqueId();
        statistics.set(path + ".name", player.getName());
        statistics.set(path + ".treasure-claims", getTreasureClaims(player) + 1);
        statistics.set("totals.treasure-claims", getTotalTreasureClaims() + 1);
        statistics.save();
    }

    public int getTreasureClaims(Player player) {
        return statistics.getInt("players." + player.getUniqueId() + ".treasure-claims", 0);
    }

    public int getTotalTreasureClaims() {
        return statistics.getInt("totals.treasure-claims", 0);
    }
}
