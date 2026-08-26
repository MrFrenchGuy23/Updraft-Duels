/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.DisabledCommand;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShortECCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player)sender;
        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (label.matches("ec[1-9]")) {
            int ecNumber = Integer.parseInt(label.substring(2));
            KitManager.get().loadEnderchest(player, ecNumber);
        } else if (label.matches("enderchest[1-9]")) {
            int ecNumber = Integer.parseInt(label.substring(10));
            KitManager.get().loadEnderchest(player, ecNumber);
        } else {
            player.sendMessage("Invalid command label.");
        }
        return true;
    }
}
