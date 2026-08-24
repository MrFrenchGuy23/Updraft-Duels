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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DuelListener implements Listener {
    private final UpdraftDuels plugin;
    private final Map<UUID, Long> lastArenaWarning;

    public DuelListener(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.lastArenaWarning = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);

        Player killer = player.getKiller();
        String deathCause = resolveDeathCause(player);
        duel.setDeathCause(deathCause);

        DuelTeam team = duel.getTeamOf(player.getUniqueId());
        if (team != null) {
            team.eliminate(player.getUniqueId());
        }

        if (killer != null && duel.getRounds() <= 1) {
            Team killerTeamIndex = duel.getTeamIndex(killer.getUniqueId());
            if (killerTeamIndex == Team.TEAM_A) {
                duel.incrementScoreA();
            } else if (killerTeamIndex == Team.TEAM_B) {
                duel.incrementScoreB();
            }
        }

        if (killer != null) {
            String trail = plugin.getCosmeticsManager().getTrail(killer.getUniqueId());
            if (!trail.equals("none")) {
                plugin.getCosmeticsManager().playTrail(killer, trail);
            }
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player participant = Bukkit.getPlayer(uuid);
            if (participant != null) {
                participant.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&c" + player.getName() + " has been eliminated!"));
            }
        }

        plugin.getDuelManager().checkWinConditions(duel);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) return;

        Entity damagerEntity = event.getDamager();
        Player damager = null;
        if (damagerEntity instanceof Player) {
            damager = (Player) damagerEntity;
        } else if (damagerEntity instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        }
        if (damager == null) return;

        Duel duel = plugin.getDuelManager().getDuelOf(damaged.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());

        if (ruleset != null && ruleset.isNoDamage()) {
            event.setCancelled(true);
            double knockback = 2.5;
            org.bukkit.util.Vector delta = damaged.getLocation().toVector()
                    .subtract(damagerEntity.getLocation().toVector());
            if (delta.lengthSquared() < 1.0E-4) {
                delta = new org.bukkit.util.Vector(0, 0, 1);
            }
            org.bukkit.util.Vector velocity = delta.normalize().multiply(knockback);
            velocity.setY(0.5);
            damaged.setVelocity(velocity);
            return;
        }

        if (ruleset != null && ruleset.isFistsOnly()) {
            if (damager.getInventory().getItemInMainHand().getType() != org.bukkit.Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }

        if (duel.getType().isTeamDuel()) {
            Team damagerTeam = duel.getTeamIndex(damager.getUniqueId());
            Team damagedTeam = duel.getTeamIndex(damaged.getUniqueId());
            if (damagerTeam == damagedTeam) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamageNonEntity(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());

        if (ruleset != null && ruleset.isNoDamage()) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK &&
                    event.getCause() != EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && ruleset != null && ruleset.isBreakableFloor()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getQueueManager().onPlayerDisconnect(uuid);
        plugin.getDuelManager().onPlayerDisconnect(uuid);
        plugin.getSpectatorManager().onPlayerQuit(uuid);
        plugin.getAntiSpamManager().clearAll(uuid);
        plugin.clearPlayerSettings(uuid);
        plugin.getFriendManager().removeFromCache(uuid);
        plugin.getHistoryManager().clearPlayer(uuid);
        plugin.getNametagManager().onPlayerQuit(player);
        lastArenaWarning.remove(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getNametagManager().onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());

        if (ruleset != null && !ruleset.isHungerEnabled()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDuelManager().isFrozen(player.getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
                player.sendMessage(plugin.getMessages().get("general.frozen"));
            }
        }

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null) {
            duel = plugin.getDuelManager().getSpectatingDuel(player.getUniqueId());
        }
        if (duel == null) {
            return;
        }
        if (duel.getState() != DuelState.IN_PROGRESS && duel.getState() != DuelState.COUNTDOWN) return;

        Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
        if (duelDebug()) {
            String world = event.getTo() != null ? event.getTo().getWorld().getName() : "null";
            plugin.getLogger().info("[DuelDebug] " + player.getName() + " moved in duel '" + duel.getArenaName()
                    + "' | world=" + world + " | to=" + fmtLoc(event.getTo())
                    + " | arena=" + (arena == null ? "NOT_FOUND" : arena.getName() + " configured=" + arena.isConfigured()));
        }
        enforceArenaBoundary(event, player, arena, duel);
    }

    private void enforceArenaBoundary(PlayerMoveEvent event, Player player, Arena arena, Duel duel) {
        if (arena == null || !arena.isConfigured()) {
            if (duelDebug()) plugin.getLogger().info("[DuelDebug] " + player.getName() + " boundary skipped: arena null/not configured");
            return;
        }

        Location to = event.getTo();
        if (to == null) return;

        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();
        if (to.getWorld() == null || !to.getWorld().equals(pos1.getWorld())) {
            if (duelDebug()) plugin.getLogger().info("[DuelDebug] " + player.getName()
                    + " boundary skipped: world mismatch | player=" + to.getWorld().getName()
                    + " arena=" + (pos1.getWorld() != null ? pos1.getWorld().getName() : "null"));
            return;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        double minY = Math.min(pos1.getY(), pos2.getY());

        // Falling through the arena floor is the intended loss condition for
        // sumo/knockback and spleef-style rulesets, so never yank those players back.
        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());
        boolean fallingIsLoss = ruleset != null && (ruleset.isNoDamage() || ruleset.isBreakableFloor());
        double floorPullMargin = plugin.getConfig().getDouble("duel.arena-floor-pull-margin", 3.0);
        boolean belowFloor = !fallingIsLoss && to.getY() < minY - floorPullMargin;

        // Only enforce horizontally for normal movement. Y is only checked against the
        // arena floor so players who fall through it (broken blocks) get pulled back up
        // instead of falling into the void and leaving a "fake body" behind.
        if (to.getX() >= minX && to.getX() <= maxX && to.getZ() >= minZ && to.getZ() <= maxZ && !belowFloor) return;

        Location safe = to.clone();
        safe.setX(Math.min(Math.max(to.getX(), minX), maxX));
        safe.setZ(Math.min(Math.max(to.getZ(), minZ), maxZ));

        if (belowFloor) {
            Location spawn = arena.getSpawn(duel.getTeamIndex(player.getUniqueId()));
            if (spawn == null) spawn = arena.getCenter();
            if (spawn != null) {
                safe = spawn.clone();
                safe.setYaw(to.getYaw());
                safe.setPitch(to.getPitch());
            }
            player.setFallDistance(0);
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        }

        event.setTo(safe);

        // A setTo() correction alone only sends a move update to the falling player.
        // For a large correction (falling through the arena) send a real teleport packet
        // so every client re-syncs and no "fake body" is left behind.
        if (belowFloor || to.distanceSquared(safe) > 16.0) {
            player.teleport(safe);
        }

        if (duelDebug()) {
            plugin.getLogger().info("[DuelDebug] " + player.getName() + (belowFloor ? " PULLED UP from below arena '" : " CLAMPED outside arena '")
                    + arena.getName() + "' | to=" + fmtLoc(to)
                    + " | box X[" + String.format("%.1f", minX) + ".." + String.format("%.1f", maxX) + "]"
                    + " Z[" + String.format("%.1f", minZ) + ".." + String.format("%.1f", maxZ) + "]"
                    + " Y>=" + String.format("%.1f", minY - floorPullMargin)
                    + " | new=" + fmtLoc(safe));
        }

        long now = System.currentTimeMillis();
        Long last = lastArenaWarning.get(player.getUniqueId());
        if (last == null || now - last > 1500) {
            lastArenaWarning.put(player.getUniqueId(), now);
            player.sendMessage(plugin.getMessages().get("duel.no-leaving-arena"));
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel != null && duel.getState() == DuelState.COUNTDOWN) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDuelManager().shouldTeleportToLobby(player.getUniqueId())) {
            org.bukkit.Location respawn = plugin.getDuelManager().consumeRespawnLocation(player.getUniqueId());
            if (respawn != null) {
                event.setRespawnLocation(respawn);
            }
            return;
        }

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel != null && duel.getState() == DuelState.IN_PROGRESS) {
            DuelTeam team = duel.getTeamOf(player.getUniqueId());
            if (team != null && team.getMembers().contains(player.getUniqueId())) {
                if (team.getAliveMembers().contains(player.getUniqueId())) {
                    Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
                    if (arena != null) {
                        org.bukkit.Location spawn = arena.getSpawn(duel.getTeamIndex(player.getUniqueId()));
                        if (spawn != null) {
                            event.setRespawnLocation(spawn);
                        } else if (arena.getCenter() != null) {
                            event.setRespawnLocation(arena.getCenter());
                        }
                    }
                } else {
                    org.bukkit.Location lobby = plugin.getLobbyLocation();
                    if (lobby != null) {
                        event.setRespawnLocation(lobby);
                    }
                }
            }
            return;
        }
    }

    private String resolveDeathCause(Player victim) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player attacker) {
                return classifyWeapon(attacker.getInventory().getItemInMainHand());
            }
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player) {
                if (damager instanceof Trident) return "trident";
                if (damager instanceof Arrow) return "bow";
                return "projectile";
            }
            return "combat";
        }
        if (lastDamage != null) {
            return lastDamage.getCause().name().toLowerCase();
        }
        return "combat";
    }

    private String classifyWeapon(ItemStack weapon) {
        if (weapon == null || weapon.getType() == Material.AIR) return "fists";
        Material type = weapon.getType();
        if (type == Material.BOW) return "bow";
        if (type == Material.CROSSBOW) return "crossbow";
        if (type.name().contains("SWORD")) return "sword";
        if (type.name().contains("SPEAR")) return "spear";
        if (type == Material.MACE) return "mace";
        return type.name().toLowerCase();
    }

    private boolean duelDebug() {
        return plugin.getConfig().getBoolean("duel.debug", false);
    }

    private String fmtLoc(Location loc) {
        if (loc == null) return "null";
        return String.format("%s %.1f,%.1f,%.1f", loc.getWorld() != null ? loc.getWorld().getName() : "null",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
