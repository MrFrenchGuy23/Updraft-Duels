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
package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.*;
import com.updraftduels.util.ColorUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Tournament> tournaments;
    private final Map<UUID, UUID> playerTournament;
    private final Map<UUID, Integer> countdownTasks;
    private final Map<UUID, Integer> countdownRemaining;
    private final Map<UUID, UUID> pendingFormatTournament;

    public TournamentManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.tournaments = new ConcurrentHashMap<>();
        this.playerTournament = new ConcurrentHashMap<>();
        this.countdownTasks = new ConcurrentHashMap<>();
        this.countdownRemaining = new ConcurrentHashMap<>();
        this.pendingFormatTournament = new ConcurrentHashMap<>();
    }

    public Tournament createTournament(String name, int maxPlayers) {
        int actualMax = nextPowerOfTwo(maxPlayers);
        Tournament tournament = new Tournament(name, actualMax);
        tournaments.put(tournament.getId(), tournament);
        broadcastTournamentAnnouncement(tournament, "&aWaiting");
        return tournament;
    }

    public void startCountdown(UUID tournamentId) {
        Tournament tournament = tournaments.get(tournamentId);
        if (tournament == null || tournament.getState() != Tournament.State.RECRUITING) return;

        int interval = Math.max(1, plugin.getConfig().getInt("tournament.announce-interval-seconds", 4));
        int seconds = Math.max(1, plugin.getConfig().getInt("tournament.countdown-seconds", 15));
        cancelCountdown(tournamentId);
        countdownRemaining.put(tournamentId, seconds);
        broadcastTournamentAnnouncement(tournament, "&eStarting in &f" + seconds + "s");

        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int remaining = countdownRemaining.getOrDefault(tournamentId, 0);
            int next = remaining - interval;
            if (next <= 0) {
                cancelCountdown(tournamentId);
                startTournament(tournamentId);
                return;
            }
            countdownRemaining.put(tournamentId, next);
            broadcastTournamentAnnouncement(tournament, "&eStarting in &f" + next + "s");
        }, interval * 20L, interval * 20L).getTaskId();
        countdownTasks.put(tournamentId, taskId);
    }

    private void cancelCountdown(UUID tournamentId) {
        Integer taskId = countdownTasks.remove(tournamentId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        countdownRemaining.remove(tournamentId);
    }

    public void broadcastTournamentAnnouncement(Tournament tournament, String status) {
        String raw = plugin.getMessages().getRaw("tournament.announcement");
        String message = raw
                .replace("%players%", String.valueOf(tournament.getParticipants().size()))
                .replace("%max%", String.valueOf(tournament.getMaxPlayers()))
                .replace("%gamemode%", getGamemodeName(tournament))
                .replace("%type%", tournament.getTeamSize() + "v" + tournament.getTeamSize())
                .replace("%status%", status);
        String[] lines = message.split("\\n");
        String command = "/tournament join " + tournament.getId();
        String hover = plugin.getMessages().getRaw("tournament.click-to-join")
                .replace("%name%", tournament.getName());
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendAnnouncement(player, lines, command, hover);
        }
    }

    private void sendAnnouncement(Player player, String[] lines, String command, String hover) {
        BaseComponent[] components = new BaseComponent[lines.length];
        for (int i = 0; i < lines.length; i++) {
            TextComponent line = new TextComponent(ColorUtil.colorize(lines[i]));
            if (i == lines.length - 1) {
                line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ColorUtil.colorize(hover))));
            }
            components[i] = line;
        }
        player.spigot().sendMessage(components);
    }

    private String getGamemodeName(Tournament tournament) {
        Ruleset ruleset = plugin.getRulesetManager().getRuleset(tournament.getRulesetId());
        if (ruleset != null) return ruleset.getDisplayName();
        return "Duels";
    }

    public boolean joinTournament(UUID tournamentId, UUID playerUUID) {
        Tournament tournament = tournaments.get(tournamentId);
        if (tournament == null || tournament.getState() != Tournament.State.RECRUITING) return false;
        if (playerTournament.containsKey(playerUUID)) return false;
        if (!tournament.addParticipant(playerUUID)) return false;
        playerTournament.put(playerUUID, tournamentId);
        return true;
    }

    public boolean leaveTournament(UUID tournamentId, UUID playerUUID) {
        Tournament tournament = tournaments.get(tournamentId);
        if (tournament == null) return false;
        if (tournament.getState() == Tournament.State.IN_PROGRESS) return false;
        if (!tournament.removeParticipant(playerUUID)) return false;
        playerTournament.remove(playerUUID);
        return true;
    }

    public void startTournament(UUID tournamentId) {
        Tournament tournament = tournaments.get(tournamentId);
        if (tournament == null || tournament.getState() != Tournament.State.RECRUITING) return;
        if (tournament.getParticipants().size() < 2) return;
        cancelCountdown(tournamentId);

        tournament.setState(Tournament.State.IN_PROGRESS);
        tournament.setStartTime(System.currentTimeMillis());
        tournament.setCurrentRound(1);

        List<UUID> shuffled = new ArrayList<>(tournament.getParticipants());
        Collections.shuffle(shuffled);

        int n = shuffled.size();
        int bracketSize = nextPowerOfTwo(n);
        tournament.setBracketSize(bracketSize);

        int totalRounds = 0;
        for (int s = bracketSize; s > 1; s /= 2) totalRounds++;
        tournament.setTotalRounds(totalRounds);

        for (int round = 1; round <= totalRounds; round++) {
            int matchesThisRound = bracketSize / (int) Math.pow(2, round);
            for (int i = 0; i < matchesThisRound; i++) {
                tournament.getMatches().add(new Tournament.TournamentMatch(round, null, null));
            }
        }

        List<Tournament.TournamentMatch> round1 = tournament.getMatchesInRound(1);
        int byes = bracketSize - n;
        int twoPlayerMatches = Math.max(0, round1.size() - byes);
        int idx = 0;
        for (int m = 0; m < twoPlayerMatches && idx < n; m++) {
            round1.get(m).setPlayer1(shuffled.get(idx++));
            round1.get(m).setPlayer2(shuffled.get(idx++));
        }
        for (int m = twoPlayerMatches; m < round1.size() && idx < n; m++) {
            round1.get(m).setPlayer1(shuffled.get(idx++));
        }

        for (UUID uuid : tournament.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String msg = plugin.getMessages().get("tournament.started",
                        "%name%", tournament.getName(),
                        "%players%", String.valueOf(tournament.getParticipants().size()));
                player.sendMessage(msg);
            }
        }

        processCurrentRound(tournament);
    }

    private void processCurrentRound(Tournament tournament) {
        if (tournament.getState() != Tournament.State.IN_PROGRESS) return;

        for (Tournament.TournamentMatch match : new ArrayList<>(tournament.getMatchesInRound(tournament.getCurrentRound()))) {
            if (match.isPlayed()) continue;

            if (match.isBye()) {
                UUID winner = match.getPlayer1() != null ? match.getPlayer1() : match.getPlayer2();
                if (winner == null) continue;
                match.setWinner(winner);
                match.setPlayed(true);
                advanceWinner(tournament, match, winner);
                continue;
            }

            Player p1 = Bukkit.getPlayer(match.getPlayer1());
            Player p2 = Bukkit.getPlayer(match.getPlayer2());
            if (p1 == null || p2 == null) {
                UUID winner = p1 != null ? match.getPlayer1() : match.getPlayer2();
                UUID loser = p1 == null ? match.getPlayer1() : match.getPlayer2();
                match.setWinner(winner);
                match.setPlayed(true);
                advanceWinner(tournament, match, winner);
                if (loser != null) {
                    notifyForfeit(tournament, loser, winner);
                    playerTournament.remove(loser);
                }
                continue;
            }

            startMatch(tournament, match);
        }

        checkRoundCompletion(tournament);
    }

    private void checkRoundCompletion(Tournament tournament) {
        if (tournament.getState() != Tournament.State.IN_PROGRESS) return;

        boolean roundComplete = tournament.getMatchesInRound(tournament.getCurrentRound())
                .stream().allMatch(Tournament.TournamentMatch::isPlayed);
        if (!roundComplete) return;

        if (tournament.getCurrentRound() >= tournament.getTotalRounds()) {
            finishTournament(tournament);
            return;
        }

        tournament.setCurrentRound(tournament.getCurrentRound() + 1);
        processCurrentRound(tournament);
    }

    private void startMatch(Tournament tournament, Tournament.TournamentMatch match) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());
        if (p1 == null || p2 == null) return;

        Arena arena = plugin.getArenaManager().getRandomAvailableArena();
        if (arena == null) {
            for (UUID uuid : tournament.getParticipants()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                            "&cNo arenas available for tournament match, retrying in 5 seconds..."));
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> processCurrentRound(tournament), 100L);
            return;
        }

        var request = plugin.getDuelManager().createRequest(
                match.getPlayer1(), match.getPlayer2(),
                DuelType.SOLO, tournament.getRulesetId());

        if (request != null) {
            boolean accepted = plugin.getDuelManager().acceptRequest(request.getRequestId(), match.getPlayer2());
            if (accepted) {
                Duel duel = plugin.getDuelManager().getDuelOf(match.getPlayer1());
                if (duel != null) {
                    match.setDuelId(duel.getId());
                }
            }
        }

        notifyMatch(tournament, match);
    }

    public void onDuelEnd(Duel duel, UUID winnerUUID) {
        for (Tournament tournament : tournaments.values()) {
            if (tournament.getState() != Tournament.State.IN_PROGRESS) continue;

            Tournament.TournamentMatch match = tournament.getMatches().stream()
                    .filter(m -> m.getDuelId() != null && m.getDuelId().equals(duel.getId()))
                    .findFirst().orElse(null);

            if (match == null || match.isPlayed()) continue;

            if (winnerUUID == null) {
                UUID fallback = match.getPlayer1() != null && Bukkit.getPlayer(match.getPlayer1()) != null
                        ? match.getPlayer1() : match.getPlayer2();
                winnerUUID = fallback;
            }

            match.setWinner(winnerUUID);
            match.setPlayed(true);

            advanceWinner(tournament, match, winnerUUID);

            UUID loser = match.getOpponent(winnerUUID);
            if (loser != null) {
                Player loserPlayer = Bukkit.getPlayer(loser);
                if (loserPlayer != null) {
                    String msg = plugin.getMessages().get("tournament.eliminated",
                            "%name%", tournament.getName(),
                            "%round%", String.valueOf(tournament.getCurrentRound()));
                    loserPlayer.sendMessage(msg);
                }
                playerTournament.remove(loser);
            }

            checkRoundCompletion(tournament);
            return;
        }
    }

    private void advanceWinner(Tournament tournament, Tournament.TournamentMatch match, UUID winnerUUID) {
        if (tournament.getCurrentRound() >= tournament.getTotalRounds()) return;

        int matchIndex = tournament.getMatchesInRound(tournament.getCurrentRound()).indexOf(match);
        if (matchIndex < 0) return;

        int nextRound = tournament.getCurrentRound() + 1;
        List<Tournament.TournamentMatch> nextRoundMatches = tournament.getMatchesInRound(nextRound);
        int nextMatchIndex = matchIndex / 2;
        if (nextRoundMatches.isEmpty() || nextMatchIndex >= nextRoundMatches.size()) return;

        Tournament.TournamentMatch nextMatch = nextRoundMatches.get(nextMatchIndex);
        if (matchIndex % 2 == 0) {
            nextMatch.setPlayer1(winnerUUID);
        } else {
            nextMatch.setPlayer2(winnerUUID);
        }
    }

    private void finishTournament(Tournament tournament) {
        tournament.setState(Tournament.State.FINISHED);
        tournament.setEndTime(System.currentTimeMillis());

        List<Tournament.TournamentMatch> finalRound = tournament.getMatchesInRound(tournament.getCurrentRound());
        UUID winner = finalRound.isEmpty() ? null : finalRound.get(0).getWinner();
        if (winner == null && tournament.getParticipants().size() == 1) {
            winner = tournament.getParticipants().get(0);
        }
        tournament.setWinner(winner);

        for (UUID uuid : tournament.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String winMsg = plugin.getMessages().get("tournament.finished-winner",
                        "%name%", tournament.getName(),
                        "%winner%", winner != null ? Bukkit.getOfflinePlayer(winner).getName() : "Unknown");
                player.sendMessage(winMsg);
                if (uuid.equals(winner)) {
                    player.sendTitle(
                            com.updraftduels.util.ColorUtil.colorize("&6Tournament Winner"),
                            com.updraftduels.util.ColorUtil.colorize("&7You won &f" + tournament.getName() + "&7."),
                            10, 60, 20);
                    giveTournamentRewards(player, tournament);
                }
            }
            playerTournament.remove(uuid);
        }
    }

    private void giveTournamentRewards(Player player, Tournament tournament) {
        String rewardCmd = plugin.getConfig().getString("tournament.reward-command", "");
        if (!rewardCmd.isEmpty()) {
            String cmd = rewardCmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private void notifyMatch(Tournament tournament, Tournament.TournamentMatch match) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());
        String roundName = getRoundName(tournament, match.getRound());

        if (p1 != null) {
            p1.sendMessage(plugin.getMessages().get("tournament.match-found",
                    "%opponent%", p2 != null ? p2.getName() : "Unknown",
                    "%round%", roundName,
                    "%name%", tournament.getName()));
        }
        if (p2 != null) {
            p2.sendMessage(plugin.getMessages().get("tournament.match-found",
                    "%opponent%", p1 != null ? p1.getName() : "Unknown",
                    "%round%", roundName,
                    "%name%", tournament.getName()));
        }
    }

    private void notifyForfeit(Tournament tournament, UUID loserUUID, UUID winnerUUID) {
        Player loser = Bukkit.getPlayer(loserUUID);
        Player winner = Bukkit.getPlayer(winnerUUID);
        if (loser != null) {
            String msg = plugin.getMessages().get("tournament.forfeit",
                    "%opponent%", winner != null ? winner.getName() : "Unknown");
            loser.sendMessage(msg);
        }
        if (winner != null) {
            String msg = plugin.getMessages().get("tournament.opponent-forfeit",
                    "%opponent%", loser != null ? loser.getName() : "Unknown");
            winner.sendMessage(msg);
        }
    }

    private String getRoundName(Tournament tournament, int round) {
        int totalRounds = tournament.getTotalRounds();
        int roundsFromFinal = totalRounds - round;
        return switch (roundsFromFinal) {
            case 0 -> "Final";
            case 1 -> "Semi-Final";
            case 2 -> "Quarter-Final";
            default -> "Round " + round;
        };
    }

    public Tournament getTournament(UUID id) { return tournaments.get(id); }
    public void setPendingFormat(UUID playerUUID, UUID tournamentId) { pendingFormatTournament.put(playerUUID, tournamentId); }
    public UUID getPendingFormat(UUID playerUUID) { return pendingFormatTournament.remove(playerUUID); }
    public Tournament getPlayerTournament(UUID playerUUID) {
        UUID tid = playerTournament.get(playerUUID);
        return tid != null ? tournaments.get(tid) : null;
    }
    public Collection<Tournament> getAllTournaments() { return tournaments.values(); }
    public boolean isInTournament(UUID uuid) { return playerTournament.containsKey(uuid); }

    public List<Tournament> getTournamentsByState(Tournament.State state) {
        return tournaments.values().stream().filter(t -> t.getState() == state).toList();
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }
}
