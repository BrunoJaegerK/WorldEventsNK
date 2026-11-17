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
