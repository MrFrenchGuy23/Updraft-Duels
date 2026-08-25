/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.UpdateChecker;
import dev.noah.perplayerkit.util.BroadcastManager;
import java.util.ArrayList;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener
implements Listener {
    private final Plugin plugin;
    private final UpdateChecker updateChecker;

    public JoinListener(Plugin plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("perplayerkit.admin") && this.plugin.getConfig().getBoolean("feature.send-update-message-on-join", true)) {
            this.updateChecker.sendUpdateMessage(player);
        }
        final UUID uuid = player.getUniqueId();
        new BukkitRunnable(){

            public void run() {
                KitManager.get().loadPlayerDataFromDB(uuid);
            }
        }.runTaskAsynchronously(this.plugin);
        if (this.plugin.getConfig().getBoolean("motd.enabled")) {
            ArrayList motdMessages = new ArrayList();
            this.plugin.getConfig().getStringList("motd.message").forEach(message -> motdMessages.add(MiniMessage.miniMessage().deserialize(message)));
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> motdMessages.forEach(message -> BroadcastManager.get().sendComponentMessage(player, (Component)message)), this.plugin.getConfig().getLong("motd.delay") * 20L);
        }
    }
}
