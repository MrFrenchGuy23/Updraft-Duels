package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class KitRoomManager {
    private final UpdraftDuels plugin;
    private static final int TOTAL_PAGES = 5;
    private static final int SLOTS_PER_PAGE = 45;
    private final List<ItemStack[]> pages = new ArrayList<>(TOTAL_PAGES);

    public KitRoomManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        for (int i = 0; i < TOTAL_PAGES; i++) {
            pages.add(new ItemStack[SLOTS_PER_PAGE]);
        }
    }

    public void loadFromDatabase() {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < TOTAL_PAGES; i++) {
                String data = plugin.getDatabase().getKitDataByID("kitroom_" + i);
                if (data != null && !data.equalsIgnoreCase("error")) {
                    try {
                        ItemStack[] items = deserializeItemStackArray(data);
                        if (items != null) {
                            pages.set(i, items);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load kit room page " + i + ": " + e.getMessage());
                    }
                }
            }
            plugin.getLogger().info("Loaded " + TOTAL_PAGES + " kit room pages.");
        });
    }

    public ItemStack[] getPage(int page) {
        if (page < 0 || page >= TOTAL_PAGES) return new ItemStack[SLOTS_PER_PAGE];
        return pages.get(page);
    }

    public void setPage(int page, ItemStack[] items) {
        if (page < 0 || page >= TOTAL_PAGES) return;
        pages.set(page, items);
    }

    public void savePageToDatabase(int page) {
        if (page < 0 || page >= TOTAL_PAGES) return;
        ItemStack[] items = pages.get(page);
        String data = serializeItemStackArray(items);
        if (data != null) {
            plugin.getDatabase().saveKitDataByID("kitroom_" + page, data);
        }
    }

    public void saveAllPagesToDatabase() {
        for (int i = 0; i < TOTAL_PAGES; i++) {
            savePageToDatabase(i);
        }
    }

    public int getTotalPages() {
        return TOTAL_PAGES;
    }

    public int getSlotsPerPage() {
        return SLOTS_PER_PAGE;
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
            plugin.getLogger().severe("Failed to serialize kit room items: " + e.getMessage());
            return null;
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
            plugin.getLogger().severe("Failed to deserialize kit room items: " + e.getMessage());
            return null;
        }
    }
}
