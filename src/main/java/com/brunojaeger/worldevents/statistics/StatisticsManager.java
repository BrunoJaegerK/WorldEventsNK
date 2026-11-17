package com.brunojaeger.worldevents.statistics;

import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;
import java.util.Locale;

public final class StatisticsManager {
    private final WorldEventsNK plugin;
    private final Config data;

    public StatisticsManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "statistics.yml");
        this.data = new Config(file, Config.YAML);
        if (!file.exists()) {
            data.set("schema-version", 1);
            data.set("events.started", 0);
            data.set("events.completed", 0);
            data.set("events.failed", 0);
            data.set("events.stopped", 0);
            data.save();
        }
    }

    public synchronized void recordStart(String eventId) {
        if (!enabled()) return;
        increment("events.started");
        increment("events." + eventId + ".started");
        save();
    }

    public synchronized void recordComplete(String eventId) {
        if (!enabled()) return;
        increment("events.completed");
        increment("events." + eventId + ".completed");
        save();
    }

    public synchronized void recordFailure(String eventId) {
        if (!enabled()) return;
        increment("events.failed");
        increment("events." + eventId + ".failed");
        save();
    }

    public synchronized void recordStopped(String eventId) {
        if (!enabled()) return;
        increment("events.stopped");
        increment("events." + eventId + ".stopped");
        save();
    }

    public synchronized void recordClaim(String eventId, String playerName) {
        if (!enabled() || playerName == null || playerName.isBlank()) return;
        String key = playerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        increment("players." + key + ".claims");
        increment("players." + key + ".events." + eventId);
        data.set("players." + key + ".last-name", playerName);
        save();
    }

    public String getGlobalSummary() {
        return "§6WorldEventsNK estadísticas §7| Iniciados: §f" + data.getInt("events.started", 0)
                + " §7| Completados: §a" + data.getInt("events.completed", 0)
                + " §7| Fallidos: §c" + data.getInt("events.failed", 0)
                + " §7| Detenidos: §e" + data.getInt("events.stopped", 0);
    }

    public String getPlayerSummary(String playerName) {
        String key = playerName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return "§6Estadísticas de §e" + playerName + " §7| Recompensas reclamadas: §f"
                + data.getInt("players." + key + ".claims", 0)
                + " §7| MeteorFall: §f" + data.getInt("players." + key + ".events.meteorfall", 0)
                + " §7| LostTreasure: §f" + data.getInt("players." + key + ".events.losttreasure", 0);
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("statistics.enabled", true);
    }

    private void increment(String path) {
        data.set(path, data.getInt(path, 0) + 1);
    }

    private void save() {
        if (!data.save()) plugin.getLogger().warning("No se pudieron guardar las estadísticas de WorldEventsNK.");
    }
}
