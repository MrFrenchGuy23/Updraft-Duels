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
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class GateManager {
    private final UpdraftDuels plugin;
    private Location pos1;
    private Location pos2;
    private Material material;
    private boolean active;
    private int closeCount;
    private boolean animationEnabled;
    private int animationSpeed;
    private final Map<Location, BlockData> originalBlocks = new HashMap<>();
    private int minX, maxX, minZ, maxZ, minY, maxY, animMaxY;
    private BukkitTask animationTask;
    private int shift;

    public GateManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();
        material = Material.matchMaterial(config.getString("gate.material", "IRON_BARS"));
        if (material == null) material = Material.IRON_BARS;
        animationEnabled = config.getBoolean("gate.animation-enabled", true);
        animationSpeed = Math.max(1, config.getInt("gate.animation-speed-ticks", 2));
        pos1 = loadLocation(config, "gate.pos1");
        pos2 = loadLocation(config, "gate.pos2");
    }

    public void setPos1(Player player) {
        pos1 = player.getLocation().clone();
        save();
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("gate.pos1-set",
                "%x%", String.valueOf(pos1.getBlockX()),
                "%y%", String.valueOf(pos1.getBlockY()),
                "%z%", String.valueOf(pos1.getBlockZ()))));
    }

    public void setPos2(Player player) {
        pos2 = player.getLocation().clone();
        save();
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("gate.pos2-set",
                "%x%", String.valueOf(pos2.getBlockX()),
                "%y%", String.valueOf(pos2.getBlockY()),
                "%z%", String.valueOf(pos2.getBlockZ()))));
    }

    public void showInfo(Player player) {
        if (!isConfigured()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("gate.not-set")));
            return;
        }
        String world = pos1.getWorld() != null ? pos1.getWorld().getName() : "?";
        player.sendMessage(ColorUtil.colorizePrefix(
                "&fGate &7(" + material.name() + ") &fin &f" + world
                        + " &7(" + pos1.getBlockX() + ", " + pos1.getBlockY() + ", " + pos1.getBlockZ() + ")"
                        + " -> (" + pos2.getBlockX() + ", " + pos2.getBlockY() + ", " + pos2.getBlockZ() + ")"));
    }

    public boolean isConfigured() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public void closeGate() {
        if (!plugin.getConfig().getBoolean("gate.enabled", true)) return;
        if (!isConfigured()) return;
        cancelAnimation();
        if (active) {
            closeCount++;
            return;
        }
        active = true;
        closeCount = 1;
        fillGate();
    }

    public void openGate() {
        openGate(null);
    }

    public void openGate(Runnable whenDone) {
        if (!active) {
            if (whenDone != null) whenDone.run();
            return;
        }
        closeCount--;
        if (closeCount > 0) {
            if (whenDone != null) whenDone.run();
            return;
        }
        active = false;
        if (animationEnabled) {
            animateRise(whenDone);
        } else {
            restoreGate();
            if (whenDone != null) whenDone.run();
        }
    }

    public void shutdown() {
        cancelAnimation();
    }

    private void fillGate() {
        originalBlocks.clear();
        World world = pos1.getWorld();
        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int gateHeight = maxY - minY + 1;
        animMaxY = Math.min(world.getMaxHeight() - 1, maxY + gateHeight);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= animMaxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    originalBlocks.put(new Location(world, x, y, z), block.getBlockData());
                }
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == material) continue;
                    block.setType(material, false);
                }
            }
        }
    }

    private void animateRise(Runnable whenDone) {
        World world = pos1.getWorld();
        int gateHeight = maxY - minY + 1;
        if (animMaxY <= maxY) {
            restoreGate();
            if (whenDone != null) whenDone.run();
            return;
        }
        shift = 0;
        Runnable task = () -> {
            shift++;
            boolean done = shift >= gateHeight;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int exitY = minY + shift - 1;
                    if (exitY >= minY) {
                        restoreColumn(world, x, exitY, z);
                    }
                    int topY = maxY + shift;
                    if (topY <= animMaxY) {
                        world.getBlockAt(x, topY, z).setType(material, false);
                    }
                }
            }
            if (done) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY + shift; y <= maxY + shift && y <= animMaxY; y++) {
                            restoreColumn(world, x, y, z);
                        }
                    }
                }
                animationTask.cancel();
                animationTask = null;
                originalBlocks.clear();
                if (whenDone != null) whenDone.run();
            }
        };
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, task, 0L, animationSpeed);
    }

    private void cancelAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
            restoreGate();
        }
    }

    private void restoreColumn(World world, int x, int y, int z) {
        BlockData data = originalBlocks.get(new Location(world, x, y, z));
        if (data != null) {
            world.getBlockAt(x, y, z).setBlockData(data, false);
        } else {
            world.getBlockAt(x, y, z).setType(Material.AIR, false);
        }
    }

    private void restoreGate() {
        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            block.setBlockData(entry.getValue(), false);
        }
        originalBlocks.clear();
    }

    private void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("gate.pos1", serializeLocation(pos1));
        config.set("gate.pos2", serializeLocation(pos2));
        plugin.saveConfig();
    }

    private Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "");
        map.put("x", loc.getBlockX());
        map.put("y", loc.getBlockY());
        map.put("z", loc.getBlockZ());
        return map;
    }

    private Location loadLocation(FileConfiguration config, String path) {
        if (!config.isConfigurationSection(path)) return null;
        String worldName = config.getString(path + ".world");
        if (worldName == null || worldName.isEmpty()) return null;
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;
        return new Location(world, config.getInt(path + ".x"), config.getInt(path + ".y"), config.getInt(path + ".z"));
    }
}
