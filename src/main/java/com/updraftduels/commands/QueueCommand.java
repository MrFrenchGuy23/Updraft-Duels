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
import com.updraftduels.manager.QueueManager;
import com.updraftduels.model.Arena;
import com.updraftduels.model.Kit;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class QueueCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public QueueCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (label.equalsIgnoreCase("leave")) {
            handleLeave(player);
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openQueueGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "leave" -> handleLeave(player);
            case "casual" -> {
                if (args.length == 1) {
                    handleMatchmakingMode(player, QueueManager.MatchmakingMode.CASUAL);
                } else {
                    joinGamemode(player, args[1], QueueManager.MatchmakingMode.CASUAL);
                }
            }
            case "competitive" -> {
                if (args.length == 1) {
                    handleMatchmakingMode(player, QueueManager.MatchmakingMode.COMPETITIVE);
                } else {
                    joinGamemode(player, args[1], QueueManager.MatchmakingMode.COMPETITIVE);
                }
            }
            case "both" -> {
                if (args.length == 1) {
                    handleMatchmakingMode(player, QueueManager.MatchmakingMode.BOTH);
                } else {
                    joinGamemode(player, args[1], QueueManager.MatchmakingMode.BOTH);
                }
            }
            case "ranked" -> {
                if (args.length == 1) {
                    handleMatchmakingMode(player, QueueManager.MatchmakingMode.COMPETITIVE);
                } else {
                    joinGamemode(player, args[1], QueueManager.MatchmakingMode.COMPETITIVE);
                }
            }
            case "unranked" -> {
                if (args.length == 1) {
                    handleMatchmakingMode(player, QueueManager.MatchmakingMode.CASUAL);
                } else {
                    joinGamemode(player, args[1], QueueManager.MatchmakingMode.CASUAL);
                }
            }
            case "add" -> handleAddGamemode(player, args);
            case "remove" -> handleRemoveGamemode(player, args);
            case "create" -> handleCreate(player, args);
            case "join" -> handleJoin(player, args);
            case "info" -> handleInfo(player, args);
            default -> {
                if (args.length >= 2) {
                    QueueManager.MatchmakingMode mode = parseMode(args[1]);
                    if (mode != null) {
                        joinGamemode(player, args[0], mode);
                    } else {
                        sendHelp(player);
                    }
                } else {
                    joinGamemode(player, args[0], plugin.getQueueManager().getMatchmakingMode(player.getUniqueId()));
                }
            }
        }
        return true;
    }

    private QueueManager.MatchmakingMode parseMode(String input) {
        return switch (input.toLowerCase()) {
            case "casual", "unranked" -> QueueManager.MatchmakingMode.CASUAL;
            case "competitive", "ranked" -> QueueManager.MatchmakingMode.COMPETITIVE;
            case "both" -> QueueManager.MatchmakingMode.BOTH;
            default -> null;
        };
    }

    private void handleLeave(Player player) {
        if (plugin.getQueueManager().leaveQueue(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.left")));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.not-in-queue")));
        }
    }

    private void handleMatchmakingMode(Player player, QueueManager.MatchmakingMode mode) {
        plugin.getQueueManager().setMatchmakingMode(player.getUniqueId(), mode);
        String color = switch (mode) {
            case CASUAL -> "&7Casual";
            case COMPETITIVE -> "&6Competitive";
            case BOTH -> "&bBoth";
        };
        player.sendMessage(ColorUtil.colorizePrefix("&7Matchmaking mode set to " + color));
    }

    public void joinGamemode(Player player, String name, QueueManager.MatchmakingMode mode) {
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
            return;
        }
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.already-in-queue")));
            return;
        }

        String kitName = resolveKitName(name);
        Runnable join = () -> {
            if (plugin.getQueueManager().joinGamemodeQueue(player.getUniqueId(), kitName, mode)) {
                player.sendMessage(plugin.getMessages().get("queue.joined-gamemode", "%gamemode_queue%", name));
            } else {
                player.sendMessage(ColorUtil.colorizePrefix("&cPlease wait a moment before joining a queue."));
            }
        };

        if (mode == QueueManager.MatchmakingMode.COMPETITIVE) {
            plugin.requireWins(player, 15, join);
        } else {
            join.run();
        }
    }

    private String resolveKitName(String name) {
        FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
        if (config != null && config.contains(name)) {
            return config.getString(name + ".kit", name);
        }
        return name;
    }

    private void handleAddGamemode(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorizePrefix("&cNo permission."));
            return;
        }
        if (args.length < 4) {
            player.sendMessage(ColorUtil.colorizePrefix("&cUsage: /queue add <name> <kit> <icon>"));
            return;
        }
        String gamemode = args[1];
        String kit = args[2];
        String icon = args[3].toUpperCase();

        if (Material.matchMaterial(icon) == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cInvalid material: " + icon));
            return;
        }

        FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
        if (config == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cgamemodes.yml not found."));
            return;
        }

        ConfigurationSection section = config.createSection(gamemode);
        section.set("kit", kit);
        section.set("icon", icon);
        section.set("arenas", List.of("Arena1"));

        saveConfig(config);
        plugin.loadExtraConfig("gamemodes.yml");
        player.sendMessage(ColorUtil.colorizePrefix("&aAdded gamemode &f" + gamemode + " &a(kit: &f" + kit + "&a)."));
    }

    private void handleRemoveGamemode(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorizePrefix("&cNo permission."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorizePrefix("&cUsage: /queue remove <name>"));
            return;
        }
        String gamemode = args[1];

        FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
        if (config == null) {
            player.sendMessage(ColorUtil.colorizePrefix("&cgamemodes.yml not found."));
            return;
        }

        if (!config.contains(gamemode)) {
            player.sendMessage(ColorUtil.colorizePrefix("&cGamemode &f" + gamemode + " &cnot found."));
            return;
        }

        config.set(gamemode, null);
        saveConfig(config);
        plugin.loadExtraConfig("gamemodes.yml");
        player.sendMessage(ColorUtil.colorizePrefix("&aRemoved gamemode &f" + gamemode + "&a."));
    }

    private void saveConfig(FileConfiguration config) {
        try {
            config.save(new File(plugin.getDataFolder(), "gamemodes.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save gamemodes.yml: " + e.getMessage());
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.admin")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/queue create <arena>")));
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("arena.not-found", "%name%", args[1])));
            return;
        }
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.queue-created", "%arena%", args[1])));
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/queue join <arena>")));
            return;
        }
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
            return;
        }
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            player.sendMessage(plugin.getMessages().get("queue.already-in-queue"));
            return;
        }
        if (plugin.getQueueManager().joinQueue(player.getUniqueId(), args[1])) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.joined", "%arena%", args[1])));
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.queue-full")));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/queue info <arena>")));
            return;
        }
        int size = plugin.getQueueManager().getQueueSize(args[1]);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.queue-info", "%arena%", args[1], "%players%", String.valueOf(size))));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fQueue Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue &7- Open the queue GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue <kit> &7- Join queue (uses current mode)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue <kit> casual &7- Join casual queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue <kit> competitive &7- Join competitive queue (&c15+ wins required&7)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue <kit> both &7- Join both queues"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue casual &7- Set casual mode as default"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue competitive &7- Set competitive mode as default"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue both &7- Set both modes as default"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue leave &7- Leave the queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/leave &7- Leave the queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue add <name> <kit> <icon> &7- Add a gamemode (Admin)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue remove <name> &7- Remove a gamemode (Admin)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/rtpqueue &7- Join the random teleport queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue create <arena> &7- Create queue sign (Admin)"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue join <arena> &7- Join arena queue"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/queue info <arena> &7- Queue info"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("leave", "casual", "competitive", "both", "ranked", "unranked", "add", "remove", "create", "join", "info"));
            FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
            if (config != null) {
                completions.addAll(config.getKeys(false));
            }
            plugin.getKitManager().getAllVisibleKits(player.getUniqueId()).stream()
                    .map(Kit::getName)
                    .forEach(completions::add);
            return filter(completions, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
                if (config != null) {
                    return config.getKeys(false).stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if (args[0].equalsIgnoreCase("add")) {
                return List.of("DIAMOND_SWORD", "DIAMOND_AXE", "MACE", "TRIDENT", "SHIELD", "GOLDEN_APPLE", "SPLASH_POTION", "PAPER");
            }
            if (List.of("create", "join", "info").contains(args[0].toLowerCase())) {
                return plugin.getArenaManager().getAllArenas().stream()
                        .map(Arena::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (!List.of("leave", "casual", "competitive", "both", "ranked", "unranked", "add", "remove").contains(args[0].toLowerCase())) {
                return filter(List.of("casual", "competitive", "both"), args[1]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            return List.of("DIAMOND_SWORD", "DIAMOND_AXE", "MACE", "TRIDENT", "SHIELD", "GOLDEN_APPLE", "SPLASH_POTION", "PAPER");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
