/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.StorageSelector;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.plugin.Plugin;

public class StorageMigrator {
    private final Plugin plugin;

    public StorageMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public MigrationResult migrate(String sourceType, String destinationType, Consumer<String> progressCallback) {
        if (sourceType.equalsIgnoreCase(destinationType)) {
            return new MigrationResult(false, 0, 0, "Source and destination storage types are the same.");
        }
        StorageManager source = null;
        StorageManager destination = null;
        try {
            this.log(progressCallback, "Creating source storage connection (" + sourceType + ")...");
            source = new StorageSelector(this.plugin, sourceType).getDbManager();
            this.log(progressCallback, "Creating destination storage connection (" + destinationType + ")...");
            destination = new StorageSelector(this.plugin, destinationType).getDbManager();
            this.log(progressCallback, "Connecting to source storage...");
            source.connect();
            source.init();
            this.log(progressCallback, "Connecting to destination storage...");
            destination.connect();
            destination.init();
            this.log(progressCallback, "Fetching all kit IDs from source...");
            Set<String> kitIDs = source.getAllKitIDs();
            int total = kitIDs.size();
            this.log(progressCallback, "Found " + total + " entries to migrate.");
            if (total == 0) {
                MigrationResult migrationResult = new MigrationResult(true, 0, 0, "No data to migrate.");
                return migrationResult;
            }
            int migrated = 0;
            int failed = 0;
            for (String kitID : kitIDs) {
                try {
                    String data = source.getKitDataByID(kitID);
                    if (data != null && !data.equals("Error") && !data.equals("error")) {
                        destination.saveKitDataByID(kitID, data);
                        ++migrated;
                    } else {
                        ++failed;
                        this.log(progressCallback, "Warning: Could not read data for kit ID: " + kitID);
                    }
                    if (migrated % 100 != 0) continue;
                    this.log(progressCallback, "Progress: " + migrated + "/" + total + " entries migrated...");
                }
                catch (Exception e) {
                    ++failed;
                    this.log(progressCallback, "Error migrating kit ID " + kitID + ": " + e.getMessage());
                }
            }
            this.log(progressCallback, "Migration complete! Migrated: " + migrated + ", Failed: " + failed);
            MigrationResult migrationResult = new MigrationResult(true, migrated, failed, null);
            return migrationResult;
        }
        catch (StorageConnectionException | StorageOperationException e) {
            MigrationResult migrationResult = new MigrationResult(false, 0, 0, "Connection error: " + e.getMessage());
            return migrationResult;
        }
        finally {
            if (source != null) {
                try {
                    source.close();
                }
                catch (StorageConnectionException e) {
                    this.plugin.getLogger().warning("Failed to close source storage: " + e.getMessage());
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                }
                catch (StorageConnectionException e) {
                    this.plugin.getLogger().warning("Failed to close destination storage: " + e.getMessage());
                }
            }
        }
    }

    private void log(Consumer<String> callback, String message) {
        this.plugin.getLogger().info("[Migration] " + message);
        if (callback != null) {
            callback.accept(message);
        }
    }

    public static class MigrationResult {
        private final boolean success;
        private final int migratedCount;
        private final int failedCount;
        private final String errorMessage;

        public MigrationResult(boolean success, int migratedCount, int failedCount, String errorMessage) {
            this.success = success;
            this.migratedCount = migratedCount;
            this.failedCount = failedCount;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public int getMigratedCount() {
            return this.migratedCount;
        }

        public int getFailedCount() {
            return this.failedCount;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }
    }
}
