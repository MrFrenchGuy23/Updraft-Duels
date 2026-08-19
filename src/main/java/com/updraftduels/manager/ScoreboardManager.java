package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelPlayerStats;
import com.updraftduels.model.DuelState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Set<String>> lastEntries = new ConcurrentHashMap<>();
    private int taskId = -1;

    public ScoreboardManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void startUpdating() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        taskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Duel duel : plugin.getDuelManager().getActiveDuels()) {
                    if (duel.getState() != DuelState.IN_PROGRESS) continue;
                    for (UUID uuid : duel.getAllParticipants()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) updateScoreboard(player, duel);
                    }
                    for (UUID uuid : duel.getSpectators()) {
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
        if (!plugin.isScoreboard(player.getUniqueId())) {
            removeScoreboard(player);
            return;
        }
        org.bukkit.scoreboard.ScoreboardManager sbManager = Bukkit.getScoreboardManager();
        if (sbManager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == null || board == sbManager.getMainScoreboard()) {
            board = sbManager.getNewScoreboard();
            player.setScoreboard(board);
        }

        String title = plugin.getConfig().getString("scoreboard.title", "&f&lDuel");
        Objective obj = board.getObjective("updraftduels_duel");
        if (obj == null) {
            obj = board.registerNewObjective("updraftduels_duel", Criteria.DUMMY,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(com.updraftduels.util.ColorUtil.colorize(title)));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(com.updraftduels.util.ColorUtil.colorize(title)));
        }
        final Objective objective = obj;

        List<String> templateLines = plugin.getConfig().getStringList("scoreboard.lines");
        if (templateLines.isEmpty()) {
            templateLines = Arrays.asList(
                    "%team_a% %team_b%",
                    "",
                    "&7Arena: &f%arena%",
                    "&7Duration: &f%duration%",
                    "&7Gamemode: &f%gamemode%",
                    "&7Ruleset: &f%ruleset%",
                    "",
                    "&7ELO: &f%elo%",
                    "&7W/L: &f%wl%"
            );
        }

        Map<String, String> placeholders = buildPlaceholders(duel, player);
        placeholders.put("%score%", duel.getScoreA() + " - " + duel.getScoreB());

        UUID uuid = player.getUniqueId();
        Set<String> previous = lastEntries.getOrDefault(uuid, Collections.emptySet());
        Set<String> next = new HashSet<>();

        int score = templateLines.size();
        for (String line : templateLines) {
            String resolved = replacePlaceholders(line, placeholders);
            String colored = com.updraftduels.util.ColorUtil.colorize(resolved);
            if (!colored.isEmpty()) {
                objective.getScore(colored).setScore(score);
                next.add(colored);
            }
            score--;
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
    }

    private Map<String, String> buildPlaceholders(Duel duel, Player player) {
        Map<String, String> p = new HashMap<>();
        p.put("%arena%", duel.getArenaName() != null ? duel.getArenaName() : "None");
        p.put("%duration%", duel.getFormattedDuration());
        p.put("%ruleset%", duel.getRulesetId() != null ? duel.getRulesetId() : "default");
        p.put("%gamemode%", duel.getType().name());

        StringBuilder teamA = new StringBuilder();
        StringBuilder teamB = new StringBuilder();
        for (int i = 0; i < duel.getTeams().size(); i++) {
            var team = duel.getTeams().get(i);
            StringBuilder sb = i == 0 ? teamA : teamB;
            sb.append(i == 0 ? "&a" : "&c").append("Team ").append(i == 0 ? "A" : "B");
            sb.append(" &7").append(team.getAliveCount()).append("/").append(team.getSize());
        }
        p.put("%team_a%", teamA.toString());
        p.put("%team_b%", teamB.toString());

        DuelPlayerStats stats = plugin.getDatabase().getCachedStats(player.getUniqueId());
        if (stats != null) {
            p.put("%elo%", String.valueOf(stats.getElo()));
            p.put("%wins%", String.valueOf(stats.getWins()));
            p.put("%losses%", String.valueOf(stats.getLosses()));
            p.put("%wl%", stats.getWins() + "-" + stats.getLosses());
            p.put("%kills%", String.valueOf(stats.getKills()));
            p.put("%deaths%", String.valueOf(stats.getDeaths()));
        } else {
            p.put("%elo%", "0");
            p.put("%wins%", "0");
            p.put("%losses%", "0");
            p.put("%wl%", "0-0");
            p.put("%kills%", "0");
            p.put("%deaths%", "0");
        }

        return p;
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public void removeScoreboard(Player player) {
        lastEntries.remove(player.getUniqueId());
        Scoreboard board = player.getScoreboard();
        if (board != null) {
            Objective obj = board.getObjective("updraftduels_duel");
            if (obj != null) obj.unregister();
        }
    }
}
