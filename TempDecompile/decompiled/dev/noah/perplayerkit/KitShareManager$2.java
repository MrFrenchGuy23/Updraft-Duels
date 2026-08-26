/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit;

import org.bukkit.scheduler.BukkitRunnable;

class KitShareManager.2
extends BukkitRunnable {
    final /* synthetic */ String val$id;

    KitShareManager.2(String string) {
        this.val$id = string;
    }

    public void run() {
        kitShareMap.remove(this.val$id);
    }
}
