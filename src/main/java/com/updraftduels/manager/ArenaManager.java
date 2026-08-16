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

    public List<Arena> getAvailableArenas() {
        return arenas.values().stream()
                .filter(a -> a.isConfigured() && !a.isInUse() && !a.isRegenerating())
                .toList();
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

        arena.getOriginalBlocks().clear();
        runChunkedArenaJob(arena, (w, x, y, z) ->
                arena.getOriginalBlocks().add(new Arena.BlockSnapshot(x, y, z, w.getBlockAt(x, y, z).getBlockData())), done);
    }

    public void regenerateArena(Arena arena) {
        World world = arena.getWorld();
        if (world == null || arena.getOriginalBlocks().isEmpty()) return;
        arena.setRegenerating(true);

        Map<Long, org.bukkit.block.data.BlockData> data = new HashMap<>();
        for (Arena.BlockSnapshot snap : arena.getOriginalBlocks()) {
            data.put(packCoord(snap.getX(), snap.getY(), snap.getZ()), snap.getBlockData());
        }

        runChunkedArenaJob(arena, (w, x, y, z) -> {
            org.bukkit.block.data.BlockData blockData = data.get(packCoord(x, y, z));
            if (blockData != null) {
                w.getBlockAt(x, y, z).setBlockData(blockData, false);
            }
        }, () -> snapshotArenaAsync(arena, () -> arena.setRegenerating(false)));
    }

    private long packCoord(int x, int y, int z) {
        return ((long) (x & 0xFFFFFF) << 40)
                | ((long) (z & 0xFFFFFF) << 16)
                | (y & 0xFFFF);
    }

    private interface ChunkBlockAction {
        void apply(World world, int x, int y, int z);
    }

    private void runChunkedArenaJob(Arena arena, ChunkBlockAction action, Runnable done) {
        World world = arena.getWorld();
        if (world == null || arena.getPos1() == null || arena.getPos2() == null) {
            if (done != null) done.run();
            return;
        }

        int minX = (int) Math.min(arena.getPos1().getX(), arena.getPos2().getX());
        int minY = (int) Math.min(arena.getPos1().getY(), arena.getPos2().getY());
        int minZ = (int) Math.min(arena.getPos1().getZ(), arena.getPos2().getZ());
        int maxX = (int) Math.max(arena.getPos1().getX(), arena.getPos2().getX());
        int maxY = (int) Math.max(arena.getPos1().getY(), arena.getPos2().getY());
        int maxZ = (int) Math.max(arena.getPos1().getZ(), arena.getPos2().getZ());

        List<int[]> chunks = new ArrayList<>();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                chunks.add(new int[]{cx, cz});
            }
        }

        int[] chunkIdx = {0};
        boolean[] loading = {false};
        boolean[] loaded = {false};
        int[] cur = {minX, minY, minZ};
        int[] taskId = new int[1];

        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (chunkIdx[0] >= chunks.size()) {
                Bukkit.getScheduler().cancelTask(taskId[0]);
                if (done != null) done.run();
                return;
            }

            int[] cc = chunks.get(chunkIdx[0]);
            int cMinX = Math.max(minX, cc[0] * 16);
            int cMaxX = Math.min(maxX, cc[0] * 16 + 15);
            int cMinZ = Math.max(minZ, cc[1] * 16);
            int cMaxZ = Math.min(maxZ, cc[1] * 16 + 15);

            if (!loaded[0]) {
                if (!loading[0]) {
                    loading[0] = true;
                    world.getChunkAtAsync(cc[0], cc[1]).whenComplete((chunk, ex) ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                cur[0] = cMinX;
                                cur[1] = minY;
                                cur[2] = cMinZ;
                                loaded[0] = true;
                            }));
                }
                return;
            }

            int processed = 0;
            while (processed < BLOCKS_PER_TICK) {
                action.apply(world, cur[0], cur[1], cur[2]);
                processed++;
                cur[2]++;
                if (cur[2] > cMaxZ) {
                    cur[2] = cMinZ;
                    cur[1]++;
                }
                if (cur[1] > maxY) {
                    cur[1] = minY;
                    cur[0]++;
                    if (cur[0] > cMaxX) {
                        chunkIdx[0]++;
                        loaded[0] = false;
                        loading[0] = false;
                        break;
                    }
                }
            }
        }, 1L, 1L);
    }

    public int getArenaCount() {
        return arenas.size();
    }
}
