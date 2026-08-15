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
import com.updraftduels.model.DuelRequest;
import com.updraftduels.model.DuelType;
import com.updraftduels.util.ChatUtil;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FriendCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public FriendCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("updraftduels.friend")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return true;
        }

        if (args.length == 0) {
            handleList(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            case "duel" -> handleFriendDuel(player, args);
            case "toggleautoaccept" -> handleToggleAutoAccept(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/friend add <player>")));
            return;
        }
        if (args[1].equalsIgnoreCase(player.getName())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.add-self")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }
        if (plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.already-friends", "%player%", target.getName())));
            return;
        }
        plugin.getFriendManager().addFriend(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.added", "%player%", target.getName())));
        target.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.added", "%player%", player.getName())));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/friend remove <player>")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }
        if (!plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.not-friends", "%player%", target.getName())));
            return;
        }
        plugin.getFriendManager().removeFriend(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.removed", "%player%", target.getName())));
    }

    private void handleList(Player player) {
        Map<UUID, Boolean> friends = plugin.getFriendManager().getFriendsWithStatus(player.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.list-header")));
        for (Map.Entry<UUID, Boolean> entry : friends.entrySet()) {
            Player friend = Bukkit.getPlayer(entry.getKey());
            String name = friend != null ? friend.getName() : entry.getKey().toString().substring(0, 8);
            if (entry.getValue()) {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.list-online", "%player%", name)));
            } else {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.list-offline", "%player%", name)));
            }
        }
    }

    private void handleFriendDuel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/friend duel <player>")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }
        if (!plugin.getFriendManager().isFriend(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou are not friends with that player."));
            return;
        }

        boolean autoAccept = plugin.getFriendManager().shouldAutoAccept(target.getUniqueId(), player.getUniqueId());

        if (plugin.getAntiSpamManager().isOnCooldown(player.getUniqueId(), "duel-request")) {
            plugin.getAntiSpamManager().sendCooldownMessage(player, "duel-request");
            return;
        }
        DuelRequest request = plugin.getDuelManager().createRequest(player.getUniqueId(), target.getUniqueId(),
                DuelType.SOLO, "default");
        plugin.getAntiSpamManager().setCooldown(player.getUniqueId(), "duel-request");

        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.request-sent", "%player%", target.getName())));

        if (autoAccept) {
            plugin.getDuelManager().acceptRequest(request.getRequestId(), target.getUniqueId());
            target.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.friend-duel-request", "%player%", player.getName())));
        } else {
            String acceptCmd = "/duel accept " + player.getName();
            ChatUtil.sendClickable(target,
                    plugin.getMessages().get("friend.friend-duel-request", "%player%", player.getName()),
                    acceptCmd, "&aClick to accept!");
        }
    }

    private void handleToggleAutoAccept(Player player) {
        Set<UUID> friends = plugin.getFriendManager().getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou have no friends."));
            return;
        }
        UUID first = friends.iterator().next();
        boolean currentlyOn = plugin.getFriendManager().getAutoAccept(player.getUniqueId(), first);
        boolean targetState = !currentlyOn;
        for (UUID friendUUID : friends) {
            boolean current = plugin.getFriendManager().getAutoAccept(player.getUniqueId(), friendUUID);
            if (current != targetState) {
                plugin.getFriendManager().toggleAutoAccept(player.getUniqueId(), friendUUID);
            }
        }
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("friend.auto-accept-toggled",
                "%state%", targetState ? "&aON" : "&cOFF")));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fFriend Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend &7- List your friends"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend add <player> &7- Add a friend"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend remove <player> &7- Remove a friend"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend list &7- View friends list"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend duel <player> &7- Quick duel a friend"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/friend toggleautoaccept &7- Toggle auto-accept"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return filter(List.of("add", "remove", "list", "duel", "toggleautoaccept"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("duel"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
