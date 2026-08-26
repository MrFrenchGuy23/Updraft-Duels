/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import java.util.Set;

public interface StorageManager {
    public boolean isConnected();

    public void connect() throws StorageConnectionException;

    public void init() throws StorageOperationException;

    public void close() throws StorageConnectionException;

    public void keepAlive() throws StorageConnectionException;

    public void saveKitDataByID(String var1, String var2);

    public String getKitDataByID(String var1);

    public boolean doesKitExistByID(String var1);

    public void deleteKitByID(String var1);

    public Set<String> getAllKitIDs();
}
