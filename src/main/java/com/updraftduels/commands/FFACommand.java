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
import com.updraftduels.manager.FFAManager;
import com.updraftduels.model.FFAGame;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class FFACommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public FFACommand(UpdraftDuels plugin) {
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
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "delete" -> handleDelete(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.ffa.create")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorizePrefix("&cUsage: /ffa create <name>"));
            return;
        }
        String name = args[1];
        int maxPlayers = plugin.getConfig().getInt("ffa.default-max-players", 10);
        if (args.length >= 3) {
            try {
                maxPlayers = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ColorUtil.colorizePrefix("&cInvalid number for max players."));
                return;
            }
        }

        com.updraftduels.model.Arena arena = plugin.getArenaManager().getRandomAvailableArena();
        if (arena == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cNo available arenas for FFA."));
            return;
        }

        FFAGame game = plugin.getFFAManager().createGame(name, arena.getName(), maxPlayers, player.getUniqueId());
        if (game != null) {
            plugin.getFFAManager().joinGame(game.getId(), player.getUniqueId());
            player.sendMessage(ColorUtil.colorizePrefix("&aFFA game &f" + name + "&a created! Joined automatically."));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cAn FFA with that name already exists."));
        }
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            List<FFAGame> openGames = plugin.getFFAManager().getOpenGames();
            if (openGames.isEmpty()) {
                player.sendMessage(ColorUtil.colorizePrefix("&cNo open FFA games. Create one with /ffa create <name>"));
                return;
            }
            joinGame(player, openGames.get(0).getId());
            return;
        }
        try {
            UUID gameId = UUID.fromString(args[1]);
            joinGame(player, gameId);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid FFA game ID."));
        }
    }

    private void joinGame(Player player, UUID gameId) {
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou are already in a duel!"));
            return;
        }
        boolean joined = plugin.getFFAManager().joinGame(gameId, player.getUniqueId());
        if (joined) {
            player.sendMessage(ColorUtil.colorizePrefix("&aYou joined the FFA game!"));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cCould not join. The game may be full or in progress."));
        }
    }

    private void handleLeave(Player player) {
        boolean left = plugin.getFFAManager().leaveGame(player.getUniqueId());
        if (left) {
            player.sendMessage(ColorUtil.colorizePrefix("&7You left the FFA game."));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cYou are not in an FFA game."));
        }
    }

    private void handleList(Player player) {
        Collection<FFAGame> games = plugin.getFFAManager().getAllGames();
        if (games.isEmpty()) {
            player.sendMessage(ColorUtil.colorizePrefix("&7No FFA games exist."));
            return;
        }
        player.sendMessage(ColorUtil.colorizePrefix("&fFFA Games:"));
        for (FFAGame game : games) {
            String status = game.getState() == FFAGame.State.WAITING ? "&aWaiting" :
                    game.getState() == FFAGame.State.IN_PROGRESS ? "&eIn Progress" : "&cFinished";
            player.sendMessage(ColorUtil.colorizePrefix("&7- &f" + game.getName() + " &7[" + status + "&7] &f"
                    + game.getAliveCount() + "/" + game.getMaxPlayers() + " &7ID: &f" + game.getId().toString().substring(0, 8)));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            FFAGame game = plugin.getFFAManager().getGameOf(player.getUniqueId());
            if (game == null) {
                player.sendMessage(ColorUtil.colorizePrefix("&cYou are not in an FFA game."));
                return;
            }
            showInfo(player, game);
            return;
        }
        try {
            UUID gameId = UUID.fromString(args[1]);
            FFAGame game = plugin.getFFAManager().getGame(gameId);
            if (game == null) {
                player.sendMessage(ColorUtil.colorizePrefix("&cFFA game not found."));
                return;
            }
            showInfo(player, game);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid FFA game ID."));
        }
    }

    private void showInfo(Player player, FFAGame game) {
        player.sendMessage(ColorUtil.colorizePrefix("&fFFA: " + game.getName()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Players: &f" + game.getAliveCount() + "/" + game.getMaxPlayers()));
        player.sendMessage(ColorUtil.colorizePrefix("&7State: &f" + game.getState().name()));
        player.sendMessage(ColorUtil.colorizePrefix("&7Arena: &f" + game.getArenaName()));
        if (!game.getAlive().isEmpty()) {
            player.sendMessage(ColorUtil.colorizePrefix("&7Alive:"));
            for (UUID uuid : game.getAlive()) {
                Player p = Bukkit.getPlayer(uuid);
                player.sendMessage(ColorUtil.colorizePrefix("  &f- " + (p != null ? p.getName() : uuid.toString().substring(0, 8))));
            }
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.ffa.create")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorizePrefix("&cUsage: /ffa delete <name>"));
            return;
        }
        boolean deleted = plugin.getFFAManager().deleteGame(args[1]);
        if (deleted) {
            player.sendMessage(ColorUtil.colorizePrefix("&aFFA game deleted."));
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cFFA game not found."));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fFFA Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa create <name> [max] &7- Create an FFA game"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa join [id] &7- Join an FFA game"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa leave &7- Leave current FFA"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa list &7- List all FFA games"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa info [id] &7- FFA game info"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/ffa delete <name> &7- Delete an FFA game"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("create", "join", "leave", "list", "info", "delete"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
