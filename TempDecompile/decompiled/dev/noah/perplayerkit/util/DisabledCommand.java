/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DisabledCommand {
    private static boolean isBlockedInWorld(World world) {
        return PerPlayerKit.getPlugin().getConfig().getStringList("disabled-command-worlds").contains(world.getName());
    }

    public static boolean isBlockedInWorld(Player player) {
        if (DisabledCommand.isBlockedInWorld(player.getWorld())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)PerPlayerKit.getPlugin().getConfig().getString("disabled-command-message")));
            SoundManager.playFailure(player);
            return true;
        }
        return false;
    }
}
