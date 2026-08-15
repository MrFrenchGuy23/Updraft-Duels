/*
 * Updraft Duels
 * Copyright (C) 2026 Updraft Duels
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.updraftduels.util;

import com.updraftduels.UpdraftDuels;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class MessageManager {
    private static final String PREFIX = "&8» &r";
    private final UpdraftDuels plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public static String getPrefix() {
        return PREFIX;
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public String get(String key, String... replacements) {
        String message = messagesConfig.getString(key, key);
        if (message == null) return key;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtil.colorize(PREFIX + message);
    }

    public String getRaw(String key) {
        String message = messagesConfig.getString(key, key);
        return message != null ? message : key;
    }

    public void reload() {
        loadMessages();
    }

    public FileConfiguration getConfig() {
        return messagesConfig;
    }
}
