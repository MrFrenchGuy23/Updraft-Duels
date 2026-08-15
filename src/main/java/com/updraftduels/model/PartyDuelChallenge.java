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

import java.util.*;

public class PartyDuelChallenge {
    private final UUID challengeId;
    private final UUID challengerPartyId;
    private final UUID defenderPartyId;
    private final String arenaName;
    private final String rulesetId;
    private long createdAt;
    private boolean processed;

    public PartyDuelChallenge(UUID challengerPartyId, UUID defenderPartyId, String arenaName, String rulesetId) {
        this.challengeId = UUID.randomUUID();
        this.challengerPartyId = challengerPartyId;
        this.defenderPartyId = defenderPartyId;
        this.arenaName = arenaName;
        this.rulesetId = rulesetId;
        this.createdAt = System.currentTimeMillis();
        this.processed = false;
    }

    public UUID getChallengeId() { return challengeId; }
    public UUID getChallengerPartyId() { return challengerPartyId; }
    public UUID getDefenderPartyId() { return defenderPartyId; }
    public String getArenaName() { return arenaName; }
    public String getRulesetId() { return rulesetId; }
    public long getCreatedAt() { return createdAt; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 120_000;
    }
}
