package com.brunojaeger.worldevents.integration.economy;

import cn.nukkit.Player;

/** Contrato para conectar proveedores económicos externos sin dependencia directa. */
public interface EconomyProvider {
    String getName();

    boolean isAvailable();

    /** @return true únicamente si el proveedor confirmó el depósito. */
    boolean deposit(Player player, double amount);
}
