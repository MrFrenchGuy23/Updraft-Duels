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
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.SoundManager;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SavePublicKitCommand
implements CommandExecutor,
TabCompleter {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }
        Player p = (Player)sender;
        if (DisabledCommand.isBlockedInWorld(p)) {
            return true;
        }
        if (args.length < 1) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "You need to specify a kit id");
            p.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /" + label + " <kitid>");
            return true;
        }
        String kidId = args[0];
        if (KitManager.get().getPublicKitList().stream().noneMatch(kit -> kit.id.equals(kidId))) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Public kit " + kidId + " does not exist");
            p.sendMessage(String.valueOf(ChatColor.RED) + "You may need to add a public kit in the config");
            return true;
        }
        PlayerInventory inv = p.getInventory();
        ItemStack[] data = new ItemStack[41];
        for (int i = 0; i < 41; ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            data[i] = item.clone();
        }
        data = ItemFilter.get().filterItemStack(data);
        KitManager kitManager = KitManager.get();
        boolean success = kitManager.savePublicKit(kidId, data);
        if (success) {
            kitManager.savePublicKitToDB(kidId);
            p.sendMessage("Saved kit " + kidId);
            SoundManager.playSuccess(p);
        } else {
            p.sendMessage("Error saving kit " + kidId);
            SoundManager.playFailure(p);
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return KitManager.get().getPublicKitList().stream().map(kit -> kit.id).toList();
    }
}
