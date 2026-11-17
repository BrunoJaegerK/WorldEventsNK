package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;

public interface EconomyProvider {
    String getName();
    boolean deposit(Player player, double amount);
}
