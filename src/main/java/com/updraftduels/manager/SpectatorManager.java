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
package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Duel;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpectatorManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, UUID> spectatingTarget;
    private final Map<UUID, Float> originalFlySpeed;
    private final Map<UUID, GameMode> originalGameMode;
    private final Set<UUID> freeCamEnabled;
    private final Set<UUID> vanishedSpectators;

    public SpectatorManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.spectatingTarget = new ConcurrentHashMap<>();
        this.originalFlySpeed = new ConcurrentHashMap<>();
        this.originalGameMode = new ConcurrentHashMap<>();
        this.freeCamEnabled = ConcurrentHashMap.newKeySet();
        this.vanishedSpectators = ConcurrentHashMap.newKeySet();
    }

    public void startSpectating(Player spectator, Player target, Duel duel) {
        UUID specUUID = spectator.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        originalGameMode.put(specUUID, spectator.getGameMode());
        originalFlySpeed.put(specUUID, spectator.getFlySpeed());

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(target.getLocation());
        spectator.setAllowFlight(true);
        spectator.setFlying(true);
        spectator.setFlySpeed(0.5f);
        spectator.setCollidable(false);
        setVanished(spectator, true);

        duel.getSpectators().add(specUUID);
        spectatingTarget.put(specUUID, targetUUID);

        spectator.sendMessage(plugin.getMessages().get("spectator.now-spectating", "%player%", target.getName()));
    }

    public void stopSpectating(Player spectator) {
        UUID specUUID = spectator.getUniqueId();

        spectator.setGameMode(originalGameMode.getOrDefault(specUUID, GameMode.SURVIVAL));
        spectator.setFlySpeed(originalFlySpeed.getOrDefault(specUUID, 0.1f));
        spectator.setFlying(false);
        spectator.setCollidable(true);
        spectator.setSneaking(false);
        spectator.setInvisible(false);
        spectator.setAllowFlight(false);
        setVanished(spectator, false);

        for (Duel duel : plugin.getDuelManager().getActiveDuels()) {
            duel.getSpectators().remove(specUUID);
        }

        plugin.getScoreboardManager().removeScoreboard(spectator);

        spectatingTarget.remove(specUUID);
        originalFlySpeed.remove(specUUID);
        originalGameMode.remove(specUUID);
        freeCamEnabled.remove(specUUID);
        vanishedSpectators.remove(specUUID);

        spectator.sendMessage(plugin.getMessages().get("spectator.stopped-spectating"));
    }

    public void toggleFreeCam(Player spectator) {
        UUID uuid = spectator.getUniqueId();
        if (freeCamEnabled.contains(uuid)) {
            freeCamEnabled.remove(uuid);
            spectator.setGameMode(GameMode.SPECTATOR);
            spectator.sendMessage(plugin.getMessages().get("spectator.free-cam-disabled"));
        } else {
            freeCamEnabled.add(uuid);
            spectator.setGameMode(GameMode.CREATIVE);
            spectator.setAllowFlight(true);
            spectator.setFlying(true);
            spectator.setInvisible(true);
            spectator.sendMessage(plugin.getMessages().get("spectator.free-cam-enabled"));
        }
    }

    public void toggleVanish(Player spectator) {
        setVanished(spectator, !vanishedSpectators.contains(spectator.getUniqueId()));
    }

    private void setVanished(Player spectator, boolean vanished) {
        if (vanished) {
            vanishedSpectators.add(spectator.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(spectator.getUniqueId())) {
                    online.hidePlayer(plugin, spectator);
                }
            }
            spectator.sendMessage(plugin.getMessages().get("spectator.vanish-enabled"));
        } else {
            vanishedSpectators.remove(spectator.getUniqueId());
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, spectator);
            }
            spectator.sendMessage(plugin.getMessages().get("spectator.vanish-disabled"));
        }
    }

    public void setFlySpeed(Player spectator, float speed) {
        spectator.setFlySpeed(Math.max(0.1f, Math.min(1.0f, speed)));
    }

    public void followPlayer(Player spectator, Player target) {
        spectator.teleport(target.getLocation());
        spectator.sendMessage(plugin.getMessages().get("spectator.following", "%player%", target.getName()));
    }

    public UUID getTarget(UUID spectatorUUID) { return spectatingTarget.get(spectatorUUID); }
    public boolean isSpectating(UUID uuid) { return spectatingTarget.containsKey(uuid); }
    public boolean isInFreeCam(UUID uuid) { return freeCamEnabled.contains(uuid); }
    public boolean isVanished(UUID uuid) { return vanishedSpectators.contains(uuid); }

    public void onDuelEnd(Duel duel) {
        for (UUID specUUID : new ArrayList<>(duel.getSpectators())) {
            Player spectator = Bukkit.getPlayer(specUUID);
            if (spectator != null) {
                stopSpectating(spectator);
                spectator.teleport(spectator.getWorld().getSpawnLocation());
            }
        }
    }

    public void onPlayerQuit(UUID uuid) {
        spectatingTarget.remove(uuid);
        originalFlySpeed.remove(uuid);
        originalGameMode.remove(uuid);
        freeCamEnabled.remove(uuid);
        vanishedSpectators.remove(uuid);
    }
}
