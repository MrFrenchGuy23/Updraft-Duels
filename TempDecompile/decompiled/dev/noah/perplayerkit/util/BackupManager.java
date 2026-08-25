/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.noah.perplayerkit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BackupManager {
    private static BackupManager instance;
    private final Plugin plugin;
    private final File backupDir;
    private final boolean enabled;
    private BukkitTask hourlyTask;
    private BukkitTask dailyTask;
    private BukkitTask weeklyTask;
    private BukkitTask monthlyTask;
    private BukkitTask cleanupTask;
    private static final long HOUR_IN_TICKS = 72000L;
    private static final long DAY_IN_TICKS = 1728000L;
    private static final long WEEK_IN_TICKS = 12096000L;
    private static final long MONTH_IN_TICKS = 51840000L;
    private static final long HOURLY_RETENTION;
    private static final long DAILY_RETENTION;
    private static final long WEEKLY_RETENTION;
    private static final long MONTHLY_RETENTION;
    private static final String HOURLY_PREFIX = "hourly_";
    private static final String DAILY_PREFIX = "daily_";
    private static final String WEEKLY_PREFIX = "weekly_";
    private static final String MONTHLY_PREFIX = "monthly_";

    public BackupManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("backup.enabled", true);
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        if (this.enabled) {
            this.initializeBackupDirectory();
            this.scheduleBackups();
        }
        instance = this;
    }

    public static BackupManager get() {
        return instance;
    }

    private void initializeBackupDirectory() {
        if (!this.backupDir.exists()) {
            if (this.backupDir.mkdirs()) {
                this.plugin.getLogger().info("Created backup directory: " + this.backupDir.getAbsolutePath());
            } else {
                this.plugin.getLogger().warning("Failed to create backup directory: " + this.backupDir.getAbsolutePath());
            }
        }
    }

    private void scheduleBackups() {
        this.plugin.getLogger().info("Scheduling automatic backups...");
        this.hourlyTask = new BukkitRunnable(){

            public void run() {
                BackupManager.this.performBackup(BackupManager.HOURLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(this.plugin, 72000L, 72000L);
        this.dailyTask = new BukkitRunnable(){

            public void run() {
                BackupManager.this.performBackup(BackupManager.DAILY_PREFIX);
            }
        }.runTaskTimerAsynchronously(this.plugin, 1728000L, 1728000L);
        this.weeklyTask = new BukkitRunnable(){

            public void run() {
                BackupManager.this.performBackup(BackupManager.WEEKLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(this.plugin, 12096000L, 12096000L);
        this.monthlyTask = new BukkitRunnable(){

            public void run() {
                BackupManager.this.performBackup(BackupManager.MONTHLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(this.plugin, 51840000L, 51840000L);
        this.cleanupTask = new BukkitRunnable(){

            public void run() {
                BackupManager.this.cleanupOldBackups();
            }
        }.runTaskTimerAsynchronously(this.plugin, 432000L, 432000L);
        this.plugin.getLogger().info("Automatic backups scheduled successfully");
    }

    public void performBackup(String prefix) {
        if (!this.enabled) {
            return;
        }
        try {
            File yamlFile;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File sqliteFile = new File(this.plugin.getDataFolder(), "database.db");
            if (sqliteFile.exists()) {
                File backupFile = new File(this.backupDir, prefix + "database_" + timestamp + ".db");
                Files.copy(sqliteFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.plugin.getLogger().info("Backed up SQLite database to: " + backupFile.getName());
            }
            if ((yamlFile = new File(this.plugin.getDataFolder(), "please-use-a-real-database.yml")).exists()) {
                File backupFile = new File(this.backupDir, prefix + "yaml-storage_" + timestamp + ".yml");
                Files.copy(yamlFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.plugin.getLogger().info("Backed up YAML storage to: " + backupFile.getName());
            }
            this.backupAdditionalFiles(prefix, timestamp);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to perform backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void backupAdditionalFiles(String prefix, String timestamp) throws IOException {
        File shmFile;
        File walFile;
        File configFile = new File(this.plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            File backupFile = new File(this.backupDir, prefix + "config_" + timestamp + ".yml");
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        if ((walFile = new File(this.plugin.getDataFolder(), "database.db-wal")).exists()) {
            File backupFile = new File(this.backupDir, prefix + "database-wal_" + timestamp + ".db");
            Files.copy(walFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        if ((shmFile = new File(this.plugin.getDataFolder(), "database.db-shm")).exists()) {
            File backupFile = new File(this.backupDir, prefix + "database-shm_" + timestamp + ".db");
            Files.copy(shmFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanupOldBackups() {
        if (!this.enabled || !this.backupDir.exists()) {
            return;
        }
        File[] backupFiles = this.backupDir.listFiles();
        if (backupFiles == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        Arrays.sort(backupFiles, Comparator.comparing(File::lastModified));
        for (File file : backupFiles) {
            if (!file.isFile()) continue;
            long fileAge = currentTime - file.lastModified();
            String fileName = file.getName();
            boolean shouldDelete = false;
            if (fileName.startsWith(HOURLY_PREFIX) && fileAge > HOURLY_RETENTION) {
                shouldDelete = true;
            } else if (fileName.startsWith(DAILY_PREFIX) && fileAge > DAILY_RETENTION) {
                shouldDelete = true;
            } else if (fileName.startsWith(WEEKLY_PREFIX) && fileAge > WEEKLY_RETENTION) {
                shouldDelete = true;
            } else if (fileName.startsWith(MONTHLY_PREFIX) && fileAge > MONTHLY_RETENTION) {
                shouldDelete = true;
            }
            if (!shouldDelete) continue;
            if (file.delete()) {
                this.plugin.getLogger().info("Deleted old backup: " + fileName);
                continue;
            }
            this.plugin.getLogger().warning("Failed to delete old backup: " + fileName);
        }
    }

    public void performManualBackup() {
        if (!this.enabled) {
            this.plugin.getLogger().warning("Backups are disabled in configuration");
            return;
        }
        this.plugin.getLogger().info("Performing manual backup...");
        new BukkitRunnable(){

            public void run() {
                BackupManager.this.performBackup("manual_");
            }
        }.runTaskAsynchronously(this.plugin);
    }

    public File getBackupDirectory() {
        return this.backupDir;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public int[] getBackupCounts() {
        if (!this.backupDir.exists()) {
            return new int[]{0, 0, 0, 0};
        }
        File[] files = this.backupDir.listFiles();
        if (files == null) {
            return new int[]{0, 0, 0, 0};
        }
        int hourly = 0;
        int daily = 0;
        int weekly = 0;
        int monthly = 0;
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (name.startsWith(HOURLY_PREFIX)) {
                ++hourly;
                continue;
            }
            if (name.startsWith(DAILY_PREFIX)) {
                ++daily;
                continue;
            }
            if (name.startsWith(WEEKLY_PREFIX)) {
                ++weekly;
                continue;
            }
            if (!name.startsWith(MONTHLY_PREFIX)) continue;
            ++monthly;
        }
        return new int[]{hourly, daily, weekly, monthly};
    }

    public void shutdown() {
        if (this.hourlyTask != null) {
            this.hourlyTask.cancel();
        }
        if (this.dailyTask != null) {
            this.dailyTask.cancel();
        }
        if (this.weeklyTask != null) {
            this.weeklyTask.cancel();
        }
        if (this.monthlyTask != null) {
            this.monthlyTask.cancel();
        }
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        if (this.enabled) {
            this.plugin.getLogger().info("Backup manager shutdown complete");
        }
    }

    static {
        HOURLY_RETENTION = TimeUnit.HOURS.toMillis(24L);
        DAILY_RETENTION = TimeUnit.DAYS.toMillis(7L);
        WEEKLY_RETENTION = TimeUnit.DAYS.toMillis(30L);
        MONTHLY_RETENTION = TimeUnit.DAYS.toMillis(365L);
    }
}
