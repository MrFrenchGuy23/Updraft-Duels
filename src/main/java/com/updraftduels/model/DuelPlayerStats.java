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

import java.util.UUID;

public class DuelPlayerStats {
    private final UUID uuid;
    private String name;
    private int wins;
    private int losses;
    private int kills;
    private int deaths;
    private int elo;
    private int winStreak;
    private int bestWinStreak;
    private int gamesPlayed;
    private long playtime;
    private String rankTier;

    public DuelPlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.elo = 1000;
        this.winStreak = 0;
        this.bestWinStreak = 0;
        this.gamesPlayed = 0;
        this.playtime = 0;
        this.rankTier = "Unranked";
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }
    public int getWinStreak() { return winStreak; }
    public void setWinStreak(int winStreak) { this.winStreak = winStreak; }
    public int getBestWinStreak() { return bestWinStreak; }
    public void setBestWinStreak(int bestWinStreak) { this.bestWinStreak = bestWinStreak; }
    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    public long getPlaytime() { return playtime; }
    public void setPlaytime(long playtime) { this.playtime = playtime; }
    public String getRankTier() { return rankTier; }
    public void setRankTier(String rankTier) { this.rankTier = rankTier; }

    public double getWinRate() {
        if (gamesPlayed == 0) return 0.0;
        return (double) wins / gamesPlayed * 100.0;
    }

    public void incrementWins() {
        this.wins++;
        this.winStreak++;
        this.gamesPlayed++;
        if (winStreak > bestWinStreak) {
            bestWinStreak = winStreak;
        }
    }

    public void incrementLosses() {
        this.losses++;
        this.winStreak = 0;
        this.gamesPlayed++;
    }

    public void incrementKills() { this.kills++; }

    public void incrementDeaths() { this.deaths++; }

    public void incrementGamesPlayed() { this.gamesPlayed++; }

    public void updateRankTier() {
        this.rankTier = com.updraftduels.UpdraftDuels.getInstance().getRankManager().getColoredRankForElo(this.elo);
    }
}
