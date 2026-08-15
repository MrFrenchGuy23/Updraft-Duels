/*
 * Updraft Duels
 * Copyright (C) 2026 Updraft Duels
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.*;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public DuelCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "spectate" -> handleSpectate(player, args);
            case "profile" -> handleProfile(player, args);
            default -> {
                if (!sub.startsWith("-")) {
                    handleDuelRequest(player, args);
                }
            }
        }
        return true;
    }

    private void handleDuelRequest(Player sender, String[] args) {
        if (!sender.hasPermission("updraftduels.duel")) {
            sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }

        if (plugin.getDuelManager().isInDuel(sender.getUniqueId())) {
            sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[0])));
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.cannot-duel-self")));
            return;
        }

        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.target-in-duel")));
            return;
        }

        if (args.length >= 2) {
            String kitName = args[1];
            int rounds = args.length >= 3 ? parseRounds(args[2]) : 1;
            plugin.getDuelManager().sendDuelRequest(sender, target, kitName, rounds);
            return;
        }

        plugin.getDuelManager().startDuelSelection(sender.getUniqueId(), target.getUniqueId());
        plugin.getGuiManager().openDuelKitGUI(sender);
    }

    private int parseRounds(String value) {
        try {
            int rounds = Integer.parseInt(value);
            return Math.max(1, Math.min(rounds, 5));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void handleAccept(Player player, String[] args) {
        List<DuelRequest> incoming = plugin.getDuelManager().getIncomingRequests(player.getUniqueId());
        if (incoming.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.no-pending-requests")));
            return;
        }

        DuelRequest request = incoming.get(0);
        if (args.length > 1) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender != null) {
                request = incoming.stream()
                        .filter(r -> r.getSenderUUID().equals(sender.getUniqueId()))
                        .findFirst().orElse(request);
            }
        }

        boolean accepted = plugin.getDuelManager().acceptRequest(request.getRequestId(), player.getUniqueId());
        if (accepted) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.request-accepted",
                    "%player%", Bukkit.getOfflinePlayer(request.getSenderUUID()).getName())));
            Player sender = Bukkit.getPlayer(request.getSenderUUID());
            if (sender != null) {
                sender.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.request-received",
                        "%player%", player.getName())));
            }
        }
    }

    private void handleDeny(Player player, String[] args) {
        List<DuelRequest> incoming = plugin.getDuelManager().getIncomingRequests(player.getUniqueId());
        if (incoming.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.no-pending-requests")));
            return;
        }

        DuelRequest request = incoming.get(0);
        if (args.length > 1) {
            Player sender = Bukkit.getPlayer(args[1]);
            if (sender != null) {
                request = incoming.stream()
                        .filter(r -> r.getSenderUUID().equals(sender.getUniqueId()))
                        .findFirst().orElse(request);
            }
        }

        plugin.getDuelManager().denyRequest(request.getRequestId(), player.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.request-denied",
                "%player%", Bukkit.getOfflinePlayer(request.getSenderUUID()).getName())));
    }

    private void handleSpectate(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.duel.spectate")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }

        if (args.length < 2) {
            plugin.getGuiManager().openSpectatorSelectorGUI(player);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }

        Duel duel = plugin.getDuelManager().getDuelOf(target.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("spectator.player-not-in-duel")));
            return;
        }

        plugin.getSpectatorManager().startSpectating(player, target, duel);
    }

    private void handleProfile(Player player, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-not-found", "%player%", args[1])));
            return;
        }
        plugin.getGuiManager().openProfileGUI(player, target);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fDuel Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duel <player> [kit] [rounds] &7- Send a duel request"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duel accept|deny [player] &7- Accept or deny a request"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duel spectate [player] &7- Spectate a duel"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duel profile [player] &7- View profile"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue <kit> &7- Join the queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena &7- Arena management"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit &7- Kit management"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/party &7- Party management"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend &7- Friends management"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("accept", "deny", "spectate", "profile"));
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spectate") || args[0].equalsIgnoreCase("profile")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (!args[0].equalsIgnoreCase("accept") && !args[0].equalsIgnoreCase("deny")) {
                return plugin.getKitManager().getAllVisibleKits(player.getUniqueId()).stream()
                        .map(kit -> kit.getName())
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
