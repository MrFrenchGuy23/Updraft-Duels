/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class UpdateChecker {
    private Plugin plugin;
    private String url = "https://hangar.papermc.io/api/v1/projects/PerPlayerKit/latestrelease";
    private String spigotDownloadUrl = "https://www.spigotmc.org/resources/perplayerkit.121437/";
    private String modrinthDownloadUrl = "https://modrinth.com/plugin/perplayerkit";
    private String hangarDownloadUrl = "https://hangar.papermc.io/noah32/PerPlayerKit";
    private Boolean updateAvailableCache = null;
    private String latestVersionCache = null;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    private String getCurrentVersion() {
        return this.plugin.getDescription().getVersion();
    }

    private String getLatestVersion() {
        if (this.latestVersionCache != null) {
            return this.latestVersionCache;
        }
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(this.url).build();
            Call call = client.newCall(request);
            Response response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                if (!responseBody.isEmpty() && responseBody.matches("\\d+(\\.\\d+)*")) {
                    this.latestVersionCache = responseBody;
                    return this.latestVersionCache;
                }
                this.plugin.getLogger().warning("Received invalid version format from update server: " + responseBody);
            } else {
                this.plugin.getLogger().warning("Failed to fetch the latest version. HTTP Status: " + response.code());
            }
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("IOException occurred while fetching the latest version: " + e.getMessage());
        }
        this.plugin.getLogger().warning("Using fallback version: 1.0.0");
        this.latestVersionCache = "1.0.0";
        return this.latestVersionCache;
    }

    public boolean checkForUpdate() {
        if (this.updateAvailableCache != null) {
            return this.updateAvailableCache;
        }
        String currentVersion = this.getCurrentVersion();
        String latestVersion = this.getLatestVersion();
        this.updateAvailableCache = this.isSemanticallyNewer(currentVersion, latestVersion);
        return this.updateAvailableCache;
    }

    public void printStartupStatus() {
        if (this.checkForUpdate()) {
            String currentVersion = this.getCurrentVersion();
            String latestVersion = this.getLatestVersion();
            this.plugin.getLogger().info("A new version of PerPlayerKit is available! You are running version " + currentVersion + " and the latest version is " + latestVersion);
            this.plugin.getLogger().info("Download the latest version at:");
            this.plugin.getLogger().info("Spigot: " + this.spigotDownloadUrl);
            this.plugin.getLogger().info("Modrinth: " + this.modrinthDownloadUrl);
            this.plugin.getLogger().info("PaperMC: " + this.hangarDownloadUrl);
        } else {
            this.plugin.getLogger().info("You are running the latest version of PerPlayerKit");
        }
    }

    public void sendUpdateMessage(Player player) {
        if (this.checkForUpdate()) {
            String currentVersion = this.getCurrentVersion();
            String latestVersion = this.getLatestVersion();
            player.sendMessage("A new version of PerPlayerKit is available! You are running version " + currentVersion + " and the latest version is " + latestVersion);
        }
    }

    private boolean isSemanticallyNewer(String currentVersion, String newVersion) {
        String[] currentVersionSplit = currentVersion.split("\\.");
        String[] newVersionSplit = newVersion.split("\\.");
        for (int i = 0; i < Math.min(currentVersionSplit.length, newVersionSplit.length); ++i) {
            int newNumber;
            int currentNumber = Integer.parseInt(currentVersionSplit[i]);
            if (currentNumber < (newNumber = Integer.parseInt(newVersionSplit[i]))) {
                return true;
            }
            if (currentNumber <= newNumber) continue;
            return false;
        }
        return newVersionSplit.length > currentVersionSplit.length;
    }
}
