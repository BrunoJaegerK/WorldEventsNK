package com.brunojaeger.worldevents.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.Player;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;

public final class WorldEventsCommand extends Command {
    private final WorldEventsNK plugin;

    public WorldEventsCommand(WorldEventsNK plugin) {
        super("worldevents", "WorldEventsNK commands", "/we <list|status|start|stop|claim|reload>", new String[]{"we"});
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6WorldEventsNK §7- §f/we list, status, start <meteorfall|losttreasure|random>, stop, claim, reload");
            return true;
        }
        EventManager manager = plugin.getEventManager();
        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("§6Eventos disponibles:");
                for (WorldEvent event : manager.getEvents()) sender.sendMessage(" §e- " + event.getId() + " §7(" + event.getState() + ")");
                return true;
            case "status":
                WorldEvent active = manager.getActiveEvent();
                sender.sendMessage(active == null ? plugin.message("no-active-event")
                        : plugin.message("event-active").replace("{event}", active.getId()));
                return true;
            case "start":
                if (!admin(sender)) return true;
                if (args.length < 2) { sender.sendMessage("§cUso: /we start <meteorfall|losttreasure|random>"); return true; }
                boolean started = args[1].equalsIgnoreCase("random") ? manager.startRandom() : manager.start(args[1]);
                sender.sendMessage(started ? plugin.message("event-started").replace("{event}", args[1]) : plugin.message("event-cancelled"));
                return true;
            case "stop":
                if (!admin(sender)) return true;
                if (!manager.isActive()) { sender.sendMessage(plugin.message("no-active-event")); return true; }
                manager.stopActive();
                return true;
            case "claim":
                if (!(sender instanceof Player)) { sender.sendMessage("Este comando solo puede usarlo un jugador."); return true; }
                LostTreasureEvent treasure = (LostTreasureEvent) manager.getEvent("losttreasure");
                if (treasure == null || !treasure.claim((Player) sender)) sender.sendMessage("§cNo tienes un tesoro reclamable cerca.");
                return true;
            case "reload":
                if (!admin(sender)) return true;
                plugin.reloadPluginConfiguration();
                sender.sendMessage(plugin.message("plugin-reloaded"));
                return true;
            default:
                sender.sendMessage(plugin.message("unknown-command"));
                return true;
        }
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("worldevents.admin")) return true;
        sender.sendMessage(plugin.message("no-permission"));
        return false;
    }
}
