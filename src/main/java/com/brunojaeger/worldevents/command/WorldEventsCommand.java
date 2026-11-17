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
                if (!sender.hasPermission("worldevents.start") && !sender.hasPermission("worldevents.admin")) return noPermission(sender);
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
                if (!sender.hasPermission("worldevents.stop") && !sender.hasPermission("worldevents.admin")) return noPermission(sender);
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
                if (args.length >= 2 && args[1].equalsIgnoreCase("top")) {
                    int limit = 5;
                    if (args.length >= 3) {
                        try {
                            limit = Integer.parseInt(args[2]);
                        } catch (NumberFormatException ignored) {
                            sender.sendMessage(plugin.getMessages().get("stats-top-usage"));
                            return true;
                        }
                    }
                    limit = Math.max(1, Math.min(10, limit));
                    sender.sendMessage(plugin.getMessages().get("stats-top-title"));
                    java.util.List<String> ranking = plugin.getStatisticsManager().getTopTreasureClaims(limit);
                    if (ranking.isEmpty()) {
                        sender.sendMessage(plugin.getMessages().get("stats-top-empty"));
                    } else {
                        for (String entry : ranking) {
                            sender.sendMessage(plugin.getMessages().format("stats-top-entry", "{entry}", entry));
                        }
                    }
                } else if (args.length >= 2) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getMessages().get("player-not-found"));
                    } else {
                        sender.sendMessage(plugin.getMessages().format("stats-player",
                                "{player}", target.getName(),
                                "{count}", Integer.toString(plugin.getStatisticsManager().getTreasureClaims(target))));
                    }
                } else if (sender instanceof Player player) {
                    sender.sendMessage(plugin.getMessages().format("stats-self",
                            "{count}", Integer.toString(plugin.getStatisticsManager().getTreasureClaims(player))));
                } else {
                    sender.sendMessage(plugin.getMessages().format("stats-total",
                            "{count}", Integer.toString(plugin.getStatisticsManager().getTotalTreasureClaims())));
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
