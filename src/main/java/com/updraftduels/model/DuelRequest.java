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

import org.bukkit.entity.Player;

import java.util.UUID;

public class DuelRequest {
    private final UUID requestId;
    private final UUID senderUUID;
    private final UUID receiverUUID;
    private final DuelType type;
    private final String rulesetId;
    private final int rounds;
    private long createdAt;
    private boolean accepted;
    private boolean processed;
    private boolean ranked;
    private String arenaName;

    public DuelRequest(UUID senderUUID, UUID receiverUUID, DuelType type, String rulesetId) {
        this(senderUUID, receiverUUID, type, rulesetId, false, 1);
    }

    public DuelRequest(UUID senderUUID, UUID receiverUUID, DuelType type, String rulesetId, boolean ranked) {
        this(senderUUID, receiverUUID, type, rulesetId, ranked, 1);
    }

    public DuelRequest(UUID senderUUID, UUID receiverUUID, DuelType type, String rulesetId, boolean ranked, int rounds) {
        this.requestId = UUID.randomUUID();
        this.senderUUID = senderUUID;
        this.receiverUUID = receiverUUID;
        this.type = type;
        this.rulesetId = rulesetId;
        this.ranked = ranked;
        this.rounds = rounds > 0 ? rounds : 1;
        this.createdAt = System.currentTimeMillis();
        this.accepted = false;
        this.processed = false;
    }

    public UUID getRequestId() { return requestId; }
    public UUID getSenderUUID() { return senderUUID; }
    public UUID getReceiverUUID() { return receiverUUID; }
    public DuelType getType() { return type; }
    public String getRulesetId() { return rulesetId; }
    public int getRounds() { return rounds; }
    public long getCreatedAt() { return createdAt; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public boolean isRanked() { return ranked; }
    public void setRanked(boolean ranked) { this.ranked = ranked; }

    public String getArenaName() { return arenaName; }
    public void setArenaName(String arenaName) { this.arenaName = arenaName; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 60_000;
    }
}
