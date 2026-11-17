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
        recordEventClaim(player, "losttreasure");
    }

    public void recordMeteorCoreClaim(Player player) {
        recordEventClaim(player, "meteorfall");
    }

    private void recordEventClaim(Player player, String eventId) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true) || player == null) return;
        String path = "players." + player.getUniqueId();
        statistics.set(path + ".name", player.getName());
        statistics.set(path + "." + eventId + "-claims", getEventClaims(player, eventId) + 1);
        statistics.set("totals." + eventId + "-claims", getTotalEventClaims(eventId) + 1);
        statistics.save();
    }

    public int getTreasureClaims(Player player) { return getEventClaims(player, "losttreasure"); }
    public int getMeteorClaims(Player player) { return getEventClaims(player, "meteorfall"); }

    public int getEventClaims(Player player, String eventId) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true) || player == null) return 0;
        return statistics.getInt("players." + player.getUniqueId() + "." + eventId + "-claims", 0);
    }

    public int getTotalTreasureClaims() { return getTotalEventClaims("losttreasure"); }
    public int getTotalMeteorClaims() { return getTotalEventClaims("meteorfall"); }

    public int getTotalEventClaims(String eventId) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return 0;
        return statistics.getInt("totals." + eventId + "-claims", 0);
    }
}
