/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.util;

import org.bukkit.scheduler.BukkitRunnable;

class BackupManager.6
extends BukkitRunnable {
    BackupManager.6() {
    }

    public void run() {
        BackupManager.this.performBackup("manual_");
    }
}
