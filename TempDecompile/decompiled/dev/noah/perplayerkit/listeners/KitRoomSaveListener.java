/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.util.StyleManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class KitRoomSaveListener
implements Listener {
    @EventHandler
    public void onSaveButtonClick(InventoryClickEvent e) {
        Inventory inv;
        if (e.getClick().isShiftClick() && e.getClick().isRightClick() && (inv = e.getInventory()).getSize() == 54 && inv.getLocation() == null) {
            ItemStack saveButton;
            InventoryView view = e.getView();
            Player p = (Player)e.getWhoClicked();
            if (view.getTitle().contains(StyleManager.get().getPrimaryColor() + p.getName() + "'s Kits") && (saveButton = e.getInventory().getItem(53)) != null && saveButton.getType() == Material.BARRIER && e.getSlot() == 53 && (p.hasPermission("perplayerkit.editkitroom") || p.isOp())) {
                int page = saveButton.getAmount() > 0 ? saveButton.getAmount() - 1 : 0;
                ItemStack[] kitroom = new ItemStack[45];
                for (int i = 0; i < 45; ++i) {
                    ItemStack item = e.getInventory().getItem(i);
                    kitroom[i] = item != null ? item.clone() : null;
                }
                KitRoomDataManager.get().setKitRoom(page, kitroom);
                KitRoomDataManager.get().saveToDBAsync();
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Saved kitroom page: " + (page + 1));
            }
        }
    }
}
