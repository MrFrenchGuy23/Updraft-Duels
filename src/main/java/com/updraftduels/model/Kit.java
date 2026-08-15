/*
 * Updraft Duels
 * Copyright (C) 2026 Updraft Duels
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.updraftduels.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Kit {
    private final String name;
    private final UUID ownerUUID;
    private final boolean isPublic;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private ItemStack offHand;
    private String permissionNode;
    private String prefix;
    private String iconMaterial;
    private int slot;

    public Kit(String name, UUID ownerUUID, boolean isPublic) {
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.isPublic = isPublic;
        this.contents = new ItemStack[36];
        this.armorContents = new ItemStack[4];
        this.permissionNode = null;
        this.prefix = "";
        this.iconMaterial = "CHEST";
    }

    public String getName() { return name; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public boolean isPublic() { return isPublic; }
    public ItemStack[] getContentsArray() { return contents.clone(); }
    public ItemStack[] getArmorContents() { return armorContents.clone(); }
    public ItemStack getOffHand() { return offHand != null ? offHand.clone() : null; }
    public void setOffHand(ItemStack offHand) { this.offHand = offHand != null ? offHand.clone() : null; }
    public String getPermissionNode() { return permissionNode; }
    public void setPermissionNode(String permissionNode) { this.permissionNode = permissionNode; }
    public String getPrefix() { return prefix != null ? prefix : ""; }
    public void setPrefix(String prefix) { this.prefix = prefix != null ? prefix : ""; }
    public String getIconMaterial() { return iconMaterial != null ? iconMaterial : "CHEST"; }
    public void setIconMaterial(String iconMaterial) { this.iconMaterial = iconMaterial != null ? iconMaterial : "CHEST"; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public ItemStack getIcon() {
        Material mat = Material.matchMaterial(iconMaterial);
        if (mat == null) mat = Material.CHEST;
        return new ItemStack(mat);
    }

    public List<ItemStack> getContents() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack item : contents) {
            if (item != null) list.add(item.clone());
        }
        return list;
    }

    public void setContents(ItemStack[] items) {
        Arrays.fill(this.contents, null);
        if (items != null) {
            for (int i = 0; i < Math.min(items.length, 36); i++) {
                this.contents[i] = items[i] != null ? items[i].clone() : null;
            }
        }
    }

    public void setArmorContents(ItemStack[] armor) {
        Arrays.fill(this.armorContents, null);
        if (armor != null) {
            for (int i = 0; i < Math.min(4, armor.length); i++) {
                this.armorContents[i] = armor[i] != null ? armor[i].clone() : null;
            }
        }
    }

    public boolean hasPermission(UUID uuid) {
        if (permissionNode == null) return true;
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
        return player != null && player.hasPermission(permissionNode);
    }
}
