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

public class PendingDuelSelection {
    private final UUID sender;
    private final UUID target;
    private String kitName;
    private int rounds;
    private boolean awaitingChat;
    private boolean personalKit;

    public PendingDuelSelection(UUID sender, UUID target) {
        this.sender = sender;
        this.target = target;
        this.rounds = 1;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public String getKitName() { return kitName; }
    public void setKitName(String kitName) { this.kitName = kitName; }
    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }
    public boolean isAwaitingChat() { return awaitingChat; }
    public void setAwaitingChat(boolean awaitingChat) { this.awaitingChat = awaitingChat; }
    public boolean isPersonalKit() { return personalKit; }
    public void setPersonalKit(boolean personalKit) { this.personalKit = personalKit; }
}
