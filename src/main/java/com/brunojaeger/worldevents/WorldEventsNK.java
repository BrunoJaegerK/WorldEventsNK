package com.brunojaeger.worldevents;

import cn.nukkit.plugin.PluginBase;
import com.brunojaeger.worldevents.command.WorldEventsCommand;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.core.EventScheduler;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;
import com.brunojaeger.worldevents.listener.WorldEventsListener;
import com.brunojaeger.worldevents.storage.TreasureRecoveryStorage;
import com.brunojaeger.worldevents.util.MessageUtils;

public final class WorldEventsNK extends PluginBase {
    private EventManager eventManager;
    private EventScheduler eventScheduler;
    private ProtectionManager protectionManager;
    private MessageUtils messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("events.yml", false);
        saveResource("rewards.yml", false);
        saveResource("integrations.yml", false);
        saveResource("lang/es_ES.yml", false);

        TreasureRecoveryStorage.recover(this);
        messages = new MessageUtils(this);
        protectionManager = new ProtectionManager(this);
        eventManager = new EventManager(this, protectionManager);
        getServer().getPluginManager().registerEvents(new WorldEventsListener(this), this);
        eventScheduler = new EventScheduler(this, eventManager);
        eventScheduler.start();

        getLogger().info("WorldEventsNK habilitado. Eventos: MeteorFall y LostTreasure.");
        getLogger().info("Protección genérica disponible; EconomyAPI es opcional.");
    }

    @Override
    public void onDisable() {
        if (eventScheduler != null) eventScheduler.stop();
        if (eventManager != null) eventManager.shutdown();
        getLogger().info("WorldEventsNK deshabilitado.");
    }

    @Override
    public boolean onCommand(cn.nukkit.command.CommandSender sender, cn.nukkit.command.Command command,
                             String label, String[] args) {
        return new WorldEventsCommand(this).execute(sender, args);
    }

    public EventManager getEventManager() { return eventManager; }
    public ProtectionManager getProtectionManager() { return protectionManager; }
    public MessageUtils getMessages() { return messages; }
}
