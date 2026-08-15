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
package com.updraftduels.model;

import org.bukkit.Location;

import java.util.*;

public class Tournament {
    public enum State { RECRUITING, IN_PROGRESS, FINISHED }

    private final UUID id;
    private final String name;
    private State state;
    private final int maxPlayers;
    private final List<UUID> participants;
    private final List<TournamentMatch> matches;
    private int currentRound;
    private UUID winner;
    private long startTime;
    private long endTime;
    private String rulesetId;
    private int teamSize;
    private int totalRounds;
    private int bracketSize;

    public Tournament(String name, int maxPlayers) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.state = State.RECRUITING;
        this.maxPlayers = maxPlayers;
        this.participants = new ArrayList<>();
        this.matches = new ArrayList<>();
        this.currentRound = 0;
        this.winner = null;
        this.rulesetId = "default";
        this.teamSize = 1;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<UUID> getParticipants() { return participants; }
    public List<TournamentMatch> getMatches() { return matches; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int round) { this.currentRound = round; }
    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getRulesetId() { return rulesetId; }
    public void setRulesetId(String rulesetId) { this.rulesetId = rulesetId; }
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }
    public int getTotalRounds() {
        if (totalRounds > 0) return totalRounds;
        int size = participants.size();
        int rounds = 0;
        while (size > 1) {
            size = (size + 1) / 2;
            rounds++;
        }
        return rounds;
    }
    public void setTotalRounds(int totalRounds) { this.totalRounds = totalRounds; }
    public int getBracketSize() { return bracketSize; }
    public void setBracketSize(int bracketSize) { this.bracketSize = bracketSize; }

    public boolean addParticipant(UUID uuid) {
        if (participants.size() >= maxPlayers) return false;
        if (participants.contains(uuid)) return false;
        participants.add(uuid);
        return true;
    }

    public boolean removeParticipant(UUID uuid) {
        return participants.remove(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public List<TournamentMatch> getMatchesInRound(int round) {
        return matches.stream().filter(m -> m.getRound() == round).toList();
    }

    public List<TournamentMatch> getMatchesForPlayer(UUID uuid) {
        return matches.stream()
                .filter(m -> m.getPlayer1().equals(uuid) || m.getPlayer2().equals(uuid))
                .toList();
    }

    public TournamentMatch getMatchById(UUID matchId) {
        return matches.stream().filter(m -> m.getId().equals(matchId)).findFirst().orElse(null);
    }

    public static class TournamentMatch {
        private final UUID id;
        private final int round;
        private UUID player1;
        private UUID player2;
        private UUID winner;
        private UUID duelId;
        private boolean played;

        public TournamentMatch(int round, UUID player1, UUID player2) {
            this.id = UUID.randomUUID();
            this.round = round;
            this.player1 = player1;
            this.player2 = player2;
            this.winner = null;
            this.duelId = null;
            this.played = false;
        }

        public UUID getId() { return id; }
        public int getRound() { return round; }
        public UUID getPlayer1() { return player1; }
        public void setPlayer1(UUID player1) { this.player1 = player1; }
        public UUID getPlayer2() { return player2; }
        public void setPlayer2(UUID player2) { this.player2 = player2; }
        public UUID getWinner() { return winner; }
        public void setWinner(UUID winner) { this.winner = winner; }
        public UUID getDuelId() { return duelId; }
        public void setDuelId(UUID duelId) { this.duelId = duelId; }
        public boolean isPlayed() { return played; }
        public void setPlayed(boolean played) { this.played = played; }

        public boolean isBye() {
            return player1 == null || player2 == null;
        }

        public UUID getOpponent(UUID player) {
            if (player1 != null && player1.equals(player)) return player2;
            if (player2 != null && player2.equals(player)) return player1;
            return null;
        }
    }
}
