/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.util.SoundManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KitRoomCommand
implements CommandExecutor,
TabCompleter {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("load")) {
                KitRoomDataManager.get().loadFromDB();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit Room loaded from SQL");
                if (sender instanceof Player) {
                    Player p = (Player)sender;
                    SoundManager.playSuccess(p);
                }
            } else if (args[0].equalsIgnoreCase("save")) {
                KitRoomDataManager.get().saveToDBAsync();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit Room saved to SQL");
                if (sender instanceof Player) {
                    Player p = (Player)sender;
                    SoundManager.playSuccess(p);
                }
            } else {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Incorrect Usage!");
                sender.sendMessage("/kitroom <load/save>");
                if (sender instanceof Player) {
                    Player p = (Player)sender;
                    SoundManager.playFailure(p);
                }
            }
        } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Incorrect Usage!");
            sender.sendMessage("/kitroom <load/save>");
            if (sender instanceof Player) {
                Player p = (Player)sender;
                SoundManager.playFailure(p);
            }
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("save");
            list.add("load");
            return list;
        }
        return null;
    }
}
