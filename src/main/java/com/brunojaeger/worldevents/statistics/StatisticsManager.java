package com.brunojaeger.worldevents.statistics;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    /**
     * Devuelve las mejores reclamaciones guardadas, incluyendo jugadores desconectados.
     * Los registros sin reclamaciones válidas se omiten.
     */
    public List<String> getTopTreasureClaims(int requestedLimit) {
        List<Map.Entry<String, Integer>> ranking = new ArrayList<>();
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return new ArrayList<>();

        ConfigSection players = statistics.getSection("players");
        for (Map.Entry<String, Object> entry : players.entrySet()) {
            if (!(entry.getValue() instanceof ConfigSection data)) continue;
            int claims = data.getInt("treasure-claims", 0);
            if (claims <= 0) continue;
            String name = data.getString("name", entry.getKey());
            ranking.add(Map.entry(name, claims));
        }

        ranking.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)));
        int limit = Math.max(1, Math.min(10, requestedLimit));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, ranking.size()); i++) {
            Map.Entry<String, Integer> entry = ranking.get(i);
            result.add((i + 1) + ". " + entry.getKey() + " - " + entry.getValue());
        }
        return result;
    }
}
