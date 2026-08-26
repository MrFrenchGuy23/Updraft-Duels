/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.util.BackupManager;
import org.bukkit.scheduler.BukkitRunnable;

class BackupManager.4
extends BukkitRunnable {
    BackupManager.4() {
    }

    public void run() {
        BackupManager.this.performBackup(BackupManager.MONTHLY_PREFIX);
    }
}
