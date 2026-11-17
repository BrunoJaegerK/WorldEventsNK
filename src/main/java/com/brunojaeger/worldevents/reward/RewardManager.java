package com.brunojaeger.worldevents.reward;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardManager {
    private final Config config;
    private final EconomyManager economy;

    public RewardManager(Config config, EconomyManager economy) {
        this.config = config;
        this.economy = economy;
    }

    public void give(String eventId, Player player) {
        if (player == null || !player.isOnline()) return;
        String base = eventId + ".money.";
        if (config.getBoolean(base + "enabled", false) && economy.isAvailable()) {
            int min = Math.max(0, config.getInt(base + "min", 0));
            int max = Math.max(min, config.getInt(base + "max", min));
            economy.deposit(player, ThreadLocalRandom.current().nextInt(min, max + 1));
        }

        Object raw = config.get(eventId + ".items");
        if (!(raw instanceof List<?>)) return;
        for (Object entry : (List<?>) raw) {
            if (!(entry instanceof Map<?, ?>)) continue;
            Map<?, ?> values = (Map<?, ?>) entry;
            int id = number(values.get("id"), -1);
            int min = Math.max(1, number(values.get("min-amount"), 1));
            int max = Math.max(min, number(values.get("max-amount"), min));
            int chance = Math.max(0, Math.min(100, number(values.get("chance"), 100)));
            if (id < 0 || ThreadLocalRandom.current().nextInt(100) >= chance) continue;
            Item reward = Item.get(id, 0, ThreadLocalRandom.current().nextInt(min, max + 1));
            if (reward == null) continue;
            Item[] leftovers = player.getInventory().addItem(reward);
            for (Item leftover : leftovers) {
                if (leftover != null && leftover.getId() != 0) player.getLevel().dropItem(player.getPosition(), leftover);
            }
        }
    }

    private int number(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }
}
