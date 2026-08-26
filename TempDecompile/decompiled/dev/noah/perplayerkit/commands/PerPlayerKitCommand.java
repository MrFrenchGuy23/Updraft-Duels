/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.storage.StorageMigrator;
import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PerPlayerKitCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> STORAGE_TYPES = Arrays.asList("sqlite", "mysql", "redis", "yml");
    private Plugin plugin;

    public PerPlayerKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Missing arguments!");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "about": {
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "PerPlayerKit is a plugin that allows players to have their own kits.");
                return true;
            }
            case "import": {
                if (args.length < 2) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Missing import type!");
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "kitsx": {
                        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Starting import...");
                        KitsXImporter importer = new KitsXImporter(this.plugin, sender);
                        if (!importer.checkForFiles()) {
                            sender.sendMessage(String.valueOf(ChatColor.RED) + "Missing files to import");
                            sender.sendMessage(String.valueOf(ChatColor.RED) + "Copy data folder from KitsX into the PerPlayerKit folder");
                        }
                        importer.importFiles();
                        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Attempted import of KitsX data!");
                        break;
                    }
                    default: {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid import type!");
                    }
                }
                return true;
            }
            case "migrate": {
                if (args.length < 3) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /perplayerkit migrate <source> <destination>");
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Available storage types: sqlite, mysql, redis, yml");
                    return true;
                }
                String sourceType = args[1].toLowerCase();
                String destType = args[2].toLowerCase();
                if (!STORAGE_TYPES.contains(sourceType)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid source storage type: " + sourceType);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Available types: sqlite, mysql, redis, yml");
                    return true;
                }
                if (!STORAGE_TYPES.contains(destType)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid destination storage type: " + destType);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Available types: sqlite, mysql, redis, yml");
                    return true;
                }
                if (sourceType.equals(destType)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Source and destination cannot be the same!");
                    return true;
                }
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Starting migration from " + sourceType + " to " + destType + "...");
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "This may take a while for large datasets. Check console for progress.");
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    StorageMigrator migrator = new StorageMigrator(this.plugin);
                    StorageMigrator.MigrationResult result = migrator.migrate(sourceType, destType, message -> Bukkit.getScheduler().runTask(this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.GRAY) + message)));
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        if (result.isSuccess()) {
                            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Migration completed successfully!");
                            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Migrated: " + result.getMigratedCount() + " entries");
                            if (result.getFailedCount() > 0) {
                                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Failed: " + result.getFailedCount() + " entries");
                            }
                            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Remember to update your config.yml storage.type to '" + destType + "' and restart the server.");
                        } else {
                            sender.sendMessage(String.valueOf(ChatColor.RED) + "Migration failed: " + result.getErrorMessage());
                        }
                    });
                });
                return true;
            }
        }
        sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid subcommand!");
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("about", "import", "migrate");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return List.of("kitsx");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return STORAGE_TYPES.stream().filter(type -> type.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            String sourceType = args[1].toLowerCase();
            return STORAGE_TYPES.stream().filter(type -> !type.equals(sourceType)).filter(type -> type.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        return null;
    }
}
