package com.brunojaeger.worldevents.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import com.brunojaeger.worldevents.WorldEventsNK;

public final class WorldEventsListener implements Listener {
    private final WorldEventsNK plugin;

    public WorldEventsListener(WorldEventsNK plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTreasureBreak(BlockBreakEvent event) {
        if (event.getBlock() == null) return;
        if (plugin.getEventManager().isTreasureBlock(event.getBlock().getLevel(),
                event.getBlock().getFloorX(), event.getBlock().getFloorY(), event.getBlock().getFloorZ())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§eEste cofre pertenece a un evento. Usa §f/we claim §epara reclamarlo.");
        }
    }

    @EventHandler
    public void onTreasureInteract(PlayerInteractEvent event) {
        if (event.getBlock() == null) return;
        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (plugin.getEventManager().isTreasureBlock(event.getBlock().getLevel(),
                event.getBlock().getFloorX(), event.getBlock().getFloorY(), event.getBlock().getFloorZ())) {
            event.setCancelled(true);
            if (!plugin.getEventManager().claimTreasure(event.getPlayer())) {
                event.getPlayer().sendMessage("§cNo se pudo reclamar el tesoro.");
            }
        }
    }
}
