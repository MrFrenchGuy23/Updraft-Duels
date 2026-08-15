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
import com.updraftduels.model.DuelPlayerStats;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SeasonManager {
    private final UpdraftDuels plugin;
    private int currentSeason;
    private long seasonStartTime;
    private long seasonEndTime;
    private boolean decayEnabled;
    private int decayDaysInactive;
    private int decayAmount;
    private final Map<UUID, Long> lastActiveMap;

    public SeasonManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.lastActiveMap = new ConcurrentHashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        currentSeason = plugin.getConfig().getInt("season.current-season", 1);
        decayEnabled = plugin.getConfig().getBoolean("season.decay-enabled", true);
        decayDaysInactive = plugin.getConfig().getInt("season.decay-days-inactive", 14);
        decayAmount = plugin.getConfig().getInt("season.decay-amount", 50);
        seasonStartTime = plugin.getConfig().getLong("season.start-time", System.currentTimeMillis());
        seasonEndTime = plugin.getConfig().getLong("season.end-time", 0);
    }

    public void recordActivity(UUID uuid) {
        lastActiveMap.put(uuid, System.currentTimeMillis());
    }

    public void runDecayCheck() {
        if (!decayEnabled) return;
        long now = System.currentTimeMillis();
        long decayThreshold = (long) decayDaysInactive * 24 * 60 * 60 * 1000;

        plugin.getDatabase().getTopPlayers(1000).thenAccept(statsList -> {
            for (DuelPlayerStats stats : statsList) {
                Long lastActive = lastActiveMap.get(stats.getUuid());
                if (lastActive == null) continue;
                long inactive = now - lastActive;

                if (inactive > decayThreshold && stats.getElo() > 100) {
                    int newElo = Math.max(100, stats.getElo() - decayAmount);
                    stats.setElo(newElo);
                    stats.updateRankTier();
                    plugin.getDatabase().saveStats(stats);
                }
            }
        });
    }

    public void startNewSeason() {
        currentSeason++;
        seasonStartTime = System.currentTimeMillis();
        int seasonLengthDays = plugin.getConfig().getInt("season.length-days", 30);
        seasonEndTime = seasonStartTime + (long) seasonLengthDays * 24 * 60 * 60 * 1000;
        List<UUID> trackedPlayers = new ArrayList<>(lastActiveMap.keySet());
        lastActiveMap.clear();

        plugin.getConfig().set("season.current-season", currentSeason);
        plugin.getConfig().set("season.start-time", seasonStartTime);
        plugin.getConfig().set("season.end-time", seasonEndTime);
        plugin.saveConfig();

        for (UUID uuid : trackedPlayers) {
            plugin.getDatabase().getOrCreateStats(uuid, null).thenAccept(stats -> {
                if (stats != null) {
                    plugin.getDatabase().getPlayerRank(uuid).thenAccept(rank -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                String msg = plugin.getMessages().get("season.new-season",
                                        "%season%", String.valueOf(currentSeason));
                                player.sendMessage(msg);
                            }
                        });
                    });
                }
            });
        }
    }

    public void resetAllElo() {
        plugin.getDatabase().getTopPlayers(Integer.MAX_VALUE).thenAccept(statsList -> {
            int defaultElo = plugin.getConfig().getInt("general.default-elo", 1000);
            for (DuelPlayerStats stats : statsList) {
                stats.setElo(defaultElo);
                stats.updateRankTier();
                plugin.getDatabase().saveStats(stats);
            }
        });
    }

    public int getCurrentSeason() { return currentSeason; }

    public void reload() {
        loadConfig();
    }
    public long getSeasonStartTime() { return seasonStartTime; }
    public long getSeasonEndTime() { return seasonEndTime; }
    public boolean isDecayEnabled() { return decayEnabled; }
    public int getDecayDaysInactive() { return decayDaysInactive; }
    public int getDecayAmount() { return decayAmount; }

    public long getSeasonTimeRemaining() {
        if (seasonEndTime <= 0) return -1;
        return Math.max(0, seasonEndTime - System.currentTimeMillis());
    }

    public String getFormattedTimeRemaining() {
        long remaining = getSeasonTimeRemaining();
        if (remaining < 0) return "N/A";
        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
        return days + "d " + hours + "h " + minutes + "m";
    }
}
