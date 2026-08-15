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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DuelTeam {
    private final Team team;
    private final List<UUID> members;
    private final List<UUID> aliveMembers;

    public DuelTeam(Team team) {
        this.team = team;
        this.members = new ArrayList<>();
        this.aliveMembers = new ArrayList<>();
    }

    public Team getTeam() { return team; }
    public List<UUID> getMembers() { return members; }
    public List<UUID> getAliveMembers() { return aliveMembers; }

    public void addMember(UUID uuid) {
        members.add(uuid);
        aliveMembers.add(uuid);
    }

    public void eliminate(UUID uuid) {
        aliveMembers.remove(uuid);
    }

    public void respawn(UUID uuid) {
        if (members.contains(uuid) && !aliveMembers.contains(uuid)) {
            aliveMembers.add(uuid);
        }
    }

    public boolean isEliminated() {
        return aliveMembers.isEmpty();
    }

    public void resetAlive() {
        aliveMembers.clear();
        aliveMembers.addAll(members);
    }

    public int getSize() {
        return members.size();
    }

    public int getAliveCount() {
        return aliveMembers.size();
    }
}
