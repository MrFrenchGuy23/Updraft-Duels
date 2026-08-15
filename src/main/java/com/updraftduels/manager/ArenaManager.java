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
package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private static final int BLOCKS_PER_TICK = 4096;

    private final UpdraftDuels plugin;
    private final Map<String, Arena> arenas;

    public ArenaManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.arenas = new ConcurrentHashMap<>();
    }

    public void loadArenas() {
        plugin.getDatabase().loadArenas().thenAccept(arenaDataList -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map<String, Object> data : arenaDataList) {
                    Arena arena = new Arena((String) data.get("name"));
                    String worldName = (String) data.get("world");
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    arena.setPos1(new Location(world, (double) data.get("pos1_x"), (double) data.get("pos1_y"), (double) data.get("pos1_z")));
                    arena.setPos2(new Location(world, (double) data.get("pos2_x"), (double) data.get("pos2_y"), (double) data.get("pos2_z")));

                    Location spawnA = new Location(world, (double) data.get("spawn_a_x"), (double) data.get("spawn_a_y"), (double) data.get("spawn_a_z"));
                    spawnA.setYaw(((Float) data.get("spawn_a_yaw")));
                    arena.setSpawn(com.updraftduels.model.Team.TEAM_A, spawnA);

                    Location spawnB = new Location(world, (double) data.get("spawn_b_x"), (double) data.get("spawn_b_y"), (double) data.get("spawn_b_z"));
                    spawnB.setYaw(((Float) data.get("spawn_b_yaw")));
                    arena.setSpawn(com.updraftduels.model.Team.TEAM_B, spawnB);

                    arena.setRegenerating(true);
                    arenas.put(arena.getName(), arena);
                    snapshotArenaAsync(arena, () -> arena.setRegenerating(false));
                }
                plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
            });
        });
    }

    public boolean createArena(String name) {
        if (arenas.containsKey(name)) return false;
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        return true;
    }

    public boolean deleteArena(String name) {
        Arena removed = arenas.remove(name);
        if (removed != null) {
            plugin.getDatabase().deleteArena(name);
            return true;
        }
        return false;
    }

    public Arena getArena(String name) {
        if (name == null) return null;
        Arena arena = arenas.get(name);
        if (arena != null) return arena;
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
        }
        return null;
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public Arena getRandomAvailableArena() {
        List<Arena> available = arenas.values().stream()
                .filter(a -> a.isConfigured() && !a.isInUse() && !a.isRegenerating())
                .toList();
        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    public Arena getRandomAvailableArenaForGamemode(String gamemode) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
        if (config != null && config.contains(gamemode + ".arenas")) {
            List<String> assignedNames = config.getStringList(gamemode + ".arenas");
            List<Arena> assigned = new ArrayList<>();
            for (String name : assignedNames) {
                Arena arena = arenas.get(name);
                if (arena != null && arena.isConfigured() && !arena.isInUse() && !arena.isRegenerating()) {
                    assigned.add(arena);
                }
            }
            if (!assigned.isEmpty()) {
                return assigned.get(new Random().nextInt(assigned.size()));
            }
        }
        return getRandomAvailableArena();
    }

    public Arena getRandomAvailableArenaForSize(int teamSize) {
        List<Arena> available = arenas.values().stream()
                .filter(a -> a.isConfigured() && !a.isInUse() && !a.isRegenerating())
                .toList();
        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    public void saveArenaToDb(Arena arena) {
        if (arena.getPos1() == null || arena.getPos2() == null) return;

        String world = arena.getWorld() != null ? arena.getWorld().getName() : "world";
        Location sA = arena.getSpawn(com.updraftduels.model.Team.TEAM_A);
        Location sB = arena.getSpawn(com.updraftduels.model.Team.TEAM_B);

        double sAx = sA != null ? sA.getX() : 0;
        double sAy = sA != null ? sA.getY() : 0;
        double sAz = sA != null ? sA.getZ() : 0;
        float sAyaw = sA != null ? sA.getYaw() : 0;
        double sBx = sB != null ? sB.getX() : 0;
        double sBy = sB != null ? sB.getY() : 0;
        double sBz = sB != null ? sB.getZ() : 0;
        float sByaw = sB != null ? sB.getYaw() : 0;

        plugin.getDatabase().saveArena(arena.getName(), world,
                arena.getPos1().getX(), arena.getPos1().getY(), arena.getPos1().getZ(),
                arena.getPos2().getX(), arena.getPos2().getY(), arena.getPos2().getZ(),
                sAx, sAy, sAz, sAyaw,
                sBx, sBy, sBz, sByaw);
    }

    public void snapshotArena(Arena arena) {
        if (arena.getPos1() == null || arena.getPos2() == null) return;
        arena.getOriginalBlocks().clear();

        World world = arena.getWorld();
        if (world == null) return;

        int minX = (int) Math.min(arena.getPos1().getX(), arena.getPos2().getX());
        int minY = (int) Math.min(arena.getPos1().getY(), arena.getPos2().getY());
        int minZ = (int) Math.min(arena.getPos1().getZ(), arena.getPos2().getZ());
        int maxX = (int) Math.max(arena.getPos1().getX(), arena.getPos2().getX());
        int maxY = (int) Math.max(arena.getPos1().getY(), arena.getPos2().getY());
        int maxZ = (int) Math.max(arena.getPos1().getZ(), arena.getPos2().getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    arena.getOriginalBlocks().add(new Arena.BlockSnapshot(x, y, z, block.getBlockData()));
                }
            }
        }
    }

    public void snapshotArenaAsync(Arena arena, Runnable done) {
        if (arena.getPos1() == null || arena.getPos2() == null) {
            if (done != null) done.run();
            return;
        }
        World world = arena.getWorld();
        if (world == null) {
            if (done != null) done.run();
            return;
        }

        int minX = (int) Math.min(arena.getPos1().getX(), arena.getPos2().getX());
        int minY = (int) Math.min(arena.getPos1().getY(), arena.getPos2().getY());
        int minZ = (int) Math.min(arena.getPos1().getZ(), arena.getPos2().getZ());
        int maxX = (int) Math.max(arena.getPos1().getX(), arena.getPos2().getX());
        int maxY = (int) Math.max(arena.getPos1().getY(), arena.getPos2().getY());
        int maxZ = (int) Math.max(arena.getPos1().getZ(), arena.getPos2().getZ());

        arena.getOriginalBlocks().clear();

        int[] pos = {minX, minY, minZ};
        int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int processed = 0;
            while (processed < BLOCKS_PER_TICK && pos[0] <= maxX) {
                arena.getOriginalBlocks().add(new Arena.BlockSnapshot(
                        pos[0], pos[1], pos[2], world.getBlockAt(pos[0], pos[1], pos[2]).getBlockData()));
                processed++;
                pos[2]++;
                if (pos[2] > maxZ) {
                    pos[2] = minZ;
                    pos[1]++;
                }
                if (pos[1] > maxY) {
                    pos[1] = minY;
                    pos[0]++;
                }
            }
            if (pos[0] > maxX) {
                Bukkit.getScheduler().cancelTask(taskId[0]);
                if (done != null) done.run();
            }
        }, 1L, 1L);
    }

    public void regenerateArena(Arena arena) {
        World world = arena.getWorld();
        if (world == null || arena.getOriginalBlocks().isEmpty()) return;
        arena.setRegenerating(true);

        Iterator<Arena.BlockSnapshot> it = new ArrayList<>(arena.getOriginalBlocks()).iterator();
        int[] taskId = new int[1];
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int processed = 0;
            while (it.hasNext() && processed < BLOCKS_PER_TICK) {
                Arena.BlockSnapshot snap = it.next();
                world.getBlockAt(snap.getX(), snap.getY(), snap.getZ()).setBlockData(snap.getBlockData(), false);
                processed++;
            }
            if (!it.hasNext()) {
                Bukkit.getScheduler().cancelTask(taskId[0]);
                snapshotArenaAsync(arena, () -> arena.setRegenerating(false));
            }
        }, 1L, 1L);
    }

    public int getArenaCount() {
        return arenas.size();
    }
}
