/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.StyleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public record RegearCommand.RegearInventoryHolder(Player player) implements InventoryHolder
{
    @NotNull
    public Inventory getInventory() {
        Inventory inventory = Bukkit.createInventory((InventoryHolder)this, (int)27, (String)(StyleManager.get().getPrimaryColor() + "Regear Shulker"));
        inventory.setItem(13, REGEAR_SHELL_ITEM);
        return inventory;
    }
}
