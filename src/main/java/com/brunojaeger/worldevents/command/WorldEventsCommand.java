package com.brunojaeger.worldevents.command;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import com.brunojaeger.worldevents.WorldEventsNK;
import com.brunojaeger.worldevents.core.EventManager;
import com.brunojaeger.worldevents.event.WorldEvent;

import java.util.Locale;

public final class WorldEventsCommand {
    private final WorldEventsNK plugin;

    public WorldEventsCommand(WorldEventsNK plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessages().get("help"));
            return true;
        }
        EventManager manager = plugin.getEventManager();
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                sender.sendMessage(plugin.getMessages().get("event-list"));
                return true;
            }
            case "status" -> {
                WorldEvent event = manager.getActiveEvent();
                if (event == null) sender.sendMessage(plugin.getMessages().get("status-none"));
                else sender.sendMessage(plugin.getMessages().format("event-status",
                        "{event}", event.getId(), "{state}", event.getState().name()));
                return true;
            }
            case "start" -> {
                if (!sender.hasPermission("worldevents.start")) return noPermission(sender);
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessages().get("unknown-event"));
                    return true;
                }
                String requested = args[1].toLowerCase(Locale.ROOT);
                if (!requested.equals("random") && !manager.getRegistry().contains(requested)) {
                    sender.sendMessage(plugin.getMessages().get("unknown-event"));
                    return true;
                }
                boolean started = manager.start(requested);
                if (!started) {
                    sender.sendMessage(manager.hasActiveEvent()
                            ? plugin.getMessages().get("event-already-active")
                            : plugin.getMessages().get("no-safe-location"));
                }
                return true;
            }
            case "stop" -> {
                if (!sender.hasPermission("worldevents.stop")) return noPermission(sender);
                sender.sendMessage(manager.stop()
                        ? plugin.getMessages().get("event-stopped")
                        : plugin.getMessages().get("no-active-event"));
                return true;
            }
            case "claim" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessages().get("player-only"));
                    return true;
                }
                Player player = (Player) sender;
                if (manager.getActiveEvent() == null
                        || !manager.getActiveEvent().getId().equals("losttreasure")) {
                    sender.sendMessage(plugin.getMessages().get("treasure-unavailable"));
                } else if (!manager.claimTreasure(player)) {
                    sender.sendMessage(plugin.getMessages().get("treasure-too-far"));
                }
                return true;
            }
            case "stats" -> {
                if (!sender.hasPermission("worldevents.stats")) return noPermission(sender);
                Player target;
                if (args.length > 1) {
                    target = plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§cEse jugador no está conectado.");
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    sender.sendMessage("§6WorldEventsNK §7| Reclamaciones de tesoros: §e"
                            + plugin.getStatisticsManager().getTotalTreasureClaims());
                    return true;
                }
                sender.sendMessage("§6WorldEventsNK §7| §f" + target.getName()
                        + " §7ha reclamado §e" + plugin.getStatisticsManager().getTreasureClaims(target)
                        + " §7tesoros. Total global: §e"
                        + plugin.getStatisticsManager().getTotalTreasureClaims());
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
