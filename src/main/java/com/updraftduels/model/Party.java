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

import java.util.*;

public class Party {
    private final UUID partyId;
    private UUID leaderUUID;
    private final List<UUID> members;
    private final List<UUID> invitees;
    private String name;
    private boolean readyCheckActive;
    private final Set<UUID> readyMembers;

    public Party(UUID leaderUUID) {
        this.partyId = UUID.randomUUID();
        this.leaderUUID = leaderUUID;
        this.members = new ArrayList<>();
        this.invitees = new ArrayList<>();
        this.readyCheckActive = false;
        this.readyMembers = new HashSet<>();
        this.members.add(leaderUUID);
    }

    public UUID getPartyId() { return partyId; }
    public UUID getLeaderUUID() { return leaderUUID; }
    public void setLeaderUUID(UUID leaderUUID) { this.leaderUUID = leaderUUID; }
    public List<UUID> getMembers() { return members; }
    public List<UUID> getInvitees() { return invitees; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isReadyCheckActive() { return readyCheckActive; }
    public void setReadyCheckActive(boolean readyCheckActive) { this.readyCheckActive = readyCheckActive; }
    public Set<UUID> getReadyMembers() { return readyMembers; }

    public boolean isLeader(UUID uuid) {
        return leaderUUID.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) {
            members.add(uuid);
            invitees.remove(uuid);
        }
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        readyMembers.remove(uuid);
    }

    public void addInvitee(UUID uuid) {
        if (!invitees.contains(uuid) && !members.contains(uuid)) {
            invitees.add(uuid);
        }
    }

    public void removeInvitee(UUID uuid) {
        invitees.remove(uuid);
    }

    public boolean isInvited(UUID uuid) {
        return invitees.contains(uuid);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isFull(int maxSize) {
        return members.size() >= maxSize;
    }

    public boolean isReadyCheckComplete() {
        return readyMembers.size() >= members.size();
    }

    public void clearReady() {
        readyMembers.clear();
        readyCheckActive = false;
    }
}
