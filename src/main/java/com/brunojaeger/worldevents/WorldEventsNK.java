package com.brunojaeger.worldevents;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.command.WorldEventsCommand;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.core.EventScheduler;
import com.brunojaeger.worldevents.integration.economy.EconomyManager;
import com.brunojaeger.worldevents.integration.protection.ProtectionManager;
import com.brunojaeger.worldevents.listener.PlayerListener;
import com.brunojaeger.worldevents.reward.RewardManager;
import com.brunojaeger.worldevents.world.SafeLocationFinder;

import java.io.File;

public final class WorldEventsNK extends PluginBase {
    private static WorldEventsNK instance;
    private Config eventsConfig, rewardsConfig, languageConfig;
    private ProtectionManager protectionManager;
    private EconomyManager economyManager;
    private RewardManager rewardManager;
    private EventManager eventManager;
    private EventScheduler scheduler;

    public static WorldEventsNK getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().error("No se pudo crear el directorio de datos.");
            return;
        }
        saveDefaultConfig();
        ensureResource("events.yml");
        ensureResource("rewards.yml");
        ensureResource("integrations.yml");
        ensureResource("lang/es_ES.yml");
        reloadPluginConfiguration();

        protectionManager = new ProtectionManager(this);
        economyManager = new EconomyManager(this);
        economyManager.hook();
        rewardManager = new RewardManager(this, rewardsConfig, economyManager);
        SafeLocationFinder finder = new SafeLocationFinder(this, protectionManager);
        eventManager = new EventManager(this, finder);
        scheduler = new EventScheduler(this, eventManager);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getCommandMap().register("worldeventsnk", new WorldEventsCommand(this));
        scheduler.start();
        getLogger().info("WorldEventsNK habilitado. Eventos registrados: " + eventManager.getEvents().size());
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        if (eventManager != null) eventManager.stopActive();
        if (instance == this) instance = null;
    }

    private void ensureResource(String path) {
        if (!new File(getDataFolder(), path).exists()) saveResource(path, false);
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        eventsConfig = new Config(new File(getDataFolder(), "events.yml"), Config.YAML);
        rewardsConfig = new Config(new File(getDataFolder(), "rewards.yml"), Config.YAML);
        languageConfig = new Config(new File(getDataFolder(), "lang/es_ES.yml"), Config.YAML);
        if (scheduler != null) scheduler.refreshSchedule();
    }

    public String message(String key) {
        String value = languageConfig == null ? key : languageConfig.getString(key, key);
        return "§8[§6WorldEvents§8] §r" + value.replace("&", "§");
    }

    public void broadcast(String text) { Server.getInstance().broadcastMessage(text); }

    public Config getEventsConfig() { return eventsConfig; }
    public Config getRewardsConfig() { return rewardsConfig; }
    public ProtectionManager getProtectionManager() { return protectionManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public EventManager getEventManager() { return eventManager; }
}
