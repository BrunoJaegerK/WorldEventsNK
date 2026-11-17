package com.brunojaeger.worldevents.reward;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardManager {
    private final EconomyManager economy;
    private final Config rewards;

    public RewardManager(WorldEventsNK plugin, EconomyManager economy) {
        this.economy = economy;
        this.rewards = new Config(new File(plugin.getDataFolder(), "rewards.yml"), Config.YAML);
    }

    public void reload() {
        rewards.reload();
    }

    public void give(String eventId, Player player) {
        if (!rewards.getBoolean("enabled", true) || !"losttreasure".equals(eventId)) return;
        String base = eventId + ".";
        int minMoney = clampReward(rewards.getInt(base + "money-min", 0));
        int maxMoney = Math.max(minMoney, clampReward(rewards.getInt(base + "money-max", minMoney)));
        if (maxMoney > 0 && economy.isAvailable()) {
            economy.deposit(player, randomInclusive(minMoney, maxMoney));
        }
        giveItem(player, Item.DIAMOND, base + "diamond-min", base + "diamond-max", 2, 6);
        giveItem(player, Item.EMERALD, base + "emerald-min", base + "emerald-max", 3, 8);
        giveConfiguredBonus(player, base);
    }

    private void giveConfiguredBonus(Player player, String base) {
        int itemId = rewards.getInt(base + "bonus-item-id", 0);
        int damage = Math.max(0, rewards.getInt(base + "bonus-item-damage", 0));
        int chance = Math.max(0, Math.min(100,
                rewards.getInt(base + "bonus-item-chance-percent", 0)));
        if (itemId <= 0 || chance == 0 || ThreadLocalRandom.current().nextInt(100) >= chance) return;

        int min = clampReward(rewards.getInt(base + "bonus-item-min", 1));
        int max = Math.max(min, clampReward(rewards.getInt(base + "bonus-item-max", min)));
        if (max <= 0) return;
        Item bonus = Item.get(itemId, damage, randomInclusive(min, max));
        for (Item leftover : player.getInventory().addItem(bonus)) {
            player.getLevel().dropItem(player, leftover);
        }
    }

    private void giveItem(Player player, int itemId, String minPath, String maxPath, int defaultMin, int defaultMax) {
        int min = clampReward(rewards.getInt(minPath, defaultMin));
        int max = Math.max(min, clampReward(rewards.getInt(maxPath, defaultMax)));
        if (max > 0) {
            Item reward = Item.get(itemId, 0, randomInclusive(min, max));
            for (Item leftover : player.getInventory().addItem(reward)) {
                player.getLevel().dropItem(player, leftover);
            }
        }
    }

    private int clampReward(int amount) {
        return Math.max(0, Math.min(1_000_000, amount));
    }

    private int randomInclusive(int minimum, int maximum) {
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }
}
