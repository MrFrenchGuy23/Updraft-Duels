/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.StyleManager;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class KitMenuCloseListener
implements Listener {
    @EventHandler
    public void onKitEditorClose(InventoryCloseEvent e) {
        InventoryView view;
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54 && inv.getLocation() == null && (view = e.getView()).getTitle().contains(StyleManager.get().getPrimaryColor() + "Kit: ")) {
            Player p = (Player)e.getPlayer();
            UUID uuid = p.getUniqueId();
            int slot = Integer.parseInt(view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Kit: ", ""));
            ItemStack[] kit = new ItemStack[41];
            ItemStack[] chestitems = e.getInventory().getContents();
            for (int i = 0; i < 41; ++i) {
                kit[i] = chestitems[i] == null ? null : chestitems[i].clone();
            }
            KitManager.get().savekit(uuid, slot, kit);
        }
    }

    @EventHandler
    public void onPublicKitEditorClose(InventoryCloseEvent e) {
        InventoryView view;
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54 && inv.getLocation() == null && (view = e.getView()).getTitle().contains(StyleManager.get().getPrimaryColor() + "Public Kit: ")) {
            Player player = (Player)e.getPlayer();
            String publickit = view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Public Kit: ", "");
            ItemStack[] kit = new ItemStack[41];
            ItemStack[] chestitems = e.getInventory().getContents();
            for (int i = 0; i < 41; ++i) {
                kit[i] = chestitems[i] == null ? null : chestitems[i].clone();
            }
            KitManager.get().savePublicKit(player, publickit, kit);
        }
    }

    @EventHandler
    public void onEnderchestEditorClose(InventoryCloseEvent e) {
        InventoryView view;
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54 && inv.getLocation() == null && (view = e.getView()).getTitle().contains(StyleManager.get().getPrimaryColor() + "Enderchest: ")) {
            Player p = (Player)e.getPlayer();
            UUID uuid = p.getUniqueId();
            int slot = Integer.parseInt(view.getTitle().replace(StyleManager.get().getPrimaryColor() + "Enderchest: ", ""));
            ItemStack[] kit = new ItemStack[27];
            ItemStack[] chestitems = e.getInventory().getContents();
            for (int i = 0; i < 27; ++i) {
                kit[i] = chestitems[i + 9] == null ? null : chestitems[i + 9].clone();
            }
            KitManager.get().saveEC(uuid, slot, kit);
        }
    }

    @EventHandler
    public void onInspectKitEditorClose(InventoryCloseEvent e) {
        InventoryView view;
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54 && inv.getLocation() == null && (view = e.getView()).getTitle().contains(StyleManager.get().getPrimaryColor() + "Inspecting ") && view.getTitle().contains("'s kit ")) {
            Player onlinePlayer;
            int slot;
            Player p = (Player)e.getPlayer();
            if (!p.hasPermission("perplayerkit.admin")) {
                return;
            }
            String title = view.getTitle();
            String[] parts = title.replace(StyleManager.get().getPrimaryColor() + "Inspecting ", "").split("'s kit ");
            if (parts.length != 2) {
                return;
            }
            String playerName = parts[0];
            try {
                slot = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException ex) {
                return;
            }
            UUID targetUuid = null;
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (!playerName.equalsIgnoreCase(offlinePlayer.getName())) continue;
                targetUuid = offlinePlayer.getUniqueId();
                break;
            }
            if (targetUuid == null && (onlinePlayer = Bukkit.getPlayerExact((String)playerName)) != null) {
                targetUuid = onlinePlayer.getUniqueId();
            }
            if (targetUuid == null) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Could not find player " + playerName);
                return;
            }
            if (GUI.removeKitDeletionFlag(p)) {
                return;
            }
            ItemStack[] kit = new ItemStack[41];
            ItemStack[] chestitems = e.getInventory().getContents();
            for (int i = 0; i < 41; ++i) {
                kit[i] = chestitems[i] == null ? null : chestitems[i].clone();
            }
            if (KitManager.get().savekit(targetUuid, slot, kit, true)) {
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + slot + " updated for player " + playerName + "!");
            } else {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Failed to update kit for player " + playerName + "!");
            }
        }
    }

    @EventHandler
    public void onInspectEnderchestEditorClose(InventoryCloseEvent e) {
        InventoryView view;
        Inventory inv = e.getInventory();
        if (inv.getSize() == 54 && inv.getLocation() == null && (view = e.getView()).getTitle().contains(StyleManager.get().getPrimaryColor() + "Inspecting ") && view.getTitle().contains("'s enderchest ")) {
            Player onlinePlayer;
            int slot;
            Player p = (Player)e.getPlayer();
            if (!p.hasPermission("perplayerkit.admin")) {
                return;
            }
            String title = view.getTitle();
            String[] parts = title.replace(StyleManager.get().getPrimaryColor() + "Inspecting ", "").split("'s enderchest ");
            if (parts.length != 2) {
                return;
            }
            String playerName = parts[0];
            try {
                slot = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException ex) {
                return;
            }
            UUID targetUuid = null;
            for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                if (!playerName.equalsIgnoreCase(offlinePlayer.getName())) continue;
                targetUuid = offlinePlayer.getUniqueId();
                break;
            }
            if (targetUuid == null && (onlinePlayer = Bukkit.getPlayerExact((String)playerName)) != null) {
                targetUuid = onlinePlayer.getUniqueId();
            }
            if (targetUuid == null) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Could not find player " + playerName);
                return;
            }
            if (GUI.removeKitDeletionFlag(p)) {
                return;
            }
            ItemStack[] kit = new ItemStack[27];
            ItemStack[] chestitems = e.getInventory().getContents();
            for (int i = 0; i < 27; ++i) {
                kit[i] = chestitems[i + 9] == null ? null : chestitems[i + 9].clone();
            }
            if (KitManager.get().saveEC(targetUuid, slot, kit)) {
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Enderchest " + slot + " updated for player " + playerName + "!");
            } else {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Failed to update enderchest for player " + playerName + "!");
            }
        }
    }
}
