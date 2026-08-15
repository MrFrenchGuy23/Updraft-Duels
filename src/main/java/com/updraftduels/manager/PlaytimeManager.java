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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeManager implements Listener {
    private final UpdraftDuels plugin;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final Map<UUID, String> sessionNames = new ConcurrentHashMap<>();

    public PlaytimeManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void startTracking(Player player) {
        UUID uuid = player.getUniqueId();
        sessionStart.put(uuid, System.currentTimeMillis());
        sessionNames.put(uuid, player.getName());
    }

    public void stopTrackingAndSave(UUID uuid) {
        Long start = sessionStart.remove(uuid);
        String name = sessionNames.remove(uuid);
        if (start == null) return;
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed < 0) return;

        plugin.getDatabase().getOrCreateStats(uuid, name).thenAccept(stats -> {
            if (stats != null && !sessionStart.containsKey(uuid)) {
                stats.setPlaytime(stats.getPlaytime() + elapsed);
                plugin.getDatabase().saveStats(stats);
            }
        });
    }

    public void saveAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopTrackingAndSave(player.getUniqueId());
        }
    }

    public long getSessionTime(UUID uuid) {
        Long start = sessionStart.get(uuid);
        if (start == null) return 0;
        return System.currentTimeMillis() - start;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startTracking(player);
        plugin.getFriendManager().loadFriends(player.getUniqueId());
        plugin.getDatabase().getOrCreateStats(player.getUniqueId(), player.getName());
        plugin.getDuelManager().restoreOfflinePlayer(player);
        plugin.getFFAManager().restoreSavedState(player);
        plugin.getDatabase().loadCosmetics(player.getUniqueId(), plugin.getCosmeticsManager());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopTrackingAndSave(event.getPlayer().getUniqueId());
    }
}
