package com.brunojaeger.worldevents.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.event.meteor.MeteorFallEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;

public final class PlayerListener implements Listener {
    private final WorldEventsNK plugin;
    public PlayerListener(WorldEventsNK plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getBlock() == null) return;
        MeteorFallEvent meteor = (MeteorFallEvent) plugin.getEventManager().getEvent("meteorfall");
        if (meteor != null && meteor.handleCoreInteract(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        LostTreasureEvent treasure = (LostTreasureEvent) plugin.getEventManager().getEvent("losttreasure");
        if (treasure != null && treasure.handleInteract(event.getPlayer(), event.getBlock())) event.setCancelled(true);
    }
}
