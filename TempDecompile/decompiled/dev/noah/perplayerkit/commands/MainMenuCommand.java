/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.DisabledCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class MainMenuCommand
implements CommandExecutor {
    private Plugin plugin;

    public MainMenuCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player p = (Player)commandSender;
        if (DisabledCommand.isBlockedInWorld(p)) {
            return true;
        }
        GUI main = new GUI(this.plugin);
        main.OpenMainMenu(p);
        return true;
    }
}
