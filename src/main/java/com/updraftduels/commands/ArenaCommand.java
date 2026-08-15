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
import com.updraftduels.model.Arena;
import com.updraftduels.model.Team;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public ArenaCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("updraftduels.arena.manage")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "setpos1" -> handleSetPos(player, args, true);
            case "setpos2" -> handleSetPos(player, args, false);
            case "setspawn" -> handleSetSpawn(player, args);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/duelarena create <name>")));
            return;
        }
        String name = args[1];
        if (plugin.getArenaManager().createArena(name)) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.created", "%name%", name)));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cArena already exists."));
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/duelarena delete <name>")));
            return;
        }
        if (plugin.getArenaManager().deleteArena(args[1])) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.deleted", "%name%", args[1])));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.not-found", "%name%", args[1])));
        }
    }

    private void handleSetPos(Player player, String[] args, boolean pos1) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage",
                    "%usage%", "/duelarena setpos1|setpos2 <name>")));
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.not-found", "%name%", args[1])));
            return;
        }

        Location loc = player.getLocation();
        if (pos1) {
            arena.setPos1(loc);
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.pos1-set", "%name%", args[1])));
        } else {
            arena.setPos2(loc);
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.pos2-set", "%name%", args[1])));
        }

        plugin.getArenaManager().saveArenaToDb(arena);
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage",
                    "%usage%", "/duelarena setspawn <name> <a|b>")));
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.not-found", "%name%", args[1])));
            return;
        }

        String teamArg = args[2].toLowerCase();
        Team team = "a".equals(teamArg) ? Team.TEAM_A : "b".equals(teamArg) ? Team.TEAM_B : null;
        if (team == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cTeam must be 'a' or 'b'."));
            return;
        }

        arena.setSpawn(team, player.getLocation().clone());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.spawn-set",
                "%team%", team.name(), "%name%", args[1])));

        plugin.getArenaManager().saveArenaToDb(arena);
    }

    private void handleList(Player player) {
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
        if (arenas.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.no-arenas")));
            return;
        }
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.list-header")));
        for (Arena arena : arenas) {
            String status = arena.isInUse() ? "&cIn Use" : arena.isConfigured() ? "&aReady" : "&eIncomplete";
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.list-entry",
                    "%name%", arena.getName(), "%status%", status)));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage",
                    "%usage%", "/duelarena info <name>")));
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.not-found", "%name%", args[1])));
            return;
        }
        player.sendMessage(ColorUtil.colorizePrefix("&6Arena: &f" + arena.getName()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Configured: " + (arena.isConfigured() ? "&aYes" : "&cNo")));
        player.sendMessage(ColorUtil.colorizePrefix("&7In Use: " + (arena.isInUse() ? "&cYes" : "&aNo")));
        player.sendMessage(ColorUtil.colorizePrefix("&7Spawns: A=" + (arena.getSpawn(Team.TEAM_A) != null) + " B=" + (arena.getSpawn(Team.TEAM_B) != null)));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fArena Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena create <name>"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena delete <name>"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena setpos1 <name>"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena setpos2 <name>"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena setspawn <name> <a|b>"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena list"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/duelarena info <name>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "delete", "setpos1", "setpos2", "setspawn", "list", "info"), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            return plugin.getArenaManager().getAllArenas().stream()
                    .map(Arena::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(List.of("a", "b"), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
