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

public class ScoreboardManager {
    private final UpdraftDuels plugin;
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
        if (obj != null) obj.unregister();

        Objective newObj = board.registerNewObjective("updraftduels_duel", Criteria.DUMMY,
                com.updraftduels.util.ColorUtil.colorize("&fDuel"));
        newObj.setDisplaySlot(DisplaySlot.SIDEBAR);
        final Objective sidebar = newObj;

        int line = 7;

        for (int i = 0; i < duel.getTeams().size(); i++) {
            var team = duel.getTeams().get(i);
            String teamName = i == 0 ? "&aTeam A" : "&cTeam B";
            sidebar.getScore(com.updraftduels.util.ColorUtil.colorize(teamName + " &7" + team.getAliveCount() + "/" + team.getSize()))
                    .setScore(line--);
        }

        sidebar.getScore(" ").setScore(line--);

        sidebar.getScore(com.updraftduels.util.ColorUtil.colorize("&7Arena: &f" + duel.getArenaName())).setScore(line--);
        sidebar.getScore(com.updraftduels.util.ColorUtil.colorize("&7Duration: &f" + duel.getFormattedDuration())).setScore(line--);
        sidebar.getScore(com.updraftduels.util.ColorUtil.colorize("&7Ruleset: &f" + (duel.getRulesetId() != null ? duel.getRulesetId() : "default"))).setScore(line--);

        sidebar.getScore("  ").setScore(line--);

        plugin.getDatabase().getOrCreateStats(player.getUniqueId(), player.getName()).thenAccept(stats -> {
            if (stats != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Objective current = player.getScoreboard().getObjective("updraftduels_duel");
                    if (current != sidebar) return;
                    try {
                        sidebar.getScore(com.updraftduels.util.ColorUtil.colorize("&7ELO: &f" + stats.getElo())).setScore(1);
                        sidebar.getScore(com.updraftduels.util.ColorUtil.colorize("&7W/L: &f" + stats.getWins() + "-" + stats.getLosses())).setScore(0);
                    } catch (IllegalStateException ignored) {
                    }
                });
            }
        });
    }

    public void removeScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board != null) {
            Objective obj = board.getObjective("updraftduels_duel");
            if (obj != null) obj.unregister();
        }
    }
}
