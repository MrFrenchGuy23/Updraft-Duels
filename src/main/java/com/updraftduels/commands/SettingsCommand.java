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
import com.updraftduels.model.Ruleset;
import com.updraftduels.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SettingsCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public SettingsCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openSettingsGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "rules", "ruleset", "rulesets" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
                    handleSet(player, args);
                } else {
                    plugin.getGuiManager().openRulesetsGUI(player);
                }
            }
            case "set" -> handleSet(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        String rulesetId = args[args.length - 1].toLowerCase();
        if (args.length < 2 || args[args.length - 1].equalsIgnoreCase("set")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/settings set <ruleset>")));
            return;
        }
        if (!plugin.getRulesetManager().hasRuleset(rulesetId)) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("rules.not-found", "%ruleset%", args[args.length - 1])));
            return;
        }
        plugin.getRulesetManager().setSelectedRuleset(player.getUniqueId(), rulesetId);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("rules.selected", "%ruleset%", args[args.length - 1])));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fSettings Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/settings &7- Open the duel settings"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/settings rules &7- Open the ruleset selector"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/settings set <ruleset> &7- Select a ruleset"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("rules", "ruleset", "rulesets", "set"), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("set")) {
            return filter(List.of("set"), args[1]);
        }
        if (args.length == 2 || (args.length == 3 && args[1].equalsIgnoreCase("set"))) {
            return plugin.getRulesetManager().getAllRulesets().stream()
                    .map(Ruleset::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
