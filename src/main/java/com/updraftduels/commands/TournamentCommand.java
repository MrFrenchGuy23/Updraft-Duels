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
import com.updraftduels.model.Tournament;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TournamentCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public TournamentCommand(UpdraftDuels plugin) {
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

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "start" -> handleStart(player, args);
            case "info" -> handleInfo(player, args);
            case "list" -> handleList(player);
            case "bracket" -> handleBracket(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        boolean requirePermission = plugin.getConfig().getBoolean("tournament.require-tourney-permission", true);
        String tourneyPerm = plugin.getConfig().getString("tournament.tourney-permission", "updraftduels.tourney.bc");

        if (requirePermission && !player.hasPermission(tourneyPerm) && !player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage",
                    "%usage%", "/tournament create <name> <maxPlayers>")));
            return;
        }
        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid number."));
            return;
        }
        if (maxPlayers < 2 || maxPlayers > 128) {
            player.sendMessage(ColorUtil.colorizePrefix("&cPlayers must be between 2 and 128."));
            return;
        }

        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length - 1; i++) {
            if (i > 1) name.append(" ");
            name.append(args[i]);
        }

        Tournament tournament = plugin.getTournamentManager().createTournament(name.toString(), maxPlayers);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("tournament.created",
                "%name%", tournament.getName(),
                "%players%", String.valueOf(tournament.getMaxPlayers()))));
        plugin.getGuiManager().openTournamentFormatGUI(player, tournament);
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            List<Tournament> recruiting = plugin.getTournamentManager().getTournamentsByState(Tournament.State.RECRUITING);
            if (recruiting.isEmpty()) {
                player.sendMessage(ColorUtil.colorizePrefix("&cNo tournaments available to join."));
                return;
            }
            Tournament nearest = recruiting.get(0);
            joinTournament(player, nearest.getId());
            return;
        }
        try {
            UUID tournamentId = UUID.fromString(args[1]);
            joinTournament(player, tournamentId);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid tournament ID."));
        }
    }

    private void joinTournament(Player player, UUID tournamentId) {
        if (plugin.getAntiSpamManager().isOnCooldown(player.getUniqueId(), "tournament-join")) {
            int remaining = plugin.getAntiSpamManager().getRemainingSeconds(player.getUniqueId(), "tournament-join");
            player.sendMessage(ColorUtil.colorizePrefix("&cWait " + remaining + "s before joining another tournament."));
            return;
        }

        if (plugin.getTournamentManager().isInTournament(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou are already in a tournament!"));
            return;
        }
        boolean joined = plugin.getTournamentManager().joinTournament(tournamentId, player.getUniqueId());
        if (joined) {
            plugin.getAntiSpamManager().setCooldown(player.getUniqueId(), "tournament-join");
            Tournament t = plugin.getTournamentManager().getTournament(tournamentId);
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("tournament.joined",
                    "%name%", t != null ? t.getName() : "Unknown",
                    "%players%", t != null ? String.valueOf(t.getParticipants().size()) : "0")));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cCould not join tournament. It may be full or in progress."));
        }
    }

    private void handleLeave(Player player) {
        Tournament tournament = plugin.getTournamentManager().getPlayerTournament(player.getUniqueId());
        if (tournament == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou are not in a tournament."));
            return;
        }
        if (tournament.getState() == Tournament.State.IN_PROGRESS) {
            player.sendMessage(ColorUtil.colorizePrefix("&cCannot leave a tournament in progress!"));
            return;
        }
        plugin.getTournamentManager().leaveTournament(tournament.getId(), player.getUniqueId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("tournament.left",
                "%name%", tournament.getName())));
    }

    private void handleStart(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            List<Tournament> recruiting = plugin.getTournamentManager().getTournamentsByState(Tournament.State.RECRUITING);
            if (recruiting.isEmpty()) {
                player.sendMessage(ColorUtil.colorizePrefix("&cNo recruiting tournaments."));
                return;
            }
            plugin.getTournamentManager().startCountdown(recruiting.get(0).getId());
            player.sendMessage(ColorUtil.colorizePrefix("&aTournament countdown started!"));
            return;
        }
        try {
            UUID tournamentId = UUID.fromString(args[1]);
            plugin.getTournamentManager().startCountdown(tournamentId);
            player.sendMessage(ColorUtil.colorizePrefix("&aTournament countdown started!"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid tournament ID."));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            Tournament current = plugin.getTournamentManager().getPlayerTournament(player.getUniqueId());
            if (current == null) {
                player.sendMessage(ColorUtil.colorizePrefix("&cYou are not in a tournament."));
                return;
            }
            showTournamentInfo(player, current);
            return;
        }
        try {
            UUID tournamentId = UUID.fromString(args[1]);
            Tournament tournament = plugin.getTournamentManager().getTournament(tournamentId);
            if (tournament == null) {
                player.sendMessage(ColorUtil.colorizePrefix("&cTournament not found."));
                return;
            }
            showTournamentInfo(player, tournament);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid tournament ID."));
        }
    }

    private void showTournamentInfo(Player player, Tournament t) {
        player.sendMessage(ColorUtil.colorizePrefix("&f" + t.getName()));
        player.sendMessage(ColorUtil.colorizePrefix("&7State: &f" + t.getState().name()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Players: &f" + t.getParticipants().size() + "/" + t.getMaxPlayers()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Round: &f" + t.getCurrentRound() + "/" + t.getTotalRounds()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Ruleset: &f" + t.getRulesetId()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Team Size: &f" + t.getTeamSize()));

        List<Tournament.TournamentMatch> currentMatches = t.getMatchesInRound(t.getCurrentRound());
        if (!currentMatches.isEmpty()) {
            player.sendMessage(ColorUtil.colorizePrefix("&7Current Matches:"));
            for (Tournament.TournamentMatch m : currentMatches) {
                String p1Name = m.getPlayer1() != null ? Bukkit.getOfflinePlayer(m.getPlayer1()).getName() : "BYE";
                String p2Name = m.getPlayer2() != null ? Bukkit.getOfflinePlayer(m.getPlayer2()).getName() : "BYE";
                String status = m.isPlayed() ? "&a✓ " + Bukkit.getOfflinePlayer(m.getWinner()).getName() : "&ePending";
                player.sendMessage(ColorUtil.colorizePrefix("  &f" + p1Name + " &7vs &f" + p2Name + " " + status));
            }
        }

        if (t.getWinner() != null) {
            player.sendMessage(ColorUtil.colorizePrefix("&6Winner: &f" + Bukkit.getOfflinePlayer(t.getWinner()).getName()));
        }
    }

    private void handleList(Player player) {
        Collection<Tournament> all = plugin.getTournamentManager().getAllTournaments();
        if (all.isEmpty()) {
            player.sendMessage(ColorUtil.colorizePrefix("&7No tournaments exist."));
            return;
        }
        player.sendMessage(ColorUtil.colorizePrefix("&fTournaments:"));
        for (Tournament t : all) {
            String status = switch (t.getState()) {
                case RECRUITING -> "&aRecruiting";
                case IN_PROGRESS -> "&eIn Progress";
                case FINISHED -> "&cFinished";
            };
            player.sendMessage(ColorUtil.colorizePrefix("&7- &f" + t.getName() + " &7[" + status + "&7] &f"
                    + t.getParticipants().size() + "/" + t.getMaxPlayers() + " &7ID: &f" + t.getId().toString().substring(0, 8)));
        }
    }

    private void handleBracket(Player player, String[] args) {
        plugin.getGuiManager().openTournamentBracketGUI(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fTournament Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament create <name> <size> &7- Create tournament"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament join [id] &7- Join a tournament"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament leave &7- Leave current tournament"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament start [id] &7- Start tournament (Admin)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament info [id] &7- Tournament info"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament list &7- List all tournaments"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/tournament bracket &7- View the bracket"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "join", "leave", "start", "info", "list", "bracket"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
