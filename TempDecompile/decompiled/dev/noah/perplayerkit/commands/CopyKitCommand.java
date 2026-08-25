/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CopyKitCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (DisabledCommand.isBlockedInWorld(player)) {
                return true;
            }
            if (args.length > 0) {
                KitShareManager.get().copyKit(player, args[0]);
            } else {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Error, you must enter a kit code to copy");
                SoundManager.playFailure(player);
            }
        } else {
            sender.sendMessage("Only players can use this command");
        }
        return true;
    }
}
