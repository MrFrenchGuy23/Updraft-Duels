/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.BroadcastManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InspectCommandUtil {
    public static final int MIN_SLOT = 1;
    public static final int MAX_SLOT = 9;
    public static final MiniMessage mm = MiniMessage.miniMessage();
    public static final Component ERROR_PREFIX = mm.deserialize("<red>Error:</red> ");

    private InspectCommandUtil() {
    }

    public static CompletableFuture<UUID> resolvePlayerIdentifierAsync(String identifier) {
        try {
            UUID uuid = UUID.fromString(identifier);
            return CompletableFuture.completedFuture(uuid);
        }
        catch (IllegalArgumentException uuid) {
            Player onlinePlayer = Bukkit.getPlayerExact((String)identifier);
            if (onlinePlayer != null) {
                return CompletableFuture.completedFuture(onlinePlayer.getUniqueId());
            }
            return CompletableFuture.supplyAsync(() -> {
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (!identifier.equalsIgnoreCase(offlinePlayer.getName())) continue;
                    return offlinePlayer.getUniqueId();
                }
                return null;
            });
        }
    }

    public static String getPlayerName(@NotNull UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer((UUID)uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }

    public static void showUsage(@NotNull Player player, @NotNull String commandName) {
        BroadcastManager.get().sendComponentMessage(player, ERROR_PREFIX.append((Component)mm.deserialize("<red>Usage: /" + commandName + " <player|uuid> <slot></red>")));
    }
}
