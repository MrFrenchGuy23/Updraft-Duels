/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.noah.perplayerkit;

import dev.noah.bukkit.Metrics;
import dev.noah.perplayerkit.ConfigManager;
import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.UpdateChecker;
import dev.noah.perplayerkit.commands.AboutCommandListener;
import dev.noah.perplayerkit.commands.CopyKitCommand;
import dev.noah.perplayerkit.commands.DeleteKitCommand;
import dev.noah.perplayerkit.commands.EnderchestCommand;
import dev.noah.perplayerkit.commands.InspectEcCommand;
import dev.noah.perplayerkit.commands.InspectKitCommand;
import dev.noah.perplayerkit.commands.KitRoomCommand;
import dev.noah.perplayerkit.commands.MainMenuCommand;
import dev.noah.perplayerkit.commands.PerPlayerKitCommand;
import dev.noah.perplayerkit.commands.PublicKitCommand;
import dev.noah.perplayerkit.commands.RegearCommand;
import dev.noah.perplayerkit.commands.SavePublicKitCommand;
import dev.noah.perplayerkit.commands.ShareECKitCommand;
import dev.noah.perplayerkit.commands.ShareKitCommand;
import dev.noah.perplayerkit.commands.ShortECCommand;
import dev.noah.perplayerkit.commands.ShortKitCommand;
import dev.noah.perplayerkit.commands.SwapKitCommand;
import dev.noah.perplayerkit.commands.extracommands.HealCommand;
import dev.noah.perplayerkit.commands.extracommands.RepairCommand;
import dev.noah.perplayerkit.commands.tabcompleters.ECSlotTabCompleter;
import dev.noah.perplayerkit.commands.tabcompleters.KitSlotTabCompleter;
import dev.noah.perplayerkit.listeners.AutoRekitListener;
import dev.noah.perplayerkit.listeners.JoinListener;
import dev.noah.perplayerkit.listeners.KitMenuCloseListener;
import dev.noah.perplayerkit.listeners.KitRoomSaveListener;
import dev.noah.perplayerkit.listeners.QuitListener;
import dev.noah.perplayerkit.listeners.antiexploit.CommandListener;
import dev.noah.perplayerkit.listeners.antiexploit.ShulkerDropItemsListener;
import dev.noah.perplayerkit.listeners.features.OldDeathDropListener;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.StorageSelector;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.util.BackupManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.StyleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

