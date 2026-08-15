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

public class DuelHistoryEntry {
    private final UUID id;
    private final UUID duelId;
    private final String arenaName;
    private final String rulesetId;
    private final UUID winnerUUID;
    private final UUID loserUUID;
    private final String winnerName;
    private final String loserName;
    private final int winnerEloChange;
    private final int loserEloChange;
    private final long timestamp;
    private final long durationMillis;
    private final int winnerHealth;
    private final String deathCause;
    private final int round;

    public DuelHistoryEntry(UUID duelId, String arenaName, String rulesetId,
                            UUID winnerUUID, UUID loserUUID, String winnerName, String loserName,
                            int winnerEloChange, int loserEloChange, long durationMillis,
                            int winnerHealth, String deathCause, int round) {
        this.id = UUID.randomUUID();
        this.duelId = duelId;
        this.arenaName = arenaName;
        this.rulesetId = rulesetId;
        this.winnerUUID = winnerUUID;
        this.loserUUID = loserUUID;
        this.winnerName = winnerName;
        this.loserName = loserName;
        this.winnerEloChange = winnerEloChange;
        this.loserEloChange = loserEloChange;
        this.timestamp = System.currentTimeMillis();
        this.durationMillis = durationMillis;
        this.winnerHealth = winnerHealth;
        this.deathCause = deathCause;
        this.round = round;
    }

    public UUID getId() { return id; }
    public UUID getDuelId() { return duelId; }
    public String getArenaName() { return arenaName; }
    public String getRulesetId() { return rulesetId; }
    public UUID getWinnerUUID() { return winnerUUID; }
    public UUID getLoserUUID() { return loserUUID; }
    public String getWinnerName() { return winnerName; }
    public String getLoserName() { return loserName; }
    public int getWinnerEloChange() { return winnerEloChange; }
    public int getLoserEloChange() { return loserEloChange; }
    public long getTimestamp() { return timestamp; }
    public long getDurationMillis() { return durationMillis; }
    public int getWinnerHealth() { return winnerHealth; }
    public String getDeathCause() { return deathCause; }
    public int getRound() { return round; }

    public String getFormattedDuration() {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
