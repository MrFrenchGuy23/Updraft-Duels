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
import java.util.stream.Collectors;

public class CosmeticsCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public CosmeticsCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openCosmeticsGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> plugin.getGuiManager().openCosmeticsGUI(player);
            case "kill" -> {
                if (args.length < 2) { sendHelp(player); return true; }
                plugin.getCosmeticsManager().setKillEffect(player.getUniqueId(), args[1]);
                player.sendMessage(ColorUtil.colorizePrefix("&aKill effect set to &f" + args[1]));
            }
            case "victory" -> {
                if (args.length < 2) { sendHelp(player); return true; }
                plugin.getCosmeticsManager().setVictoryAnimation(player.getUniqueId(), args[1]);
                player.sendMessage(ColorUtil.colorizePrefix("&aVictory animation set to &f" + args[1]));
            }
            case "trail" -> {
                if (args.length < 2) { sendHelp(player); return true; }
                plugin.getCosmeticsManager().setTrail(player.getUniqueId(), args[1]);
                player.sendMessage(ColorUtil.colorizePrefix("&aTrail set to &f" + args[1]));
            }
            case "deathmsg" -> {
                if (args.length < 2) { sendHelp(player); return true; }
                plugin.getCosmeticsManager().setDeathMessage(player.getUniqueId(), args[1]);
                player.sendMessage(ColorUtil.colorizePrefix("&aDeath message set to &f" + args[1]));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fCosmetics Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics &7- Open the cosmetics GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics gui &7- Open the cosmetics GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics kill <effect> &7- Set kill effect"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics victory <anim> &7- Set victory animation"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics trail <trail> &7- Set trail"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/cosmetics deathmsg <type> &7- Set death message"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("gui", "kill", "victory", "trail", "deathmsg"), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "kill" -> filter(plugin.getCosmeticsManager().getAvailableKillEffects(), args[1]);
                case "victory" -> filter(plugin.getCosmeticsManager().getAvailableVictoryAnimations(), args[1]);
                case "trail" -> filter(plugin.getCosmeticsManager().getAvailableTrails(), args[1]);
                case "deathmsg" -> filter(plugin.getCosmeticsManager().getAvailableDeathMessages(), args[1]);
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
