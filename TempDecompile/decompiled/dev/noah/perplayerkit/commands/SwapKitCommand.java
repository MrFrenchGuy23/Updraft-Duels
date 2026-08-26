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
 *  org.bukkit.inventory.ItemStack
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SwapKitCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        UUID uuid;
        if (!(sender instanceof Player)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Only Players can use this!");
            return true;
        }
        Player player = (Player)sender;
        if (args.length != 2) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /swapkit <slot1> <slot2>");
            SoundManager.playFailure(player);
            return true;
        }
        Integer slot1 = Ints.tryParse((String)args[0]);
        Integer slot2 = Ints.tryParse((String)args[1]);
        if (slot1 == null || slot2 == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /swapkit <slot1> <slot2>");
            player.sendMessage(String.valueOf(ChatColor.RED) + "Select real numbers");
            SoundManager.playFailure(player);
            return true;
        }
        KitManager kitManager = KitManager.get();
        if (!kitManager.hasKit(uuid = player.getUniqueId(), slot1)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Kit " + slot1 + " doesn't exist!");
            SoundManager.playFailure(player);
            return true;
        }
        if (!kitManager.hasKit(uuid, slot2)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Kit " + slot2 + " doesn't exist!");
            SoundManager.playFailure(player);
            return true;
        }
        ItemStack[] tempkit = (ItemStack[])kitManager.getPlayerKit(uuid, slot1).clone();
        kitManager.savekit(uuid, slot1, kitManager.getPlayerKit(uuid, slot2), true);
        kitManager.savekit(uuid, slot2, (ItemStack[])tempkit.clone(), true);
        kitManager.saveEnderchestToDB(uuid, slot1);
        kitManager.saveEnderchestToDB(uuid, slot2);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kits " + slot1 + " and " + slot2 + " have been swapped!");
        SoundManager.playSuccess(player);
        return true;
    }
}
