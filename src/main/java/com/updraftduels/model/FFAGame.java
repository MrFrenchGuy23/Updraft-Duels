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
import java.util.concurrent.CopyOnWriteArrayList;

public class FFAGame {
    public enum State { WAITING, IN_PROGRESS, FINISHED }

    private final UUID id;
    private final String name;
    private final String arenaName;
    private final int maxPlayers;
    private final UUID creatorUUID;
    private State state;
    private final List<UUID> participants;
    private final List<UUID> alive;
    private UUID winner;
    private long startTime;

    public FFAGame(String name, String arenaName, int maxPlayers, UUID creatorUUID) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.arenaName = arenaName;
        this.maxPlayers = maxPlayers;
        this.creatorUUID = creatorUUID;
        this.state = State.WAITING;
        this.participants = new CopyOnWriteArrayList<>();
        this.alive = new CopyOnWriteArrayList<>();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getArenaName() { return arenaName; }
    public int getMaxPlayers() { return maxPlayers; }
    public UUID getCreatorUUID() { return creatorUUID; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public List<UUID> getParticipants() { return participants; }
    public List<UUID> getAlive() { return alive; }
    public int getAliveCount() { return alive.size(); }
    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public boolean addParticipant(UUID uuid) {
        if (participants.contains(uuid)) return false;
        participants.add(uuid);
        alive.add(uuid);
        return true;
    }

    public boolean removeParticipant(UUID uuid) {
        participants.remove(uuid);
        alive.remove(uuid);
        return true;
    }

    public void eliminate(UUID uuid) {
        alive.remove(uuid);
    }

    public List<UUID> getAllParticipants() {
        return new ArrayList<>(participants);
    }
}
