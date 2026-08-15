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

public enum DuelType {
    SOLO(1),
    DUO(2),
    TRIO(3),
    QUAD(4),
    FFA(0);

    private final int teamSize;

    DuelType(int teamSize) {
        this.teamSize = teamSize;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public boolean isTeamDuel() {
        return this != FFA;
    }

    public boolean isFFA() {
        return this == FFA;
    }

    public static DuelType fromTeamSize(int size) {
        for (DuelType type : values()) {
            if (type.teamSize == size) return type;
        }
        return null;
    }
}
