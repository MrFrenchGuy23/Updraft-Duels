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
package com.updraftduels.listeners;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCheatListener implements Listener {
    private final UpdraftDuels plugin;
    private final Set<String> antiCheatPlugins;
    private final Set<UUID> exempted = ConcurrentHashMap.newKeySet();

    public AntiCheatListener(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.antiCheatPlugins = new HashSet<>(Arrays.asList(
                "AAC", "NoCheatPlus", "Spartan", "Vulcan", "Matrix", "Grim", "Horizon"
        ));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("anticheat.bypass-during-duel", true)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Duel duel = plugin.getDuelManager().getDuelOf(uuid);
        if (duel == null) {
            exempted.remove(uuid);
            return;
        }

        if (duel.getState() == DuelState.COUNTDOWN) {
            if (plugin.getDuelManager().isFrozen(uuid)) {
                if (event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getZ() != event.getTo().getZ()) {
                    exemptOnce(uuid, player);
                }
            } else {
                exempted.remove(uuid);
            }
        } else if (duel.getState() == DuelState.IN_PROGRESS) {
            exemptOnce(uuid, player);
        } else {
            exempted.remove(uuid);
        }
    }

    private void exemptOnce(UUID uuid, Player player) {
        if (exempted.add(uuid)) {
            exemptPlayer(player);
        }
    }

    private void exemptPlayer(Player player) {
        for (String acPlugin : antiCheatPlugins) {
            if (Bukkit.getPluginManager().getPlugin(acPlugin) != null) {
                switch (acPlugin) {
                    case "NoCheatPlus" -> {
                        try {
                            Class.forName("fr.neatmonster.nocheatplus.NCPAPIProvider");
                            // NCPExemptManager exemptManager = NCPAPIProvider.getNoCheatPlusAPI().getExemptManager();
                            // exemptManager.exempt(player.getUniqueId(), NoCheatPlusAPI.class);
                        } catch (ClassNotFoundException ignored) {}
                    }
                    case "AAC" -> {
                        try {
                            Class.forName("me.gabriel AAC.AAC");
                        } catch (ClassNotFoundException ignored) {}
                    }
                    case "Spartan" -> {
                        try {
                            Class.forName("me.vagdedes.spartan.Spartan");
                            // spartanAPI exemptPlayer(player)
                        } catch (ClassNotFoundException ignored) {}
                    }
                    default -> {}
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("anticheat.bypass-during-duel", true)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager == null) return;

        Duel duel = plugin.getDuelManager().getDuelOf(damager.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        exemptPlayer(damager);
    }
}
