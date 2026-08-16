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
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class UduelsCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public UduelsCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setlobby" -> handleSetLobby(player);
            case "lobby" -> handleLobby(player);
            case "reload" -> handleReload(player);
            case "setpos1" -> handleGatePos(player, true);
            case "setpos2" -> handleGatePos(player, false);
            case "gateinfo" -> handleGateInfo(player);
            case "update" -> handleUpdate(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleSetLobby(Player player) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        Location loc = player.getLocation();
        plugin.setLobbyLocation(loc);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("lobby.set",
                "%world%", loc.getWorld().getName(),
                "%x%", String.valueOf((int) loc.getX()),
                "%y%", String.valueOf((int) loc.getY()),
                "%z%", String.valueOf((int) loc.getZ()))));
    }

    private void handleLobby(Player player) {
        Location lobby = plugin.getLobbyLocation();
        if (lobby == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("lobby.not-set")));
            return;
        }
        player.teleport(lobby);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("lobby.teleported")));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        plugin.reloadPlugin();
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.reload")));
    }

    private void handleGatePos(Player player, boolean pos1) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (pos1) {
            plugin.getGateManager().setPos1(player);
        } else {
            plugin.getGateManager().setPos2(player);
        }
    }

    private void handleGateInfo(Player player) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        plugin.getGateManager().showInfo(player);
    }

    private void handleUpdate(Player player) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        plugin.getUpdateChecker().check(player);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fUpdraftDuels Admin Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels setlobby &7- Set lobby teleport location"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels lobby &7- Teleport to lobby"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels reload &7- Reload configuration"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels setpos1 &7- Set gate position 1"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels setpos2 &7- Set gate position 2"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels gateinfo &7- Show gate region"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/uduels update &7- Check for plugin updates"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("setlobby", "lobby", "reload", "setpos1", "setpos2", "gateinfo", "update"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).toList();
    }
}
