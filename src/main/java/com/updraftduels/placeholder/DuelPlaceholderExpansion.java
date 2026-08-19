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
package com.updraftduels.placeholder;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelPlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DuelPlaceholderExpansion extends PlaceholderExpansion {
    private final UpdraftDuels plugin;

    public DuelPlaceholderExpansion(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "updraftduels";
    }

    @Override
    public @NotNull String getAuthor() {
        return "UpdraftDuels";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return getFallback();

        UUID uuid = player.getUniqueId();

        if (identifier.equalsIgnoreCase("team")) {
            Duel duel = plugin.getDuelManager().getDuelOf(uuid);
            if (duel == null) return "NONE";
            return switch (duel.getTeamIndex(uuid)) {
                case TEAM_A -> "TEAM_A";
                case TEAM_B -> "TEAM_B";
                default -> "NONE";
            };
        }

        DuelPlayerStats stats = plugin.getDatabase().getCachedStats(uuid);
        if (stats == null) {
            plugin.getDatabase().getOrCreateStats(uuid, player.getName());
            stats = new DuelPlayerStats(uuid, player.getName());
            stats.setElo(plugin.getConfig().getInt("general.default-elo", 1000));
            stats.updateRankTier();
        }

        return switch (identifier.toLowerCase()) {
            case "wins" -> String.valueOf(stats.getWins());
            case "losses" -> String.valueOf(stats.getLosses());
            case "elo" -> String.valueOf(stats.getElo());
            case "rank" -> {
                String tier = stats.getRankTier();
                tier = tier.replaceAll("&[0-9a-fk-or]", "");
                yield tier;
            }
            case "coloredrank" -> stats.getRankTier();
            case "kills" -> String.valueOf(stats.getKills());
            case "deaths" -> String.valueOf(stats.getDeaths());
            case "winstreak" -> String.valueOf(stats.getWinStreak());
            case "bestwinstreak", "best_winstreak" -> String.valueOf(stats.getBestWinStreak());
            case "winrate", "win_rate" -> String.format("%.1f", stats.getWinRate());
            case "gamesplayed", "games_played" -> String.valueOf(stats.getGamesPlayed());
            case "winrank", "division" -> {
                var info = plugin.getRankManager().getDivision(stats.getGamesPlayed());
                yield info.getColor() + info.getName();
            }
            case "winrankprogress", "divisionprogress" -> {
                var info = plugin.getRankManager().getDivision(stats.getGamesPlayed());
                if (info.isMaxed()) yield "MAXED";
                yield info.getMatchesIntoDivision() + "/" + info.getMatchesNeeded();
            }
            case "winrankbar", "divisionbar" -> {
                var info = plugin.getRankManager().getDivision(stats.getGamesPlayed());
                if (info.isMaxed()) yield "MAXED";
                yield plugin.getRankManager().getProgressBar(info.getMatchesIntoDivision(), info.getMatchesNeeded());
            }
            case "duel_duration" -> {
                Duel duel = plugin.getDuelManager().getDuelOf(uuid);
                if (duel != null) yield duel.getFormattedDuration();
                yield "0:00";
            }
            case "duel_score" -> {
                Duel duel = plugin.getDuelManager().getDuelOf(uuid);
                if (duel != null) yield duel.getScoreA() + " - " + duel.getScoreB();
                yield "0 - 0";
            }
            case "duel_opponent" -> {
                String ctx = plugin.getDuelManager().getDuelContext(uuid);
                if (ctx != null) yield ctx;
                yield "None";
            }
            case "duel_arena" -> {
                Duel duel = plugin.getDuelManager().getDuelOf(uuid);
                if (duel != null && duel.getArenaName() != null) yield duel.getArenaName();
                yield "None";
            }
            default -> getFallback();
        };
    }

    private String getFallback() {
        return plugin.getConfig().getString("placeholder-api.no-data", "N/A");
    }
}
