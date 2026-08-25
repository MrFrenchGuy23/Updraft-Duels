/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage;

public static class StorageMigrator.MigrationResult {
    private final boolean success;
    private final int migratedCount;
    private final int failedCount;
    private final String errorMessage;

    public StorageMigrator.MigrationResult(boolean success, int migratedCount, int failedCount, String errorMessage) {
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
