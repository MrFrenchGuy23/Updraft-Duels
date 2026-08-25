/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class QuitListener
implements Listener {
    private Plugin plugin;

    public QuitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        new BukkitRunnable(){

            public void run() {
                KitManager.get().savePlayerKitsToDB(uuid);
            }
        }.runTaskAsynchronously(this.plugin);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> KitManager.get().savePlayerKitsToDB(uuid));
    }
}
