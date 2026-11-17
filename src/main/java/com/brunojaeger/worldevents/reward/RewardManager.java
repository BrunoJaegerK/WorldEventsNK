package com.brunojaeger.worldevents.reward;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardManager {
    private final WorldEventsNK plugin;
    private final EconomyManager economy;
    private final Config rewards;

    public RewardManager(WorldEventsNK plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.rewards = new Config(new File(plugin.getDataFolder(), "rewards.yml"), Config.YAML);
    }

    public void reload() { rewards.reload(); }

    public void give(String eventId, Player player) {
        if (player == null || !player.isOnline()) return;
        String base = eventId + ".";
        int minMoney = Math.max(0, rewards.getInt(base + "money-min", 0));
        int maxMoney = Math.max(minMoney, rewards.getInt(base + "money-max", minMoney));
        if (maxMoney > 0 && economy.isAvailable()) {
            economy.deposit(player, ThreadLocalRandom.current().nextInt(minMoney, maxMoney + 1));
        }
        if ("meteorfall".equals(eventId)) {
            giveItem(player, Item.DIAMOND, base + "diamond-min", base + "diamond-max", 1, 3);
        } else if ("losttreasure".equals(eventId)) {
            giveItem(player, Item.DIAMOND, base + "diamond-min", base + "diamond-max", 2, 6);
            giveItem(player, Item.EMERALD, base + "emerald-min", base + "emerald-max", 3, 8);
        }
    }

    private void giveItem(Player player, int itemId, String minPath, String maxPath, int defaultMin, int defaultMax) {
        int min = Math.max(0, rewards.getInt(minPath, defaultMin));
        int max = Math.max(min, rewards.getInt(maxPath, defaultMax));
        if (max <= 0) return;
        Item item = Item.get(itemId, 0, ThreadLocalRandom.current().nextInt(min, max + 1));
        if (item == null || item.getId() == Item.AIR) return;
        Item[] leftovers = player.getInventory().addItem(item);
        for (Item leftover : leftovers) {
            if (leftover != null && leftover.getId() != Item.AIR) {
                player.getLevel().dropItem(player.getPosition(), leftover);
            }
        }
    }
}
