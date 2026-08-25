/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.commands.InspectCommandUtil;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.SoundManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InspectKitCommand
implements CommandExecutor,
TabCompleter {
    private final Plugin plugin;

    public InspectKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int slot;
        if (!(sender instanceof Player)) {
            sender.sendMessage(InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>This command can only be executed by players.</red>")).toString());
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("perplayerkit.inspect")) {
            BroadcastManager.get().sendComponentMessage(player, InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>You don't have permission to use this command.</red>")));
            SoundManager.playFailure(player);
            return true;
        }
        if (args.length < 2) {
            InspectCommandUtil.showUsage(player, "inspectkit");
            return true;
        }
        try {
            slot = Integer.parseInt(args[1]);
            if (slot < 1 || slot > 9) {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e) {
            BroadcastManager.get().sendComponentMessage(player, InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>Slot must be a number between 1 and 9.</red>")));
            SoundManager.playFailure(player);
            return true;
        }
        CompletionStage future = InspectCommandUtil.resolvePlayerIdentifierAsync(args[0]).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    BroadcastManager.get().sendComponentMessage(player, InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>Could not find a player with that name or UUID.</red>")));
                    SoundManager.playFailure(player);
                });
                return CompletableFuture.completedFuture(null);
            }
            Player targetPlayer = Bukkit.getPlayer((UUID)targetUuid);
            return CompletableFuture.runAsync(() -> {
                if (targetPlayer == null) {
                    KitManager.get().loadPlayerDataFromDB((UUID)targetUuid);
                }
            }).thenRun(() -> Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (KitManager.get().hasKit((UUID)targetUuid, slot)) {
                    GUI gui = new GUI(this.plugin);
                    gui.InspectKit(player, (UUID)targetUuid, slot);
                } else {
                    String targetName = InspectCommandUtil.getPlayerName(targetUuid);
                    BroadcastManager.get().sendComponentMessage(player, InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>" + targetName + " does not have a kit in slot " + slot + "</red>")));
                    SoundManager.playFailure(player);
                }
            }));
        });
        ((CompletableFuture)future).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                this.plugin.getLogger().severe("Error loading kit data: " + ex.getMessage());
                BroadcastManager.get().sendComponentMessage(player, InspectCommandUtil.ERROR_PREFIX.append((Component)InspectCommandUtil.mm.deserialize("<red>An error occurred while loading kit data. See console for details.</red>")));
                SoundManager.playFailure(player);
            });
            return null;
        });
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("perplayerkit.inspect")) {
            return List.of();
        }
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            ArrayList<String> completions = new ArrayList<String>(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).filter(name -> name.toLowerCase().startsWith(input)).toList());
            if (input.length() >= 4 && input.contains("-")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).filter(uuid -> uuid.startsWith(input)).toList());
            }
            return completions;
        }
        if (args.length == 2) {
            return IntStream.rangeClosed(1, 9).mapToObj(String::valueOf).filter(slot -> slot.startsWith(args[1])).collect(Collectors.toList());
        }
        return new ArrayList<String>();
    }
}
