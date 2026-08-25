/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigManager {
    private final File configFile;
    private final FileConfiguration config;
    private final Plugin plugin;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration((File)this.configFile);
    }

    public void loadConfig() {
        if (this.configFile.exists()) {
            this.mergeMissingKeys();
        } else {
            this.plugin.saveDefaultConfig();
        }
        this.plugin.saveConfig();
    }

    private void mergeMissingKeys() {
        InputStream defaultConfigStream = this.plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            return;
        }
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defaultConfigStream));
        boolean updated = false;
        for (String key : defaultConfig.getKeys(true)) {
            if (key.equals("publickits")) {
                if (this.config.contains(key)) continue;
                this.config.set(key, (Object)defaultConfig.getConfigurationSection(key).getValues(true));
                this.plugin.getLogger().info("Added missing section: publickits");
                updated = true;
                continue;
            }
            if (key.startsWith("publickits") || this.config.contains(key)) continue;
            this.config.set(key, defaultConfig.get(key));
            this.plugin.getLogger().info("Added missing config key: " + key);
            updated = true;
        }
        if (updated) {
            try {
                this.config.save(this.configFile);
                this.plugin.getLogger().info("Configuration updated with missing keys.");
            }
            catch (Exception e) {
                this.plugin.getLogger().severe("Failed to save updated configuration: " + e.getMessage());
            }
        }
    }
}
