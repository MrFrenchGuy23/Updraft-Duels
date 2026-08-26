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
import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShareECKitCommand
implements CommandExecutor {
    private final CooldownManager shareECCommandCooldown = new CooldownManager(5L);

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        Player player = (Player)sender;
        if (args.length < 1) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Error, you must select a EC slot to share");
            SoundManager.playFailure(player);
            return true;
        }
        if (this.shareECCommandCooldown.isOnCooldown(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Please don't spam the command (5 second cooldown)");
            SoundManager.playFailure(player);
            return true;
        }
        Integer slot = Ints.tryParse((String)args[0]);
        if (slot == null || slot < 1 || slot > 9) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Select a valid kit slot");
            SoundManager.playFailure(player);
            return true;
        }
        KitShareManager.get().shareEC(player, slot);
        this.shareECCommandCooldown.setCooldown(player);
        return true;
    }
}
