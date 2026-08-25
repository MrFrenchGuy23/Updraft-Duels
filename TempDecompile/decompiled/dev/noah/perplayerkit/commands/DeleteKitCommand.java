/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.commands;

import com.google.common.primitives.Ints;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.SoundManager;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DeleteKitCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            UUID uuid = player.getUniqueId();
            if (args.length == 1) {
                Integer slot = Ints.tryParse((String)args[0]);
                KitManager kitManager = KitManager.get();
                if (slot == null) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /deletekit <slot>");
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Select a real number");
                    SoundManager.playFailure(player);
                    return true;
                }
                if (kitManager.hasKit(uuid, slot)) {
                    if (kitManager.deleteKit(uuid, slot)) {
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + slot + " deleted!");
                        SoundManager.playSuccess(player);
                    } else {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Kit deletion failed!");
                        SoundManager.playFailure(player);
                    }
                } else {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Kit " + slot + " doesnt exist!");
                    SoundManager.playFailure(player);
                }
            } else {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /deletekit <slot>");
                SoundManager.playFailure(player);
            }
        } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Only Players can use this!");
            if (sender instanceof Player) {
                Player s = (Player)sender;
                SoundManager.playFailure(s);
            }
        }
        return true;
    }
}
