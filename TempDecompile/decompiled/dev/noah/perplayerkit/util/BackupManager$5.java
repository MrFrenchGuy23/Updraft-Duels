/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.util;

import org.bukkit.scheduler.BukkitRunnable;

class BackupManager.5
extends BukkitRunnable {
    BackupManager.5() {
    }

    public void run() {
        BackupManager.this.cleanupOldBackups();
    }
}
