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
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;

    public AntiSpamManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public boolean isOnCooldown(UUID uuid, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long lastUse = playerCooldowns.get(type);
        if (lastUse == null) return false;

        int cooldownSeconds = getCooldownSeconds(type);
        return System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L;
    }

    public int getRemainingSeconds(UUID uuid, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return 0;
        Long lastUse = playerCooldowns.get(type);
        if (lastUse == null) return 0;

        int cooldownSeconds = getCooldownSeconds(type);
        long remaining = cooldownSeconds - (System.currentTimeMillis() - lastUse) / 1000;
        return remaining > 0 ? (int) remaining : 0;
    }

    public void setCooldown(UUID uuid, String type) {
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(type, System.currentTimeMillis());
    }

    public void clearCooldown(UUID uuid, String type) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(type);
        }
    }

    public void clearAll(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public boolean checkAndSet(UUID uuid, String type) {
        long now = System.currentTimeMillis();
        long cooldownMs = getCooldownSeconds(type) * 1000L;

        boolean[] allowed = new boolean[1];
        cooldowns.compute(uuid, (k, existing) -> {
            if (existing == null) {
                existing = new ConcurrentHashMap<>();
            }
            Long lastUse = existing.get(type);
            if (lastUse != null && now - lastUse < cooldownMs) {
                allowed[0] = false;
                return existing;
            }
            existing.put(type, now);
            allowed[0] = true;
            return existing;
        });
        return allowed[0];
    }

    private int getCooldownSeconds(String type) {
        return switch (type) {
            case "duel-request" -> plugin.getConfig().getInt("anti-spam.duel-request-cooldown-seconds", 5);
            case "queue-join" -> plugin.getConfig().getInt("anti-spam.queue-join-cooldown-seconds", 3);
            case "tournament-join" -> plugin.getConfig().getInt("anti-spam.tournament-join-cooldown-seconds", 10);
            case "message" -> plugin.getConfig().getInt("anti-spam.message-cooldown-seconds", 2);
            default -> 5;
        };
    }
}
