package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.DuelPlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {
    private final UpdraftDuels plugin;
    private int taskId = -1;
    private final java.util.Map<UUID, String> cachedPrefixes = new ConcurrentHashMap<>();

    public NametagManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        if (taskId != -1) return;
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateNametag(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L).getTaskId();
    }

    public void stopUpdating() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        cachedPrefixes.clear();
    }

    public void updateNametag(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        String prefix = getFormattedPrefix(uuid);

        if (prefix.equals(cachedPrefixes.get(uuid))) return;
        cachedPrefixes.put(uuid, prefix);

        Scoreboard board = player.getScoreboard();
        if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        String teamName = "ud_rank_" + uuid.toString().substring(0, 8);
        org.bukkit.scoreboard.Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        String entry = player.getName();
        if (!team.hasEntry(entry)) {
            for (org.bukkit.scoreboard.Team t : board.getTeams()) {
                if (t.hasEntry(entry)) {
                    t.removeEntry(entry);
                }
            }
            team.addEntry(entry);
        }

        String colorPrefix = prefix;
        team.prefix(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(colorPrefix));
    }

    public void removeNametag(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        cachedPrefixes.remove(uuid);

        Scoreboard board = player.getScoreboard();
        if (board == null) return;

        String teamName = "ud_rank_" + uuid.toString().substring(0, 8);
        org.bukkit.scoreboard.Team team = board.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    private String getFormattedPrefix(UUID uuid) {
        DuelPlayerStats stats = plugin.getDatabase().getCachedStats(uuid);
        if (stats == null) {
            stats = new DuelPlayerStats(uuid, Bukkit.getOfflinePlayer(uuid).getName());
            stats.setElo(plugin.getConfig().getInt("general.default-elo", 1000));
        }

        String rank = plugin.getRankManager().getColoredRankForElo(stats.getElo());
        return rank + " &r";
    }

    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateNametag(player), 5L);
    }

    public void onPlayerQuit(Player player) {
        removeNametag(player);
    }
}
