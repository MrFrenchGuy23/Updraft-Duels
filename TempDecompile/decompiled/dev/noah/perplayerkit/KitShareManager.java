/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.SoundManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class KitShareManager {
    public static HashMap<String, ItemStack[]> kitShareMap;
    private static KitShareManager instance;
    private final Plugin plugin;

    public KitShareManager(Plugin plugin) {
        this.plugin = plugin;
        kitShareMap = new HashMap();
        instance = this;
    }

    public static KitShareManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitShareManager has not been initialized");
        }
        return instance;
    }

    public List<String> getKitSlots(Player p) {
        ArrayList<String> slots = new ArrayList<String>();
        for (int i = 1; i <= 9; ++i) {
            if (!KitManager.get().hasKit(p.getUniqueId(), i)) continue;
            slots.add(String.valueOf(i));
        }
        return slots;
    }

    public List<String> getECSlots(Player p) {
        ArrayList<String> slots = new ArrayList<String>();
        for (int i = 1; i <= 9; ++i) {
            if (!KitManager.get().hasEC(p.getUniqueId(), i)) continue;
            slots.add(String.valueOf(i));
        }
        return slots;
    }

    public void shareKit(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasKit(uuid, slot)) {
            final String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
            if (kitShareMap.putIfAbsent(id, (ItemStack[])kitManager.getPlayerKit(uuid, slot).clone()) == null) {
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Use /copykit " + id + " to copy this kit");
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Code expires in 15 minutes");
                SoundManager.playSuccess(p);
                new BukkitRunnable(){

                    public void run() {
                        kitShareMap.remove(id);
                    }
                }.runTaskLater(this.plugin, 18000L);
            } else {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Unexpected error occurred, please try again.");
                SoundManager.playFailure(p);
            }
        } else {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Error, that kit does not exist");
            SoundManager.playFailure(p);
        }
    }

    public void shareEC(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasEC(uuid, slot)) {
            final String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
            if (kitShareMap.putIfAbsent(id, (ItemStack[])kitManager.getPlayerEC(uuid, slot).clone()) == null) {
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Use /copyEC " + id + " to copy this enderchest");
                p.sendMessage(String.valueOf(ChatColor.GREEN) + "Code expires in 15 minutes");
                SoundManager.playSuccess(p);
                new BukkitRunnable(){

                    public void run() {
                        kitShareMap.remove(id);
                    }
                }.runTaskLater(this.plugin, 18000L);
            } else {
                p.sendMessage(String.valueOf(ChatColor.RED) + "Unexpected error occurred, please try again.");
                SoundManager.playFailure(p);
            }
        } else {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Error, that EC does not exist");
            SoundManager.playFailure(p);
        }
    }

    public void copyKit(Player p, String str) {
        String id = str.toUpperCase();
        if (!kitShareMap.containsKey(id)) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Error, kit does not exist or has expired");
            SoundManager.playFailure(p);
            return;
        }
        ItemStack[] data = kitShareMap.get(id);
        if (data.length == 27) {
            p.getEnderChest().setContents(kitShareMap.get(id));
            BroadcastManager.get().broadcastPlayerCopiedEC(p);
            SoundManager.playSuccess(p);
        } else if (data.length == 41) {
            p.getInventory().setContents(kitShareMap.get(id));
            BroadcastManager.get().broadcastPlayerCopiedKit(p);
            SoundManager.playSuccess(p);
        } else {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Unexpected error occurred, please try again.");
            SoundManager.playFailure(p);
        }
    }
}
