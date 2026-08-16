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
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelPlayerStats;
import com.updraftduels.model.DuelState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Set<String>> lastEntries = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastStatsLines = new ConcurrentHashMap<>();
    private int taskId = -1;

    public ScoreboardManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Duel duel : plugin.getDuelManager().getActiveDuels()) {
                    if (duel.getState() != DuelState.IN_PROGRESS) continue;
                    for (java.util.UUID uuid : duel.getAllParticipants()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) updateScoreboard(player, duel);
                    }
                    for (java.util.UUID uuid : duel.getSpectators()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) updateScoreboard(player, duel);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    public void stopUpdating() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void updateScoreboard(Player player, Duel duel) {
        org.bukkit.scoreboard.ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == null || board == sbManager.getMainScoreboard()) {
            board = sbManager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("updraftduels_duel");
        if (obj == null) {
            obj = board.registerNewObjective("updraftduels_duel", Criteria.DUMMY,
                    com.updraftduels.util.ColorUtil.colorize("&fDuel"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        final Objective objective = obj;

        LinkedHashMap<String, Integer> rows = new LinkedHashMap<>();
        int line = 7;

        for (int i = 0; i < duel.getTeams().size(); i++) {
            var team = duel.getTeams().get(i);
            String teamName = i == 0 ? "&aTeam A" : "&cTeam B";
            rows.put(com.updraftduels.util.ColorUtil.colorize(teamName + " &7" + team.getAliveCount() + "/" + team.getSize()), line--);
        }

        rows.put(" ", line--);
        rows.put(com.updraftduels.util.ColorUtil.colorize("&7Arena: &f" + duel.getArenaName()), line--);
        rows.put(com.updraftduels.util.ColorUtil.colorize("&7Duration: &f" + duel.getFormattedDuration()), line--);
        rows.put(com.updraftduels.util.ColorUtil.colorize("&7Ruleset: &f" + (duel.getRulesetId() != null ? duel.getRulesetId() : "default")), line--);
        rows.put("  ", line--);

        UUID uuid = player.getUniqueId();
        Set<String> previous = lastEntries.getOrDefault(uuid, Collections.emptySet());
        Set<String> next = new HashSet<>();
        for (Map.Entry<String, Integer> entry : rows.entrySet()) {
            objective.getScore(entry.getKey()).setScore(entry.getValue());
            next.add(entry.getKey());
        }
        for (String key : previous) {
            if (!next.contains(key)) {
                try {
                    board.resetScores(key);
                } catch (IllegalStateException ignored) {
                }
            }
        }
        lastEntries.put(uuid, next);

        updateStatsLine(uuid, objective);
    }

    private void updateStatsLine(UUID uuid, Objective objective) {
        DuelPlayerStats cached = plugin.getDatabase().getCachedStats(uuid);
        if (cached == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            plugin.getDatabase().getOrCreateStats(uuid, player.getName()).thenAccept(stats -> {
                if (stats != null) {
                    applyStatsLine(uuid, stats.getElo(), stats.getWins(), stats.getLosses(), objective);
                }
            });
            return;
        }
        applyStatsLine(uuid, cached.getElo(), cached.getWins(), cached.getLosses(), objective);
    }

    private void applyStatsLine(UUID uuid, int elo, int wins, int losses, Objective objective) {
        String statsKey = elo + "|" + wins + "|" + losses;
        if (statsKey.equals(lastStatsLines.get(uuid))) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;
            Objective current = player.getScoreboard().getObjective("updraftduels_duel");
            if (current != objective) return;
            try {
                objective.getScore(com.updraftduels.util.ColorUtil.colorize("&7ELO: &f" + elo)).setScore(1);
                objective.getScore(com.updraftduels.util.ColorUtil.colorize("&7W/L: &f" + wins + "-" + losses)).setScore(0);
            } catch (IllegalStateException ignored) {
            }
        });
        lastStatsLines.put(uuid, statsKey);
    }

    public void removeScoreboard(Player player) {
        lastEntries.remove(player.getUniqueId());
        lastStatsLines.remove(player.getUniqueId());
        Scoreboard board = player.getScoreboard();
        if (board != null) {
            Objective obj = board.getObjective("updraftduels_duel");
            if (obj != null) obj.unregister();
        }
    }
}
