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
package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class KitManager {
    private final UpdraftDuels plugin;
    private final Map<String, Kit> kits;

    public KitManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.kits = new ConcurrentHashMap<>();
    }

    public void loadKits() {
        loadAllKits();
    }

    private void loadAllKits() {
        plugin.getDatabase().loadKits(null, false).thenAccept(kitDataList -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map<String, Object> data : kitDataList) {
                    try {
                        String name = (String) data.get("name");
                        UUID ownerUUID = UUID.fromString((String) data.get("owner_uuid"));
                        boolean isPublic = (boolean) data.get("is_public");
                        String contentsB64 = (String) data.get("contents");
                        String armorB64 = (String) data.get("armor");
                        String offHandB64 = (String) data.get("offhand");
                        String permission = (String) data.get("permission_node");

                        Kit kit = new Kit(name, ownerUUID, isPublic);
                        kit.setPermissionNode(permission);
                        kit.setPrefix((String) data.getOrDefault("prefix", ""));
                        kit.setIconMaterial((String) data.getOrDefault("icon", "CHEST"));
                        kit.setSlot((int) data.getOrDefault("slot", 0));

                        if (contentsB64 != null && !contentsB64.isEmpty()) {
                            ItemStack[] contents = deserializeItemStackArray(contentsB64);
                            if (contents != null) kit.setContents(contents);
                        }
                        if (armorB64 != null && !armorB64.isEmpty()) {
                            ItemStack[] armor = deserializeItemStackArray(armorB64);
                            if (armor != null) kit.setArmorContents(armor);
                        }
                        if (offHandB64 != null && !offHandB64.isEmpty()) {
                            ItemStack[] offHand = deserializeItemStackArray(offHandB64);
                            if (offHand != null && offHand.length > 0) kit.setOffHand(offHand[0]);
                        }

                        kits.put(name.toLowerCase(), kit);
                    } catch (Exception ignored) {
                    }
                }
                plugin.getLogger().info("Loaded " + kits.size() + " kits.");
            });
        });
    }

    public boolean createKit(String name, UUID ownerUUID, boolean isPublic) {
        String key = name.toLowerCase();
        if (kits.containsKey(key)) return false;
        Kit kit = new Kit(name, ownerUUID, isPublic);
        kits.put(key, kit);
        return true;
    }

    public boolean createKitFromInventory(String name, UUID ownerUUID, boolean isPublic, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        if (!createKit(name, ownerUUID, isPublic)) return false;
        Kit kit = kits.get(name.toLowerCase());
        kit.setContents(contents);
        kit.setArmorContents(armor);
        kit.setOffHand(offHand);
        if (!isPublic) {
            kit.setSlot(getNextSlot(ownerUUID));
        }
        saveKit(kit);
        return true;
    }

    public void saveKit(Kit kit) {
        String contentsB64 = serializeItemStackArray(kit.getContentsArray());
        String armorB64 = serializeItemStackArray(kit.getArmorContents());
        String offHandB64 = serializeItemStackArray(new ItemStack[]{kit.getOffHand()});
        String id = kit.getName().toLowerCase().replace(" ", "_");
        plugin.getDatabase().saveKit(id, kit.getName(), kit.getOwnerUUID(), kit.isPublic(), contentsB64, armorB64, offHandB64, kit.getPermissionNode(), kit.getPrefix(), kit.getIconMaterial(), kit.getSlot());
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public boolean deleteKit(String name) {
        Kit removed = kits.remove(name.toLowerCase());
        if (removed != null) {
            String id = name.toLowerCase().replace(" ", "_");
            plugin.getDatabase().deleteKit(id);
            if (!removed.isPublic()) {
                renumberSlots(removed.getOwnerUUID());
            }
            return true;
        }
        return false;
    }

    private void renumberSlots(UUID ownerUUID) {
        List<Kit> personal = new ArrayList<>(kits.values().stream()
                .filter(k -> !k.isPublic() && k.getOwnerUUID().equals(ownerUUID))
                .sorted(Comparator.comparingInt(Kit::getSlot))
                .toList());
        for (int i = 0; i < personal.size(); i++) {
            personal.get(i).setSlot(i + 1);
            saveKit(personal.get(i));
        }
    }

    public List<Kit> getPublicKits() {
        return kits.values().stream().filter(Kit::isPublic).toList();
    }

    public List<Kit> getPersonalKits(UUID ownerUUID) {
        return kits.values().stream()
                .filter(k -> !k.isPublic() && k.getOwnerUUID().equals(ownerUUID))
                .toList();
    }

    public int getNextSlot(UUID ownerUUID) {
        int max = 0;
        for (Kit kit : kits.values()) {
            if (!kit.isPublic() && kit.getOwnerUUID().equals(ownerUUID) && kit.getSlot() > max) {
                max = kit.getSlot();
            }
        }
        return max + 1;
    }

    public Kit getKitBySlot(UUID ownerUUID, int slot) {
        for (Kit kit : kits.values()) {
            if (!kit.isPublic() && kit.getOwnerUUID().equals(ownerUUID) && kit.getSlot() == slot) {
                return kit;
            }
        }
        return null;
    }

    public List<Kit> getAllVisibleKits(UUID playerUUID) {
        List<Kit> visible = new ArrayList<>();
        for (Kit kit : kits.values()) {
            if (kit.isPublic()) {
                Player p = plugin.getServer().getPlayer(playerUUID);
                if (kit.getPermissionNode() == null || (p != null && p.hasPermission("updraftduels.kit.public." + kit.getName().toLowerCase()))) {
                    visible.add(kit);
                }
            } else if (kit.getOwnerUUID().equals(playerUUID)) {
                visible.add(kit);
            }
        }
        return visible;
    }

    public boolean updateKit(String name, UUID ownerUUID, ItemStack[] contents, ItemStack[] armor, ItemStack offHand) {
        Kit kit = kits.get(name.toLowerCase());
        if (kit == null) return false;
        if (!kit.getOwnerUUID().equals(ownerUUID)) {
            Player p = plugin.getServer().getPlayer(ownerUUID);
            if (p == null || !p.hasPermission("updraftduels.kit.edit." + name.toLowerCase())) return false;
        }
        kit.setContents(contents);
        kit.setArmorContents(armor);
        kit.setOffHand(offHand);
        saveKit(kit);
        return true;
    }

    public boolean renameKit(String oldName, String newName) {
        Kit kit = kits.remove(oldName.toLowerCase());
        if (kit == null) return false;
        kits.put(newName.toLowerCase(), kit);
        return true;
    }

    private String serializeItemStackArray(ItemStack[] items) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(byteOut);
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(gzip);
            out.writeObject(items);
            out.close();
            return Base64.getEncoder().encodeToString(byteOut.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to serialize kit items: " + e.getMessage());
            return "";
        }
    }

    private ItemStack[] deserializeItemStackArray(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(byteIn);
            BukkitObjectInputStream in = new BukkitObjectInputStream(gzip);
            ItemStack[] items = (ItemStack[]) in.readObject();
            in.close();
            return items;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to deserialize kit items: " + e.getMessage());
            return null;
        }
    }
}
