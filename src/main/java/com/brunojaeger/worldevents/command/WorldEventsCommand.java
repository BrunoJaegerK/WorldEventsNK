package com.brunojaeger.worldevents.command;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.event.WorldEvent;

public final class WorldEventsCommand {
    private final WorldEventsNK plugin;

    public WorldEventsCommand(WorldEventsNK plugin) { this.plugin = plugin; }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessages().get("help"));
            return true;
        }
        EventManager manager = plugin.getEventManager();
        switch (args[0].toLowerCase()) {
            case "list" -> {
                sender.sendMessage("§6Eventos disponibles: §fmeteorfall, losttreasure");
                return true;
            }
            case "status" -> {
                WorldEvent event = manager.getActiveEvent();
                if (event == null) sender.sendMessage(plugin.getMessages().get("status-none"));
                else sender.sendMessage("§aEvento activo: §e" + event.getId() + " §7(" + event.getState() + ")");
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("worldevents.start") && !sender.hasPermission("worldevents.admin")) return noPermission(sender);
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /we start <meteorfall|losttreasure|random>");
                    return true;
                }
                boolean started = manager.start(args[1]);
                sender.sendMessage(started ? "§aEvento iniciado." : plugin.getMessages().get("no-safe-location"));
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("worldevents.stop") && !sender.hasPermission("worldevents.admin")) return noPermission(sender);
                sender.sendMessage(manager.stop() ? plugin.getMessages().get("event-stopped") : plugin.getMessages().get("no-active-event"));
                return true;
            }
            case "claim" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessages().get("player-only"));
                    return true;
                }
                Player player = (Player) sender;
                if (manager.claimTreasure(player)) return true;
                sender.sendMessage(plugin.getMessages().get("treasure-too-far"));
                return true;
            }
            case "stats" -> {
                if (args.length > 1) {
                    if (!sender.hasPermission("worldevents.admin")) return noPermission(sender);
                    sender.sendMessage(manager.getStatistics().getPlayerSummary(args[1]));
                } else if (sender instanceof Player) {
                    sender.sendMessage(manager.getStatistics().getPlayerSummary(sender.getName()));
                } else {
                    sender.sendMessage(manager.getStatistics().getGlobalSummary());
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("worldevents.admin")) return noPermission(sender);
                manager.reload();
                sender.sendMessage(plugin.getMessages().get("reload-success"));
                return true;
            }
            default -> {
                sender.sendMessage(plugin.getMessages().get("help"));
                return true;
            }
        }
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().get("no-permission"));
        return true;
    }
}
