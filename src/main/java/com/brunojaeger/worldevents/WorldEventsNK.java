package com.brunojaeger.worldevents;

import cn.nukkit.plugin.PluginBase;
import com.brunojaeger.worldevents.command.WorldEventsCommand;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.core.EventScheduler;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;
import com.brunojaeger.worldevents.history.EventHistoryManager;
import com.brunojaeger.worldevents.listener.WorldEventsListener;
import com.brunojaeger.worldevents.statistics.StatisticsManager;
import com.brunojaeger.worldevents.storage.EventStorage;
import com.brunojaeger.worldevents.util.MessageUtils;

public final class WorldEventsNK extends PluginBase {
    private EventManager eventManager;
    private EventScheduler eventScheduler;
    private ProtectionManager protectionManager;
    private MessageUtils messages;
    private EventStorage eventStorage;
    private StatisticsManager statisticsManager;
    private EventHistoryManager historyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("events.yml", false);
        saveResource("rewards.yml", false);
        saveResource("integrations.yml", false);
        saveResource("lang/es_ES.yml", false);

        messages = new MessageUtils(this);
        protectionManager = new ProtectionManager(this);
        eventStorage = new EventStorage(this);
        eventStorage.recover();
        statisticsManager = new StatisticsManager(this);
        historyManager = new EventHistoryManager(this);
        eventManager = new EventManager(this, protectionManager, eventStorage, statisticsManager, historyManager);
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
    public EventStorage getEventStorage() { return eventStorage; }
    public StatisticsManager getStatisticsManager() { return statisticsManager; }
    public EventHistoryManager getHistoryManager() { return historyManager; }
}
