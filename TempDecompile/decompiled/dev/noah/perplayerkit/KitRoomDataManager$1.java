/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

class KitRoomDataManager.1
extends BukkitRunnable {
    KitRoomDataManager.1() {
    }

    public void run() {
        for (int i = 0; i < 5; ++i) {
            ItemStack[] pagedata = KitRoomDataManager.this.kitroomData.get(i);
            String output = Serializer.itemStackArrayToBase64(pagedata);
            PerPlayerKit.storageManager.saveKitDataByID(IDUtil.getKitRoomId(i), output);
        }
    }
}
