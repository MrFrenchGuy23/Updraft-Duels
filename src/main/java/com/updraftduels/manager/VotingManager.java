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
import com.updraftduels.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VotingManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, VoteSession> activeSessions;

    public VotingManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    public void startVote(UUID duelId, List<UUID> participants, List<String> arenaOptions) {
        startVote(duelId, participants, arenaOptions, 15, null);
    }

    public void startVote(UUID duelId, List<UUID> participants, List<String> arenaOptions, Consumer<String> onComplete) {
        startVote(duelId, participants, arenaOptions, 15, onComplete);
    }

    public void startVote(UUID duelId, List<UUID> participants, List<String> arenaOptions, int timeSeconds, Consumer<String> onComplete) {
        VoteSession session = new VoteSession(duelId, arenaOptions, participants, Math.max(3, timeSeconds), onComplete);
        activeSessions.put(duelId, session);

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String optionsStr = String.join(", ", arenaOptions);
                player.sendMessage(plugin.getMessages().get("voting.vote-started",
                        "%options%", optionsStr,
                        "%time%", String.valueOf(session.getTimeSeconds())));
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> resolveVote(duelId), session.getTimeSeconds() * 20L);
    }

    public boolean castVote(UUID duelId, UUID playerUUID, String arenaName) {
        VoteSession session = activeSessions.get(duelId);
        if (session == null || session.isResolved()) return false;
        if (!session.isParticipant(playerUUID)) return false;
        if (session.hasVoted(playerUUID)) return false;
        if (!session.getOptions().contains(arenaName)) return false;

        session.vote(playerUUID, arenaName);

        for (UUID uuid : session.getParticipantVotes().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(plugin.getMessages().get("voting.player-voted",
                        "%player%", Bukkit.getPlayer(playerUUID) != null
                                ? Bukkit.getPlayer(playerUUID).getName() : "Unknown",
                        "%arena%", arenaName,
                        "%votes%", String.valueOf(session.getVoteCount(arenaName))));
            }
        }

        if (session.getParticipantVotes().size() >= session.getAllParticipants().size()) {
            resolveVote(duelId);
        }
        return true;
    }

    public void resolveVote(UUID duelId) {
        VoteSession session = activeSessions.remove(duelId);
        if (session == null || session.isResolved()) return;
        session.setResolved(true);

        String winner = session.computeWinningArena();

        for (UUID uuid : session.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(plugin.getMessages().get("voting.vote-resolved",
                        "%arena%", winner != null ? winner : "Random"));
            }
        }

        if (winner == null) {
            Arena arena = plugin.getArenaManager().getRandomAvailableArena();
            if (arena != null) winner = arena.getName();
        }

        session.setWinningArena(winner);
        if (session.getOnComplete() != null) {
            session.getOnComplete().accept(winner);
        }
    }

    public String getVotedArena(UUID duelId) {
        VoteSession session = activeSessions.get(duelId);
        if (session == null || !session.isResolved()) return null;
        return session.getWinningArena();
    }

    public VoteSession getSession(UUID duelId) {
        return activeSessions.get(duelId);
    }

    public static class VoteSession {
        private final UUID duelId;
        private final List<String> options;
        private final Map<UUID, String> participantVotes;
        private final List<UUID> participants;
        private boolean resolved;
        private String winningArena;
        private final int timeSeconds;
        private final Consumer<String> onComplete;

        public VoteSession(UUID duelId, List<String> options, List<UUID> participants, int timeSeconds, Consumer<String> onComplete) {
            this.duelId = duelId;
            this.options = options;
            this.participantVotes = new ConcurrentHashMap<>();
            this.participants = participants;
            this.resolved = false;
            this.timeSeconds = timeSeconds;
            this.onComplete = onComplete;
        }

        public UUID getDuelId() { return duelId; }
        public List<String> getOptions() { return options; }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
        public String getWinningArena() { return winningArena; }
        public void setWinningArena(String arena) { this.winningArena = arena; }
        public int getTimeSeconds() { return timeSeconds; }
        public Consumer<String> getOnComplete() { return onComplete; }

        public void vote(UUID uuid, String arena) { participantVotes.put(uuid, arena); }
        public boolean hasVoted(UUID uuid) { return participantVotes.containsKey(uuid); }
        public boolean isParticipant(UUID uuid) { return participants.contains(uuid); }
        public Map<UUID, String> getParticipantVotes() { return participantVotes; }

        public List<UUID> getAllParticipants() { return new ArrayList<>(participants); }

        public int getVoteCount(String arena) {
            return (int) participantVotes.values().stream().filter(a -> a.equals(arena)).count();
        }

        public String computeWinningArena() {
            return options.stream()
                    .max(Comparator.comparingInt(this::getVoteCount))
                    .orElse(options.isEmpty() ? null : options.get(0));
        }
    }
}
