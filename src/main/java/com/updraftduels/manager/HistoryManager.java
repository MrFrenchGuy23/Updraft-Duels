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
import com.updraftduels.model.DuelHistoryEntry;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryManager {
    private final UpdraftDuels plugin;
    private final List<DuelHistoryEntry> recentHistory;
    private final Map<UUID, List<DuelHistoryEntry>> playerHistory;

    public HistoryManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.recentHistory = new CopyOnWriteArrayList<>();
        this.playerHistory = new ConcurrentHashMap<>();
    }

    public void recordDuel(DuelHistoryEntry entry) {
        recentHistory.add(entry);
        if (recentHistory.size() > 500) {
            recentHistory.remove(0);
        }

        addToPlayerHistory(entry.getWinnerUUID(), entry);
        addToPlayerHistory(entry.getLoserUUID(), entry);
    }

    private void addToPlayerHistory(UUID uuid, DuelHistoryEntry entry) {
        if (uuid == null) return;
        playerHistory.computeIfAbsent(uuid, k -> new CopyOnWriteArrayList<>()).add(entry);
        List<DuelHistoryEntry> list = playerHistory.get(uuid);
        if (list.size() > 100) {
            list.remove(0);
        }
    }

    public List<DuelHistoryEntry> getRecentHistory(int count) {
        int size = recentHistory.size();
        return recentHistory.subList(Math.max(0, size - count), size);
    }

    public List<DuelHistoryEntry> getPlayerHistory(UUID uuid, int count) {
        List<DuelHistoryEntry> history = playerHistory.getOrDefault(uuid, new ArrayList<>());
        int size = history.size();
        return history.subList(Math.max(0, size - count), size);
    }

    public List<DuelHistoryEntry> getPlayerHistory(UUID uuid) {
        return playerHistory.getOrDefault(uuid, new ArrayList<>());
    }

    public int getPlayerWinCount(UUID uuid) {
        return (int) getPlayerHistory(uuid).stream()
                .filter(e -> uuid.equals(e.getWinnerUUID())).count();
    }

    public int getPlayerLossCount(UUID uuid) {
        return (int) getPlayerHistory(uuid).stream()
                .filter(e -> uuid.equals(e.getLoserUUID())).count();
    }

    public List<DuelHistoryEntry> getHeadToHead(UUID player1, UUID player2) {
        return getPlayerHistory(player1).stream()
                .filter(e -> (player1.equals(e.getWinnerUUID()) && player2.equals(e.getLoserUUID()))
                        || (player2.equals(e.getWinnerUUID()) && player1.equals(e.getLoserUUID())))
                .toList();
    }

    public DuelHistoryEntry getLastDuel(UUID uuid) {
        List<DuelHistoryEntry> history = playerHistory.get(uuid);
        if (history == null || history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }
}
