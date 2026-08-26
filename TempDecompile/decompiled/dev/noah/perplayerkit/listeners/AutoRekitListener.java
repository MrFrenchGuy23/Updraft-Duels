/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class AutoRekitListener
implements Listener {
    private final Plugin plugin;

    public AutoRekitListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!this.plugin.getConfig().getBoolean("feature.rekit-on-respawn", true)) {
            return;
        }
        if (!e.getPlayer().hasPermission("perplayerkit.rekitonrespawn")) {
            return;
        }
        KitManager.get().loadLastKit(e.getPlayer());
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent e) {
        if (!this.plugin.getConfig().getBoolean("feature.rekit-on-kill", false)) {
            return;
        }
        Player killer = e.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (!killer.hasPermission("perplayerkit.rekitonkill")) {
            return;
        }
        KitManager.get().loadLastKit(killer);
    }
}
