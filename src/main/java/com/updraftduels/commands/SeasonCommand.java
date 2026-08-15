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
import com.updraftduels.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class SeasonCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public SeasonCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            showSeasonInfo(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> showSeasonInfo(player);
            case "startnew" -> {
                if (!player.hasPermission("updraftduels.admin")) {
                    player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
                    return true;
                }
                plugin.getSeasonManager().startNewSeason();
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("season.new-season-started",
                        "%season%", String.valueOf(plugin.getSeasonManager().getCurrentSeason()))));
            }
            case "resetelo" -> {
                if (!player.hasPermission("updraftduels.admin")) {
                    player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
                    return true;
                }
                plugin.getSeasonManager().resetAllElo();
                player.sendMessage(ColorUtil.colorizePrefix("&aAll ELO ratings have been reset!"));
            }
            default -> showSeasonInfo(player);
        }
        return true;
    }

    private void showSeasonInfo(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fSeason " + plugin.getSeasonManager().getCurrentSeason()));
        player.sendMessage(ColorUtil.colorizePrefix("&eTime Remaining: &f" + plugin.getSeasonManager().getFormattedTimeRemaining()));
        player.sendMessage(ColorUtil.colorizePrefix("&eELO Decay: &f" + (plugin.getSeasonManager().isDecayEnabled() ? "&aEnabled" : "&cDisabled")));
        if (plugin.getSeasonManager().isDecayEnabled()) {
            player.sendMessage(ColorUtil.colorizePrefix("&7  Inactive after &f" + plugin.getSeasonManager().getDecayDaysInactive() + " &7days"));
            player.sendMessage(ColorUtil.colorizePrefix("&7  Decay amount: &f" + plugin.getSeasonManager().getDecayAmount() + " ELO"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("info"));
            if (sender.hasPermission("updraftduels.admin")) {
                opts.add("startnew");
                opts.add("resetelo");
            }
            return opts.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
