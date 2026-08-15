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
import org.bukkit.World;

import java.util.*;

public class Arena {
    private final String name;
    private Location pos1;
    private Location pos2;
    private final Map<Team, Location> spawns;
    private final List<BlockSnapshot> originalBlocks;
    private boolean inUse;
    private boolean regenerating;
    private UUID currentDuelId;

    public Arena(String name) {
        this.name = name;
        this.spawns = new EnumMap<>(Team.class);
        this.originalBlocks = new ArrayList<>();
        this.inUse = false;
        this.regenerating = false;
        this.currentDuelId = null;
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
    public Map<Team, Location> getSpawns() { return spawns; }
    public void setSpawn(Team team, Location loc) { spawns.put(team, loc); }
    public Location getSpawn(Team team) { return spawns.get(team); }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
    public boolean isRegenerating() { return regenerating; }
    public void setRegenerating(boolean regenerating) { this.regenerating = regenerating; }
    public UUID getCurrentDuelId() { return currentDuelId; }
    public void setCurrentDuelId(UUID currentDuelId) { this.currentDuelId = currentDuelId; }
    public List<BlockSnapshot> getOriginalBlocks() { return originalBlocks; }

    public boolean isConfigured() {
        return pos1 != null && pos2 != null && !spawns.isEmpty();
    }

    public World getWorld() {
        return pos1 != null ? pos1.getWorld() : null;
    }

    public Location getCenter() {
        if (pos1 == null || pos2 == null) return null;
        return new Location(pos1.getWorld(),
                (pos1.getX() + pos2.getX()) / 2,
                (pos1.getY() + pos2.getY()) / 2,
                (pos1.getZ() + pos2.getZ()) / 2);
    }

    public boolean containsLocation(Location loc) {
        if (pos1 == null || pos2 == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public int getXSize() {
        return (int) Math.abs(pos1.getX() - pos2.getX());
    }

    public int getYSize() {
        return (int) Math.abs(pos1.getY() - pos2.getY());
    }

    public int getZSize() {
        return (int) Math.abs(pos1.getZ() - pos2.getZ());
    }

    public static class BlockSnapshot {
        private final int x, y, z;
        private final org.bukkit.block.data.BlockData blockData;
        private final org.bukkit.Material material;

        public BlockSnapshot(int x, int y, int z, org.bukkit.block.data.BlockData blockData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockData = blockData;
            this.material = blockData.getMaterial();
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public org.bukkit.block.data.BlockData getBlockData() { return blockData; }
        public org.bukkit.Material getMaterial() { return material; }
    }
}
