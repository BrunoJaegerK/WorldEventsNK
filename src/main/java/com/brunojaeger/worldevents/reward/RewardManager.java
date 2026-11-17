package com.brunojaeger.worldevents.reward;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;

import java.util.concurrent.ThreadLocalRandom;

public final class RewardManager {
    private final WorldEventsNK plugin;
    private final EconomyManager economy;

    public RewardManager(WorldEventsNK plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void give(String eventId, Player player) {
        String base = eventId + ".";
        int minMoney = Math.max(0, plugin.getConfig().getInt("rewards." + base + "money-min", 0));
        int maxMoney = Math.max(minMoney, plugin.getConfig().getInt("rewards." + base + "money-max", minMoney));
        if (maxMoney > 0 && economy.isAvailable()) {
            economy.deposit(player, ThreadLocalRandom.current().nextInt(minMoney, maxMoney + 1));
        }

        if ("meteorfall".equals(eventId)) {
            giveItem(player, Item.DIAMOND, "rewards.meteorfall.diamond-min", "rewards.meteorfall.diamond-max", 1, 3);
        } else if ("losttreasure".equals(eventId)) {
            giveItem(player, Item.DIAMOND, "rewards.losttreasure.diamond-min", "rewards.losttreasure.diamond-max", 2, 6);
            giveItem(player, Item.EMERALD, "rewards.losttreasure.emerald-min", "rewards.losttreasure.emerald-max", 3, 8);
        }
    }

    private void giveItem(Player player, int itemId, String minPath, String maxPath, int defaultMin, int defaultMax) {
        int min = Math.max(0, plugin.getConfig().getInt(minPath, defaultMin));
        int max = Math.max(min, plugin.getConfig().getInt(maxPath, defaultMax));
        if (max > 0) player.getInventory().addItem(Item.get(itemId, 0,
                ThreadLocalRandom.current().nextInt(min, max + 1)));
    }
}
