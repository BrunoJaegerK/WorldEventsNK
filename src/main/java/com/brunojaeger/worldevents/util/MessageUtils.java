package com.brunojaeger.worldevents.util;

import cn.nukkit.utils.Config;
import com.brunojaeger.worldevents.WorldEventsNK;

import java.io.File;

public final class MessageUtils {
    private final WorldEventsNK plugin;
    private Config language;

    public MessageUtils(WorldEventsNK plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String languageName = plugin.getConfig().getString("settings.language", "es_ES");
        File file = new File(plugin.getDataFolder(), "lang/" + languageName + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/es_ES.yml", false);
            file = new File(plugin.getDataFolder(), "lang/es_ES.yml");
        }
        language = new Config(file, Config.YAML);
    }

    public String get(String key) {
        String prefix = language.getString("prefix", "&8[&6WorldEvents&8] ");
        return color(prefix + language.getString(key, key));
    }

    public String color(String message) {
        return message == null ? "" : message.replace('&', '§');
    }
}
