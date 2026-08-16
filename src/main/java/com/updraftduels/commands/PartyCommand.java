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
import com.updraftduels.model.Party;
import com.updraftduels.util.ChatUtil;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public PartyCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("updraftduels.party")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openPartyGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "kick" -> handleKick(player, args);
            case "duel", "challenge" -> handlePartyDuel(player, args);
            case "list" -> handleList(player);
            case "ready" -> handleReady(player);
            case "chat" -> handleChat(player, args);
            case "gui" -> plugin.getGuiManager().openPartyGUI(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player) {
        Party party = plugin.getPartyManager().createParty(player.getUniqueId());
        if (party != null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.created")));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.already-in-party")));
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/party invite <player>")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.cannot-invite-self")));
            return;
        }

        if (plugin.getPartyManager().inviteToParty(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.invite-sent", "%player%", target.getName())));

            String acceptCmd = "/party accept";
            ChatUtil.sendClickable(target,
                    plugin.getMessages().get("party.invite-received", "%player%", player.getName()),
                    acceptCmd, "&aClick to join the party!");
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.invite-failed")));
        }
    }

    private void handleAccept(Player player) {
        com.updraftduels.model.PartyDuelChallenge challenge = plugin.getPartyManager().getPendingChallengeFor(player.getUniqueId());
        if (challenge != null) {
            if (plugin.getPartyManager().acceptChallenge(challenge.getChallengeId(), player.getUniqueId())) {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.duel-accepted")));
            } else {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.duel-accept-failed")));
            }
            return;
        }

        if (plugin.getPartyManager().acceptInvite(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.invite-accepted", "%player%", "the party")));
            Party party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party != null) {
                Player leader = Bukkit.getPlayer(party.getLeaderUUID());
                if (leader != null) {
                    leader.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.invite-accepted", "%player%", player.getName())));
                }
            }
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.no-pending-invites")));
        }
    }

    private void handleLeave(Player player) {
        if (plugin.getPartyManager().leaveParty(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.left")));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
        }
    }

    private void handleDisband(Player player) {
        if (plugin.getPartyManager().disbandParty(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.disbanded")));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/party kick <player>")));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }
        if (plugin.getPartyManager().kickMember(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.kicked-player", "%player%", target.getName())));
            target.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.kicked")));
        }
    }

    private void handlePartyDuel(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getGuiManager().openPartyDuelGUI(player);
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline", "%player%", args[1])));
            return;
        }

        Party defender = plugin.getPartyManager().getParty(target.getUniqueId());
        if (defender == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.target-not-in-party")));
            return;
        }
        challengeParty(player, defender);
    }

    public void challengeParty(Player player, Party defender) {
        Party challenger = plugin.getPartyManager().getParty(player.getUniqueId());
        if (challenger == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
            return;
        }

        if (!challenger.isLeader(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.leader-only")));
            return;
        }

        if (plugin.getPartyManager().isPartyInFight(defender)) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.in-fight")));
            return;
        }

        if (challenger.getSize() != defender.getSize()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.wrong-size")));
            return;
        }

        com.updraftduels.model.Arena arena = plugin.getArenaManager().getRandomAvailableArena();
        if (arena == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.no-arenas")));
            return;
        }

        if (plugin.getPartyManager().createChallenge(player.getUniqueId(), defender.getLeaderUUID(), arena.getName(), "default")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.duel-challenge-sent",
                    "%party%", nameOf(defender.getLeaderUUID()))));

            for (UUID memberUUID : defender.getMembers()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null) {
                    String acceptCmd = "/party accept";
                    ChatUtil.sendClickable(member,
                            plugin.getMessages().get("party.party-duel-challenge", "%party%", nameOf(challenger.getLeaderUUID())),
                            acceptCmd, "&aClick to accept party duel!");
                }
            }
        }
    }

    private String nameOf(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private void handleList(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
            return;
        }
        Player leader = Bukkit.getPlayer(party.getLeaderUUID());
        String leaderName = leader != null ? leader.getName() : party.getLeaderUUID().toString().substring(0, 8);
        List<String> memberNames = party.getMembers().stream()
                .map(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    return (p != null && p.isOnline() ? "&a" : "&c") + (p != null ? p.getName() : uuid.toString().substring(0, 8));
                }).toList();
        String membersStr = String.join("&7, ", memberNames);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.party-list",
                "%leader%", leaderName, "%members%", membersStr)));
    }

    private void handleReady(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
            return;
        }
        party.getReadyMembers().add(player.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.ready-confirmed")));

        if (party.isReadyCheckComplete()) {
            for (UUID uuid : party.getMembers()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    member.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.ready-check-complete")));
                }
            }
        }
    }

    private void handleChat(Player player, String[] args) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.not-in-party")));
            return;
        }
        if (args.length < 2) {
            boolean enabled = !plugin.getPartyManager().isPartyChatEnabled(player.getUniqueId());
            plugin.getPartyManager().setPartyChat(player.getUniqueId(), enabled);
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get(enabled ? "party.chat-enabled" : "party.chat-disabled")));
            return;
        }
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.chat-prefix") + "&f" + player.getName() + ": &7" + message));
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fParty Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party create &7- Create a party"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party invite <player> &7- Invite a player"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party accept &7- Accept an invite"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party leave &7- Leave the party"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party disband &7- Disband the party"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party kick <player> &7- Kick a member"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party duel [player] &7- Open party duel GUI or challenge a party"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party list &7- View party members"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party ready &7- Mark as ready"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party chat [msg] &7- Toggle party chat or send a message"));
        player.sendMessage(ColorUtil.colorizePrefix("&2/party gui &7- Open the party GUI"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return filter(List.of("create", "invite", "accept", "leave", "disband", "kick", "duel", "challenge", "list", "ready", "chat", "gui"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("duel") || args[0].equalsIgnoreCase("challenge"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
