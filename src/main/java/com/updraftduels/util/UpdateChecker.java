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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.updraftduels.UpdraftDuels;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker implements Listener {
    private final UpdraftDuels plugin;
    private final String owner;
    private final String repo;
    private final String currentVersion;

    private volatile boolean checked;
    private volatile boolean checkFailed;
    private volatile boolean updateAvailable;
    private volatile String latestVersion;
    private volatile String downloadUrl;

    public UpdateChecker(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.owner = plugin.getConfig().getString("update-checker.repo-owner", "MrFrenchGuy23");
        this.repo = plugin.getConfig().getString("update-checker.repo-name", "Updraft-Duels");
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("update-checker.enabled", true);
    }

    public String getCurrentVersion() { return currentVersion; }
    public String getLatestVersion() { return latestVersion; }
    public String getDownloadUrl() { return downloadUrl; }
    public boolean isUpdateAvailable() { return updateAvailable; }
    public boolean hasChecked() { return checked; }

    public void check() {
        check(null);
    }

    public void check(CommandSender requester) {
        if (!isEnabled() && requester == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CheckResult result = fetchLatest();
            checked = true;
            if (result == null) {
                checkFailed = true;
            } else {
                checkFailed = false;
                latestVersion = result.tag;
                downloadUrl = result.downloadUrl;
                updateAvailable = compareVersions(result.tag, currentVersion) > 0;
            }

            if (requester != null) {
                Bukkit.getScheduler().runTask(plugin, () -> sendResult(requester));
            } else if (plugin.getConfig().getBoolean("update-checker.notify-console", true)) {
                Bukkit.getScheduler().runTask(plugin, this::sendConsoleResult);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isEnabled()) return;
        if (!plugin.getConfig().getBoolean("update-checker.notify-players", true)) return;
        if (!checked || !updateAvailable) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(plugin.getConfig().getString("update-checker.notify-permission", "updraftduels.admin"))) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(plugin.getMessages().get("update-checker.player-available",
                        "%current%", currentVersion,
                        "%latest%", latestVersion,
                        "%download%", downloadUrl));
            }
        }, 20L);
    }

    private void sendResult(CommandSender sender) {
        if (checkFailed) {
            sender.sendMessage(plugin.getMessages().get("update-checker.check-failed"));
        } else if (updateAvailable) {
            sender.sendMessage(plugin.getMessages().get("update-checker.available",
                    "%current%", currentVersion,
                    "%latest%", latestVersion,
                    "%download%", downloadUrl));
        } else {
            sender.sendMessage(plugin.getMessages().get("update-checker.up-to-date", "%current%", currentVersion));
        }
    }

    private void sendConsoleResult() {
        if (checkFailed) {
            plugin.getLogger().warning("Update check failed (could not reach the GitHub releases API).");
        } else if (updateAvailable) {
            plugin.getLogger().warning("A new version of UpdraftDuels is available! Current: "
                    + currentVersion + " | Latest: " + latestVersion + " | Download: " + downloadUrl);
        } else {
            plugin.getLogger().info("UpdraftDuels is up to date (version " + currentVersion + ").");
        }
    }

    private CheckResult fetchLatest() {
        try {
            URL url = new URL("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "UpdraftDuels-Updater");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            try {
                int code = conn.getResponseCode();
                if (code != 200) {
                    return null;
                }
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }
                JsonObject root = JsonParser.parseString(body.toString()).getAsJsonObject();
                String tag = root.has("tag_name") ? root.get("tag_name").getAsString() : null;
                if (tag == null || tag.isBlank()) {
                    return null;
                }
                String download = "https://github.com/" + owner + "/" + repo + "/releases/latest";
                if (root.has("html_url") && root.get("html_url").isJsonPrimitive()) {
                    download = root.get("html_url").getAsString();
                }
                JsonArray assets = root.has("assets") && root.get("assets").isJsonArray()
                        ? root.getAsJsonArray("assets") : null;
                if (assets != null && !assets.isEmpty()) {
                    JsonObject first = assets.get(0).getAsJsonObject();
                    if (first.has("browser_download_url") && first.get("browser_download_url").isJsonPrimitive()) {
                        download = first.get("browser_download_url").getAsString();
                    }
                }
                return new CheckResult(tag, download);
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int compareVersions(String a, String b) {
        String[] pa = normalize(a).split("\\.");
        String[] pb = normalize(b).split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int x = i < pa.length ? parseInt(pa[i]) : 0;
            int y = i < pb.length ? parseInt(pb[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    private static String normalize(String v) {
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int dash = s.indexOf('-');
        if (dash >= 0) s = s.substring(0, dash);
        return s;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record CheckResult(String tag, String downloadUrl) {
    }
}
