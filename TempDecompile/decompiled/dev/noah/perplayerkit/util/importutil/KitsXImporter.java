/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.util.importutil;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import java.io.File;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class KitsXImporter {
    private final String kitroomFilePath = "data/kitroom.yml";
    private final String kitsFilePath = "data/kits.yml";
    private final String enderchestsFilePath = "data/enderchest.yml";
    private final Plugin plugin;
    private final CommandSender sender;

    public KitsXImporter(Plugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public boolean checkForFiles() {
        File kitroom = new File(this.plugin.getDataFolder(), "data/kitroom.yml");
        File kits = new File(this.plugin.getDataFolder(), "data/kits.yml");
        File enderchests = new File(this.plugin.getDataFolder(), "data/enderchest.yml");
        return kitroom.exists() && kits.exists() && enderchests.exists();
    }

    public void importFiles() {
        if (!this.checkForFiles()) {
            this.sender.sendMessage(String.valueOf(ChatColor.RED) + "Required files are missing. Cannot proceed with the import.");
            return;
        }
        this.importKitroom(this.sender);
        this.importKits(this.sender);
        this.importEnderchests(this.sender);
    }

    private void importKitroom(CommandSender sender) {
        File kitroomFile = new File(this.plugin.getDataFolder(), "data/kitroom.yml");
        if (!kitroomFile.exists()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "kitroom.yml file not found! Skipping import.");
            return;
        }
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration((File)kitroomFile);
        if (yamlConfig.contains("categories")) {
            ArrayList categoryKeys = new ArrayList(yamlConfig.getConfigurationSection("categories").getKeys(false));
            for (int categoryIndex = 0; categoryIndex < Math.min(5, categoryKeys.size()); ++categoryIndex) {
                String category = (String)categoryKeys.get(categoryIndex);
                sender.sendMessage(String.valueOf(ChatColor.BLUE) + "Processing category: " + category);
                ItemStack[] categoryItems = new ItemStack[45];
                int itemIndex = 0;
                for (String key : yamlConfig.getConfigurationSection("categories." + category).getKeys(false)) {
                    if (itemIndex >= 45) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Ignoring additional items in category: " + category);
                        break;
                    }
                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack itemStack = yamlConfig.getItemStack("categories." + category + "." + key);
                        if (itemStack == null || slot >= 45) continue;
                        categoryItems[slot] = itemStack;
                        ++itemIndex;
                    }
                    catch (NumberFormatException e) {
                        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "[Warning] Invalid slot number: " + key + " in category: " + category);
                    }
                }
                KitRoomDataManager.get().setKitRoom(categoryIndex, categoryItems);
                KitRoomDataManager.get().saveToDBAsync();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Imported " + itemIndex + " items in category: " + category);
            }
        }
        sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Finished importing kitroom.yml.");
    }

    private void importKits(CommandSender sender) {
        File kitsFile = new File(this.plugin.getDataFolder(), "data/kits.yml");
        if (!kitsFile.exists()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "kits.yml file not found! Skipping import.");
            return;
        }
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration((File)kitsFile);
        if (yamlConfig.getKeys(false).isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No player kits found in kits.yml.");
            return;
        }
        for (String uuid : yamlConfig.getKeys(false)) {
            sender.sendMessage(String.valueOf(ChatColor.BLUE) + "Processing kits for player UUID: " + uuid);
            if (!yamlConfig.isConfigurationSection(uuid)) {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No kits found for UUID: " + uuid);
                continue;
            }
            for (String kitKey : yamlConfig.getConfigurationSection(uuid).getKeys(false)) {
                int kitNumber;
                try {
                    kitNumber = Integer.parseInt(kitKey.replace("Kit ", "").trim());
                }
                catch (NumberFormatException e) {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "[Warning] Invalid kit number: " + kitKey + " for player UUID: " + uuid);
                    continue;
                }
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Processing Kit " + kitNumber + " for player UUID: " + uuid);
                if (!yamlConfig.isConfigurationSection(uuid + "." + kitKey)) {
                    sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No items found for Kit " + kitNumber + " for player UUID: " + uuid);
                    continue;
                }
                ItemStack[] kitItems = new ItemStack[41];
                for (String slotKey : yamlConfig.getConfigurationSection(uuid + "." + kitKey).getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    }
                    catch (NumberFormatException e) {
                        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "[Warning] Invalid slot number: " + slotKey + " in Kit " + kitNumber);
                        continue;
                    }
                    if (slot < 0 || slot >= 41) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Slot " + slot + " in Kit " + kitNumber + " is out of range (0-40). Skipping.");
                        continue;
                    }
                    ItemStack itemStack = yamlConfig.getItemStack(uuid + "." + kitKey + "." + slotKey);
                    if (itemStack == null) continue;
                    kitItems[slot] = itemStack;
                }
                KitManager.get().savekit(UUID.fromString(uuid), kitNumber, kitItems);
            }
            KitManager.get().savePlayerKitsToDB(UUID.fromString(uuid));
        }
        sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Finished importing kits.yml.");
    }

    private void importEnderchests(CommandSender sender) {
        File kitsFile = new File(this.plugin.getDataFolder(), "data/kits.yml");
        if (!kitsFile.exists()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "enderchest.yml file not found! Skipping import.");
            return;
        }
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration((File)kitsFile);
        if (yamlConfig.getKeys(false).isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No player kits found in kits.yml.");
            return;
        }
        for (String uuid : yamlConfig.getKeys(false)) {
            sender.sendMessage(String.valueOf(ChatColor.BLUE) + "Processing EC for player UUID: " + uuid);
            if (!yamlConfig.isConfigurationSection(uuid)) {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No EC found for UUID: " + uuid);
                continue;
            }
            for (String kitKey : yamlConfig.getConfigurationSection(uuid).getKeys(false)) {
                int kitNumber;
                try {
                    kitNumber = Integer.parseInt(kitKey.replace("Kit ", "").trim());
                }
                catch (NumberFormatException e) {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "[Warning] Invalid EC number: " + kitKey + " for player UUID: " + uuid);
                    continue;
                }
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Processing EC " + kitNumber + " for player UUID: " + uuid);
                if (!yamlConfig.isConfigurationSection(uuid + "." + kitKey)) {
                    sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No items found for EC " + kitNumber + " for player UUID: " + uuid);
                    continue;
                }
                ItemStack[] kitItems = new ItemStack[27];
                for (String slotKey : yamlConfig.getConfigurationSection(uuid + "." + kitKey).getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    }
                    catch (NumberFormatException e) {
                        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "[Warning] Invalid slot number: " + slotKey + " in EC " + kitNumber);
                        continue;
                    }
                    if (slot < 0 || slot >= 27) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Slot " + slot + " in EC " + kitNumber + " is out of range (0-26). Skipping.");
                        continue;
                    }
                    ItemStack itemStack = yamlConfig.getItemStack(uuid + "." + kitKey + "." + slotKey);
                    if (itemStack == null) continue;
                    kitItems[slot] = itemStack;
                }
                KitManager.get().savekit(UUID.fromString(uuid), kitNumber, kitItems);
            }
            KitManager.get().savePlayerKitsToDB(UUID.fromString(uuid));
        }
        sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Finished importing enderchest.yml.");
    }
}
