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
        recordEventClaim(player, "treasure-claims");
    }

    public void recordMeteorCoreClaim(Player player) {
        recordEventClaim(player, "meteor-claims");
    }

    private void recordEventClaim(Player player, String counter) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true) || player == null) return;
        String path = "players." + player.getUniqueId();
        statistics.set(path + ".name", player.getName());
        statistics.set(path + "." + counter, getEventClaims(player, counter) + 1);
        statistics.set("totals." + counter, getTotalEventClaims(counter) + 1);
        if (!statistics.save()) {
            plugin.getLogger().warning("No se pudieron guardar las estadísticas (" + counter + ") de " + player.getName() + ".");
        }
    }

    private int getEventClaims(Player player, String counter) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true) || player == null) return 0;
        return Math.max(0, statistics.getInt("players." + player.getUniqueId() + "." + counter, 0));
    }

    private int getTotalEventClaims(String counter) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return 0;
        return Math.max(0, statistics.getInt("totals." + counter, 0));
    }

    public int getTreasureClaims(Player player) {
        return getEventClaims(player, "treasure-claims");
    }

    public int getMeteorClaims(Player player) {
        return getEventClaims(player, "meteor-claims");
    }

    public int getTotalTreasureClaims() {
        return getTotalEventClaims("treasure-claims");
    }

    public int getTotalMeteorClaims() {
        return getTotalEventClaims("meteor-claims");
    }

    /**
     * Busca las reclamaciones por nombre guardado, incluso si el jugador está desconectado.
     * Devuelve null cuando no existe un registro con ese nombre.
     */
    public Integer getTreasureClaimsByName(String playerName) {
        if (!plugin.getConfig().getBoolean("statistics.enabled", true)) return 0;
        ConfigSection players = statistics.getSection("players");
        for (Map.Entry<String, Object> entry : players.entrySet()) {
            if (!(entry.getValue() instanceof ConfigSection data)) continue;
            if (data.getString("name", "").equalsIgnoreCase(playerName)) {
                return Math.max(0, data.getInt("treasure-claims", 0));
            }
        }
        return null;
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