public final class PerPlayerKit
extends JavaPlugin {
    public static Plugin plugin;
    public static StorageManager storageManager;
    private BackupManager backupManager;

    public static Plugin getPlugin() {
        return plugin;
    }

    public void onEnable() {
        int i;
        this.notice();
        int bstatsId = 24380;
        Metrics metrics = new Metrics(this, bstatsId);
        plugin = this;
        ConfigManager configManager = new ConfigManager((Plugin)this);
        configManager.loadConfig();
        new StyleManager((Plugin)this);
        new ItemFilter((Plugin)this);
        new BroadcastManager((Plugin)this);
        new KitManager(this);
        new KitShareManager((Plugin)this);
        new KitRoomDataManager((Plugin)this);
        this.loadPublicKitsIdsFromConfig();
        this.getLogger().info("Public Kit Configuration Loaded");
        String dbType = this.getConfig().getString("storage.type");
        if (dbType == null) {
            this.getLogger().warning("Database type not found in config, fix your config to continue!");
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        storageManager = new StorageSelector((Plugin)this, dbType).getDbManager();
        this.getLogger().info("Using storage type: " + storageManager.getClass().getName());
        if (storageManager == null) {
            this.getLogger().warning("Database error occurred, please check your config!");
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.attemptDatabaseConnection(true);
        try {
            storageManager.init();
        }
        catch (StorageOperationException e) {
            this.getLogger().warning("Failed to initialize the database. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        if (this.isFileBasedStorage(dbType)) {
            this.backupManager = new BackupManager((Plugin)this);
            if (this.backupManager.isEnabled()) {
                this.getLogger().info("Backup system initialized for file-based storage");
            } else {
                this.getLogger().info("Backup system disabled in configuration");
            }
        } else {
            this.getLogger().info("Backup system not needed for non-file-based storage: " + dbType);
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this, () -> {
            if (storageManager.isConnected()) {
                try {
                    storageManager.keepAlive();
                }
                catch (StorageConnectionException e) {
                    this.getLogger().warning("Database keep alive failed: " + e.getMessage());
                }
            } else {
                this.getLogger().warning("Database connection failed. Attempting to reconnect.");
                this.attemptDatabaseConnection(false);
            }
        }, 600L, 600L);
        this.loadDatabaseData();
        this.getLogger().info("Database data loaded");
        UpdateChecker updateChecker = new UpdateChecker((Plugin)this);
        KitSlotTabCompleter kitSlotTabCompleter = new KitSlotTabCompleter();
        ECSlotTabCompleter ecSlotTabCompleter = new ECSlotTabCompleter();
        this.getCommand("kit").setExecutor((CommandExecutor)new MainMenuCommand(plugin));
        this.getCommand("sharekit").setExecutor((CommandExecutor)new ShareKitCommand());
        this.getCommand("sharekit").setTabCompleter((TabCompleter)kitSlotTabCompleter);
        this.getCommand("shareec").setExecutor((CommandExecutor)new ShareECKitCommand());
        this.getCommand("shareec").setTabCompleter((TabCompleter)ecSlotTabCompleter);
        this.getCommand("copykit").setExecutor((CommandExecutor)new CopyKitCommand());
        KitRoomCommand kitRoomCommand = new KitRoomCommand();
        this.getCommand("kitroom").setExecutor((CommandExecutor)kitRoomCommand);
        this.getCommand("kitroom").setTabCompleter((TabCompleter)kitRoomCommand);
        this.getCommand("swapkit").setExecutor((CommandExecutor)new SwapKitCommand());
        this.getCommand("swapkit").setTabCompleter((TabCompleter)kitSlotTabCompleter);
        this.getCommand("deletekit").setExecutor((CommandExecutor)new DeleteKitCommand());
        this.getCommand("deletekit").setTabCompleter((TabCompleter)kitSlotTabCompleter);
        this.getCommand("inspectkit").setExecutor((CommandExecutor)new InspectKitCommand(plugin));
        this.getCommand("inspectkit").setTabCompleter((TabCompleter)new InspectKitCommand(plugin));
        this.getCommand("inspectec").setExecutor((CommandExecutor)new InspectEcCommand(plugin));
        this.getCommand("inspectec").setTabCompleter((TabCompleter)new InspectEcCommand(plugin));
        this.getCommand("enderchest").setExecutor((CommandExecutor)new EnderchestCommand());
        SavePublicKitCommand savePublicKitCommand = new SavePublicKitCommand();
        this.getCommand("savepublickit").setExecutor((CommandExecutor)savePublicKitCommand);
        this.getCommand("savepublickit").setTabCompleter((TabCompleter)savePublicKitCommand);
        PublicKitCommand publicKitCommand = new PublicKitCommand(plugin);
        this.getCommand("publickit").setExecutor((CommandExecutor)publicKitCommand);
        this.getCommand("publickit").setTabCompleter((TabCompleter)publicKitCommand);
        for (i = 1; i <= 9; ++i) {
            this.getCommand("k" + i).setExecutor((CommandExecutor)new ShortKitCommand());
        }
        for (i = 1; i <= 9; ++i) {
            this.getCommand("ec" + i).setExecutor((CommandExecutor)new ShortECCommand());
        }
        RegearCommand regearCommand = new RegearCommand((Plugin)this);
        this.getCommand("regear").setExecutor((CommandExecutor)regearCommand);
        this.getCommand("heal").setExecutor((CommandExecutor)new HealCommand());
        this.getCommand("repair").setExecutor((CommandExecutor)new RepairCommand());
        this.getCommand("perplayerkit").setExecutor((CommandExecutor)new PerPlayerKitCommand((Plugin)this));
        Bukkit.getPluginManager().registerEvents((Listener)regearCommand, (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new JoinListener((Plugin)this, updateChecker), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new QuitListener((Plugin)this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new MenuFunctionListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new KitMenuCloseListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new KitRoomSaveListener(), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new AutoRekitListener((Plugin)this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new AboutCommandListener(), (Plugin)this);
        if (this.getConfig().getBoolean("feature.old-death-drops", false)) {
            Bukkit.getPluginManager().registerEvents((Listener)new OldDeathDropListener(), (Plugin)this);
        }
        if (this.getConfig().getBoolean("anti-exploit.block-spaces-in-commands", false)) {
            Bukkit.getPluginManager().registerEvents((Listener)new CommandListener(), (Plugin)this);
        }
        if (this.getConfig().getBoolean("anti-exploit.prevent-shulkers-dropping-items", false)) {
            Bukkit.getPluginManager().registerEvents((Listener)new ShulkerDropItemsListener(), (Plugin)this);
        }
        BroadcastManager.get().startScheduledBroadcast();
        updateChecker.printStartupStatus();
    }

    public void onDisable() {
        this.closeDatabaseConnection();
        if (this.backupManager != null) {
            this.backupManager.shutdown();
        }
    }

    private boolean isFileBasedStorage(String storageType) {
        return storageType.equalsIgnoreCase("sqlite") || storageType.equalsIgnoreCase("yml") || storageType.equalsIgnoreCase("yaml");
    }

    private void loadPublicKitsIdsFromConfig() {
        ConfigurationSection publicKitsSection = this.getConfig().getConfigurationSection("publickits");
        if (publicKitsSection == null) {
            this.getLogger().warning("No public kits found in config!");
        } else {
            publicKitsSection.getKeys(false).forEach(key -> {
                String name = this.getConfig().getString("publickits." + key + ".name");
                Material icon = Material.valueOf((String)this.getConfig().getString("publickits." + key + ".icon"));
                PublicKit kit = new PublicKit((String)key, name, icon);
                KitManager.get().getPublicKitList().add(kit);
            });
        }
    }

    private void loadDatabaseData() {
        KitRoomDataManager.get().loadFromDB();
        KitManager.get().getPublicKitList().forEach(kit -> KitManager.get().loadPublicKitFromDB(kit.id));
        Bukkit.getOnlinePlayers().forEach(player -> KitManager.get().loadPlayerDataFromDB(player.getUniqueId()));
    }

    private void attemptDatabaseConnection(boolean disableOnFail) {
        try {
            storageManager.connect();
            if (!storageManager.isConnected()) {
                throw new StorageConnectionException("Expected to be connected to the database, but failed.");
            }
        }
        catch (StorageConnectionException e) {
            if (disableOnFail) {
                this.getLogger().warning("Database connection failed: " + e.getMessage());
                this.getLogger().warning("Disabling plugin.");
                Bukkit.getPluginManager().disablePlugin((Plugin)this);
            }
            this.getLogger().warning("Database connection failed: " + e.getMessage());
        }
    }

    private void closeDatabaseConnection() {
        try {
            storageManager.close();
        }
        catch (StorageConnectionException e) {
            try {
                storageManager.close();
            }
            catch (StorageConnectionException ex) {
                this.getLogger().warning("Failed to close the database connection: " + e.getMessage());
            }
        }
    }

    private void notice() {
        String notice = "* PerPlayerKit is free software: you can redistribute it and/or modify it under\n* the terms of the GNU Affero General Public License as published by the\n* Free Software Foundation, either version 3 of the License, or (at your\n* option) any later version.\n*\n* PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY\n* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS\n* FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for\n* more details.\n*\n* You should have received a copy of the GNU Affero General Public License\n* along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.";
        String otherInfo = "* All users must be provided with the source code of the software, as per the AGPL-3.0 license.\n* If you are using a modified version of PerPlayerKit, you must make the source code of your\n* modified version available to all users, as per the AGPL-3.0 license.\n* Consider modifying the /aboutperplayerkit command to include a link to your modified source code.\n";
        this.getLogger().info(notice);
        this.getLogger().info(otherInfo);
    }
}
