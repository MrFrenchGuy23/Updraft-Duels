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

public class Duel {
    private final UUID id;
    private final DuelType type;
    private final String arenaName;
    private DuelState state;
    private final List<DuelTeam> teams;
    private final List<UUID> spectators;
    private String rulesetId;
    private final Map<UUID, Location> originalLocations;
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> originalContents;
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> originalArmorContents;
    private final Map<UUID, org.bukkit.inventory.ItemStack> originalOffHandContents;
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> originalEnderChestContents;
    private final Map<UUID, Double> originalHealth;
    private final Map<UUID, Integer> originalFoodLevel;
    private UUID winnerTeamIndex;
    private long startTime;
    private long endTime;
    private UUID tournamentId;
    private int tournamentRound;
    private String deathCause;
    private boolean ranked;
    private int scoreA;
    private int scoreB;
    private int rounds = 1;
    private int currentRound = 1;

    public Duel(UUID id, DuelType type, String arenaName) {
        this.id = id;
        this.type = type;
        this.arenaName = arenaName;
        this.state = DuelState.WAITING;
        this.teams = new ArrayList<>();
        this.spectators = new ArrayList<>();
        this.originalLocations = new HashMap<>();
        this.originalContents = new HashMap<>();
        this.originalArmorContents = new HashMap<>();
        this.originalOffHandContents = new HashMap<>();
        this.originalEnderChestContents = new HashMap<>();
        this.originalHealth = new HashMap<>();
        this.originalFoodLevel = new HashMap<>();
    }

    public UUID getId() { return id; }
    public DuelType getType() { return type; }
    public String getArenaName() { return arenaName; }
    public DuelState getState() { return state; }
    public void setState(DuelState state) { this.state = state; }
    public List<DuelTeam> getTeams() { return teams; }
    public List<UUID> getSpectators() { return spectators; }
    public String getRulesetId() { return rulesetId; }
    public void setRulesetId(String rulesetId) { this.rulesetId = rulesetId; }
    public Map<UUID, Location> getOriginalLocations() { return originalLocations; }
    public Map<UUID, org.bukkit.inventory.ItemStack[]> getOriginalContents() { return originalContents; }
    public Map<UUID, org.bukkit.inventory.ItemStack[]> getOriginalArmorContents() { return originalArmorContents; }
    public Map<UUID, org.bukkit.inventory.ItemStack> getOriginalOffHandContents() { return originalOffHandContents; }
    public Map<UUID, org.bukkit.inventory.ItemStack[]> getOriginalEnderChestContents() { return originalEnderChestContents; }
    public Map<UUID, Double> getOriginalHealth() { return originalHealth; }
    public Map<UUID, Integer> getOriginalFoodLevel() { return originalFoodLevel; }
    public UUID getWinnerTeamIndex() { return winnerTeamIndex; }
    public void setWinnerTeamIndex(UUID winnerTeamIndex) { this.winnerTeamIndex = winnerTeamIndex; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public UUID getTournamentId() { return tournamentId; }
    public void setTournamentId(UUID tournamentId) { this.tournamentId = tournamentId; }
    public int getTournamentRound() { return tournamentRound; }
    public void setTournamentRound(int tournamentRound) { this.tournamentRound = tournamentRound; }
    public String getDeathCause() { return deathCause; }
    public void setDeathCause(String deathCause) { this.deathCause = deathCause; }
    public boolean isRanked() { return ranked; }
    public void setRanked(boolean ranked) { this.ranked = ranked; }
    public int getScoreA() { return scoreA; }
    public int getScoreB() { return scoreB; }
    public void incrementScoreA() { scoreA++; }
    public void incrementScoreB() { scoreB++; }
    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = Math.max(1, rounds); }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = Math.max(1, currentRound); }

    public void addTeam(DuelTeam team) {
        teams.add(team);
    }

    public DuelTeam getTeam(int index) {
        return index < teams.size() ? teams.get(index) : null;
    }

    public boolean isParticipant(UUID uuid) {
        return teams.stream().anyMatch(t -> t.getMembers().contains(uuid));
    }

    public DuelTeam getTeamOf(UUID uuid) {
        return teams.stream().filter(t -> t.getMembers().contains(uuid)).findFirst().orElse(null);
    }

    public Team getTeamIndex(UUID uuid) {
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).getMembers().contains(uuid)) {
                return i == 0 ? Team.TEAM_A : Team.TEAM_B;
            }
        }
        return Team.NONE;
    }

    public boolean isAlive(UUID uuid) {
        for (DuelTeam team : teams) {
            if (team.getAliveMembers().contains(uuid)) return true;
        }
        return false;
    }

    public List<UUID> getAllParticipants() {
        List<UUID> all = new ArrayList<>();
        teams.forEach(t -> all.addAll(t.getMembers()));
        return all;
    }

    public List<UUID> getAllAlive() {
        List<UUID> all = new ArrayList<>();
        teams.forEach(t -> all.addAll(t.getAliveMembers()));
        return all;
    }

    public long getDurationMillis() {
        if (startTime == 0) return 0;
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    public String getFormattedDuration() {
        long duration = getDurationMillis();
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
