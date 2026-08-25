/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.SoundManager;
import dev.noah.perplayerkit.util.StyleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

public class EnderchestCommand
implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (DisabledCommand.isBlockedInWorld(player)) {
                return true;
            }
            this.viewOnlyEC(player);
            return true;
        }
        sender.sendMessage("Only players can use this command");
        if (sender instanceof Player) {
            Player s = (Player)sender;
            SoundManager.playFailure(s);
        }
        return true;
    }

    public void viewOnlyEC(Player p) {
        int i;
        ItemStack fill = ItemUtil.createGlassPane();
        ChestMenu menu = ((ChestMenu.Builder)ChestMenu.builder(5).title(StyleManager.get().getPrimaryColor() + "View Only Enderchest")).build();
        for (i = 0; i < 9; ++i) {
            menu.getSlot(i).setItem(fill);
        }
        for (i = 36; i < 45; ++i) {
            menu.getSlot(i).setItem(fill);
        }
        ItemStack[] items = p.getEnderChest().getContents();
        for (int i2 = 0; i2 < 27; ++i2) {
            menu.getSlot(i2 + 9).setItem(items[i2]);
        }
        menu.open(p);
        SoundManager.playOpenGui(p);
    }
}
