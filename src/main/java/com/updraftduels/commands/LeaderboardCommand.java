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
package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.DuelPlayerStats;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public LeaderboardCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        String statType = "kills";
        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("playtime") || arg.equals("deaths") || arg.equals("kills")) {
                statType = arg;
            } else if (arg.equals("gui")) {
                plugin.getGuiManager().openLeaderboardGUI(player);
                return true;
            } else {
                player.sendMessage(ColorUtil.colorizePrefix("&cUsage: /leaderboard [kills|deaths|playtime]"));
                return true;
            }
        }

        if (args.length == 0) {
            plugin.getGuiManager().openLeaderboardGUI(player);
            return true;
        }

        plugin.getGuiManager().openLeaderboardCategoryGUI(player, statType);
        return true;
    }

    public void sendLeaderboard(Player player, String statType) {
        String column = switch (statType) {
            case "deaths" -> "deaths";
            case "playtime" -> "playtime";
            default -> "kills";
        };

        String title = switch (column) {
            case "deaths" -> "&fDeaths Leaderboard";
            case "playtime" -> "&fPlaytime Leaderboard";
            default -> "&fKills Leaderboard";
        };

        plugin.getDatabase().getTopPlayersByStat(column, 10).thenAccept(topPlayers -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.colorizePrefix("&7- " + title + " -"));
                if (topPlayers.isEmpty()) {
                    player.sendMessage(ColorUtil.colorizePrefix("&cNo players have played yet."));
                    return;
                }
                int place = 1;
                for (DuelPlayerStats stats : topPlayers) {
                    String rankColor = place == 1 ? "&6" : place == 2 ? "&f" : place == 3 ? "&c" : "&e";
                    String value = switch (column) {
                        case "deaths" -> "&c" + stats.getDeaths() + " deaths";
                        case "playtime" -> "&e" + formatPlaytime(stats.getPlaytime());
                        default -> "&a" + stats.getKills() + " kills";
                    };
                    player.sendMessage(ColorUtil.colorizePrefix(rankColor + "#" + place + " &f" + stats.getName() + " &8- " + value));
                    place++;
                }
            });
        });
    }

    private String formatPlaytime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("kills", "deaths", "playtime", "gui"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
