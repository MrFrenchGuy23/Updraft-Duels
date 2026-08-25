/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.RedisStorage;
import dev.noah.perplayerkit.storage.SQLStorage;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.YAMLStorage;
import dev.noah.perplayerkit.storage.sql.MySQL;
import dev.noah.perplayerkit.storage.sql.SQLite;
import java.io.File;
import org.bukkit.plugin.Plugin;

public class StorageSelector {
    private StorageManager storageManager;
    private Plugin plugin;

    public StorageSelector(Plugin plugin, String storageType) {
        this.plugin = plugin;
        switch (storageType) {
            case "yml": 
            case "yaml": {
                this.storageManager = new YAMLStorage(plugin, new File(plugin.getDataFolder(), "please-use-a-real-database.yml").getAbsolutePath());
                break;
            }
            case "redis": {
                this.storageManager = new RedisStorage(plugin);
                break;
            }
            case "mysql": {
                MySQL db = new MySQL(plugin);
                this.storageManager = new SQLStorage(db);
                break;
            }
            default: {
                SQLite db = new SQLite(plugin);
                this.storageManager = new SQLStorage(db);
            }
        }
    }

    public StorageManager getDbManager() {
        return this.storageManager;
    }
}
