package com.brunojaeger.worldevents.command;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.event.WorldEvent;
import com.brunojaeger.worldevents.event.treasure.LostTreasureEvent;

public final class WorldEventsCommand {
    private final WorldEventsNK plugin;

    public WorldEventsCommand(WorldEventsNK plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6WorldEventsNK §7| §f/we list, status, start, stop, claim, reload");
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
                if (event == null) sender.sendMessage("§7No hay eventos activos.");
                else sender.sendMessage("§aEvento activo: §e" + event.getId() + " §7(" + event.getState() + ")");
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("worldevents.start")) return noPermission(sender);
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /we start <meteorfall|losttreasure|random>");
                    return true;
                }
                boolean started = manager.start(args[1]);
                sender.sendMessage(started ? "§aEvento iniciado." : "§cNo se pudo iniciar. Comprueba que no exista otro evento y que haya una ubicación segura.");
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("worldevents.stop")) return noPermission(sender);
                sender.sendMessage(manager.stop() ? "§aEvento detenido." : "§cNo hay evento activo.");
                return true;
            }
            case "claim" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cEste comando solo puede ejecutarlo un jugador.");
                    return true;
                }
                Player player = (Player) sender;
                if (manager.claimTreasure(player)) return true;
                sender.sendMessage("§cNo puedes reclamar el tesoro: no hay uno disponible o estás demasiado lejos.");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("worldevents.admin")) return noPermission(sender);
                manager.reload();
                sender.sendMessage("§aConfiguración recargada.");
                return true;
            }
            default -> {
                sender.sendMessage("§e/we list, /we status, /we start <evento>, /we stop, /we claim, /we reload");
                return true;
            }
        }
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage("§cNo tienes permiso para ejecutar este comando.");
        return true;
    }
}
