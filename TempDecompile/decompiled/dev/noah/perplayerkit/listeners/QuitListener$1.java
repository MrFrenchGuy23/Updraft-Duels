/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

class QuitListener.1
extends BukkitRunnable {
    final /* synthetic */ UUID val$uuid;

    QuitListener.1(UUID uUID) {
        this.val$uuid = uUID;
    }

    public void run() {
        KitManager.get().savePlayerKitsToDB(this.val$uuid);
    }
}
