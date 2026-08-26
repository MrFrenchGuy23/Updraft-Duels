/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.util;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CooldownManager {
    private final long cooldownInSeconds;
    private final HashMap<String, Long> cooldownMap;

    public CooldownManager(long cooldownInSeconds) {
        this.cooldownInSeconds = cooldownInSeconds;
        this.cooldownMap = new HashMap();
    }

    public boolean isOnCooldown(String key) {
        if (!this.cooldownMap.containsKey(key)) {
            return false;
        }
        return System.currentTimeMillis() - this.cooldownMap.get(key) < this.cooldownInSeconds * 1000L;
    }

    public boolean isOnCooldown(UUID uuid) {
        return this.isOnCooldown(uuid.toString());
    }

    public boolean isOnCooldown(Player player) {
        return this.isOnCooldown(player.getUniqueId());
    }

    public void setCooldown(String key) {
        this.cooldownMap.put(key, System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid) {
        this.setCooldown(uuid.toString());
    }

    public void setCooldown(Player player) {
        this.setCooldown(player.getUniqueId());
    }

    public int getTimeLeft(String key) {
        return (int)(this.cooldownInSeconds - (System.currentTimeMillis() - this.cooldownMap.get(key)) / 1000L);
    }

    public int getTimeLeft(UUID uuid) {
        return this.getTimeLeft(uuid.toString());
    }

    public int getTimeLeft(Player player) {
        return this.getTimeLeft(player.getUniqueId());
    }
}
