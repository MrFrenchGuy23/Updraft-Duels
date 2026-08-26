/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import java.io.IOException;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class KitRoomDataManager {
    private final ArrayList<ItemStack[]> kitroomData;
    private final Plugin plugin;
    private static KitRoomDataManager instance;

    public KitRoomDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.kitroomData = new ArrayList();
        ItemStack[] defaultPage = new ItemStack[45];
        defaultPage[0] = ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, "<aqua>Default Kit Room Item</aqua>");
        this.kitroomData.add(defaultPage);
        this.kitroomData.add(defaultPage);
        this.kitroomData.add(defaultPage);
        this.kitroomData.add(defaultPage);
        this.kitroomData.add(defaultPage);
        ItemFilter.get().addToWhitelist(this.kitroomData);
        instance = this;
    }

    public static KitRoomDataManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitRoomDataManager has not been initialized yet!");
        }
        return instance;
    }

    public void setKitRoom(int page, ItemStack[] data) {
        this.kitroomData.set(page, data);
        ItemFilter.get().clearWhitelist();
        ItemFilter.get().addToWhitelist(this.kitroomData);
    }

    public ItemStack[] getKitRoomPage(int page) {
        return this.kitroomData.get(page);
    }

    public void saveToDBAsync() {
        new BukkitRunnable(){

            public void run() {
                for (int i = 0; i < 5; ++i) {
                    ItemStack[] pagedata = KitRoomDataManager.this.kitroomData.get(i);
                    String output = Serializer.itemStackArrayToBase64(pagedata);
                    PerPlayerKit.storageManager.saveKitDataByID(IDUtil.getKitRoomId(i), output);
                }
            }
        }.runTaskAsynchronously(this.plugin);
    }

    public void loadFromDB() {
        ItemFilter.get().clearWhitelist();
        for (int i = 0; i < 5; ++i) {
            String input = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getKitRoomId(i));
            if (input.equalsIgnoreCase("error")) continue;
            try {
                ItemStack[] pagedata = Serializer.itemStackArrayFromBase64(input);
                this.kitroomData.set(i, pagedata);
                continue;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        ItemFilter.get().addToWhitelist(this.kitroomData);
    }
}
