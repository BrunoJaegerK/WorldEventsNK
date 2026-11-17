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
                sender.sendMessage(plugin.getMessages().get("event-list-header"));
                for (String id : manager.getRegistry().getFactories().keySet()) {
                    String status = plugin.getMessages().get(manager.isEventEnabled(id)
                            ? "event-enabled" : "event-disabled");
                    sender.sendMessage(plugin.getMessages().format("event-list-entry",
                            "{event}", id, "{status}", status,
                            "{weight}", Integer.toString(manager.getEventWeight(id)),
                            "{cooldown}", Integer.toString(manager.getCooldownRemainingSeconds(id))));
                }
                return true;
            }
            case "history" -> {
                if (!sender.hasPermission("worldevents.stats")) return noPermission(sender);
                int limit = 5;
                if (args.length >= 2) {
                    try {
                        limit = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {
                        sender.sendMessage(plugin.getMessages().get("history-usage"));
                        return true;
                    }
                }
                if (limit < 1 || limit > 10) {
                    sender.sendMessage(plugin.getMessages().get("history-usage"));
                    return true;
                }
                sender.sendMessage(plugin.getMessages().get("history-title"));
                java.util.List<String> entries = plugin.getHistoryManager().getRecent(limit);
                if (entries.isEmpty()) sender.sendMessage(plugin.getMessages().get("history-empty"));
                else for (String entry : entries) sender.sendMessage(plugin.getMessages().format("history-entry", "{entry}", entry));
                return true;
            }
            case "status" -> {
                WorldEvent event = manager.getActiveEvent();
                if (event == null) {
                    sender.sendMessage(plugin.getMessages().get("status-none"));
                    if (plugin.getConfig().getBoolean("events.automatic", true)) {
                        sender.sendMessage(plugin.getMessages().format("status-next",
                                "{remaining}", Integer.toString(manager.getAutomaticCountdown())));
                    } else {
                        sender.sendMessage(plugin.getMessages().get("status-auto-disabled"));
                    }
                } else {
                    var location = event.getLocation();
                    sender.sendMessage(plugin.getMessages().format("event-status",
                            "{event}", event.getId(), "{state}", event.getState().name(),
                            "{remaining}", Integer.toString(event.getRemainingSeconds()),
                            "{world}", location == null ? "-" : location.getLevel().getName(),
                            "{x}", location == null ? "-" : Integer.toString(location.getX()),
                            "{z}", location == null ? "-" : Integer.toString(location.getZ())));
                }
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
                if (!requested.equals("random") && !manager.isEventEnabled(requested)) {
                    sender.sendMessage(plugin.getMessages().get("event-disabled-command"));
                    return true;
                }
                boolean started = manager.start(requested);
                if (!started) {
                    sender.sendMessage(manager.hasActiveEvent()
                            ? plugin.getMessages().get("event-already-active")
                            : (requested.equals("random")
                            ? plugin.getMessages().get("event-random-unavailable")
                            : plugin.getMessages().get("no-safe-location")));
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
                    if (limit < 1 || limit > 10) {
                        sender.sendMessage(plugin.getMessages().get("stats-top-usage"));
                        return true;
                    }
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
                    String targetName = target == null ? args[1] : target.getName();
                    Integer treasureClaims = target == null
                            ? plugin.getStatisticsManager().getTreasureClaimsByName(args[1])
                            : plugin.getStatisticsManager().getTreasureClaims(target);
                    Integer meteorClaims = target == null
                            ? plugin.getStatisticsManager().getMeteorClaimsByName(args[1])
                            : plugin.getStatisticsManager().getMeteorClaims(target);
                    if (treasureClaims == null && meteorClaims == null) {
                        sender.sendMessage(plugin.getMessages().get("player-not-found"));
                    } else {
                        sender.sendMessage(plugin.getMessages().format("stats-player",
                                "{player}", targetName,
                                "{treasure}", Integer.toString(treasureClaims == null ? 0 : treasureClaims),
                                "{meteor}", Integer.toString(meteorClaims == null ? 0 : meteorClaims)));
                    }
                } else if (sender instanceof Player player) {
                    sender.sendMessage(plugin.getMessages().format("stats-self",
                            "{treasure}", Integer.toString(plugin.getStatisticsManager().getTreasureClaims(player)),
                            "{meteor}", Integer.toString(plugin.getStatisticsManager().getMeteorClaims(player))));
                } else {
                    sender.sendMessage(plugin.getMessages().format("stats-total",
                            "{treasure}", Integer.toString(plugin.getStatisticsManager().getTotalTreasureClaims()),
                            "{meteor}", Integer.toString(plugin.getStatisticsManager().getTotalMeteorClaims())));
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
