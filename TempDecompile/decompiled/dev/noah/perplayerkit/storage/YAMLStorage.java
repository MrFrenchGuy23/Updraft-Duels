/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.yaml.snakeyaml.DumperOptions
 *  org.yaml.snakeyaml.DumperOptions$FlowStyle
 *  org.yaml.snakeyaml.Yaml
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.StorageManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YAMLStorage
implements StorageManager {
    private final File storageFile;
    private Map<String, String> data;
    private Plugin plugin;

    public YAMLStorage(Plugin plugin, String filePath) {
        this.plugin = plugin;
        this.storageFile = new File(filePath);
        this.data = new HashMap<String, String>();
    }

    @Override
    public void connect() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public void init() {
        try {
            if (this.storageFile.exists()) {
                yaml = new Yaml();
                inputStream = new FileInputStream(this.storageFile);
                try {
                    loadedData = (Map)yaml.load((InputStream)inputStream);
                    if (loadedData == null) ** GOTO lbl17
                    this.data = loadedData;
                }
                finally {
                    inputStream.close();
                }
            } else {
                this.storageFile.getParentFile().mkdirs();
                this.storageFile.createNewFile();
            }
lbl17:
            // 3 sources

            this.plugin.getLogger().info("YAML storage initialized.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            this.saveToFile();
            this.plugin.getLogger().info("YAML storage closed and saved.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keepAlive() {
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        this.data.put(kitID, data);
        try {
            this.saveToFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        return this.data.getOrDefault(kitID, "error");
    }

    @Override
    public boolean doesKitExistByID(String kitID) {
        return this.data.containsKey(kitID);
    }

    @Override
    public void deleteKitByID(String kitID) {
        this.data.remove(kitID);
        try {
            this.saveToFile();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(this.storageFile);){
            yaml.dump(this.data, (Writer)writer);
        }
    }

    @Override
    public Set<String> getAllKitIDs() {
        return new HashSet<String>(this.data.keySet());
    }
}
