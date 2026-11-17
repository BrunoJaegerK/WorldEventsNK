package com.brunojaeger.worldevents.history;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.world.EventLocation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Historial persistente y acotado de ejecuciones de eventos. */
public final class EventHistoryManager {
    private final WorldEventsNK plugin;
    private final Config history;

    public EventHistoryManager(WorldEventsNK plugin) {
        this.plugin = plugin;
        this.history = new Config(new File(plugin.getDataFolder(), "history.yml"), Config.YAML);
    }

    public void record(String eventId, String result, EventLocation location) {
        if (!plugin.getConfig().getBoolean("history.enabled", true)) return;
        long timestamp = System.currentTimeMillis();
        String key = timestamp + "-" + UUID.randomUUID().toString().substring(0, 8);
        String path = "entries." + key;
        history.set(path + ".timestamp", timestamp);
        history.set(path + ".event", eventId);
        history.set(path + ".result", result);
        if (location != null && location.getLevel() != null) {
            history.set(path + ".world", location.getLevel().getName());
            history.set(path + ".x", location.getX());
            history.set(path + ".y", location.getY());
            history.set(path + ".z", location.getZ());
        }
        if (!history.save()) {
            plugin.getLogger().warning("No se pudo guardar el historial del evento " + eventId + ".");
            return;
        }
        trim();
    }

    private void trim() {
        int maximum = Math.max(10, Math.min(1000,
                plugin.getConfig().getInt("history.maximum-entries", 100)));
        ConfigSection entries = history.getSection("entries");
        if (entries.size() <= maximum) return;

        List<Map.Entry<String, Long>> ordered = new ArrayList<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (!(entry.getValue() instanceof ConfigSection data)) continue;
            ordered.add(Map.entry(entry.getKey(), data.getLong("timestamp", 0L)));
        }
        ordered.sort(Map.Entry.comparingByValue());
        int removeCount = ordered.size() - maximum;
        for (int i = 0; i < removeCount; i++) {
            history.remove("entries." + ordered.get(i).getKey());
        }
        if (!history.save()) plugin.getLogger().warning("No se pudo limitar el tamaño de history.yml.");
    }

    public List<String> getRecent(int requestedLimit) {
        List<Map.Entry<Long, String>> ordered = new ArrayList<>();
        ConfigSection entries = history.getSection("entries");
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (!(entry.getValue() instanceof ConfigSection data)) continue;
            long timestamp = data.getLong("timestamp", 0L);
            String event = data.getString("event", "unknown");
            String result = data.getString("result", "UNKNOWN");
            String world = data.getString("world", "-");
            String coordinates = data.containsKey("x")
                    ? data.getInt("x") + ", " + data.getInt("y") + ", " + data.getInt("z") : "-";
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                    .format(new Date(timestamp));
            ordered.add(Map.entry(timestamp, date + " | " + event + " | " + result
                    + " | " + world + " (" + coordinates + ")"));
        }
        ordered.sort(Map.Entry.<Long, String>comparingByKey(Comparator.reverseOrder()));
        int limit = Math.max(1, Math.min(10, requestedLimit));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, ordered.size()); i++) {
            result.add(ordered.get(i).getValue());
        }
        return result;
    }
}