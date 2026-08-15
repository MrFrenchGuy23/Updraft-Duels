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
import com.updraftduels.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DeathMessageListener implements Listener {
    private final UpdraftDuels plugin;

    public DeathMessageListener(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("death-messages.enabled", true)) return;

        Player victim = event.getEntity();
        if (plugin.getDuelManager().isInDuel(victim.getUniqueId())) {
            return;
        }

        String victimName = victim.getName();
        String message;

        Player killer = victim.getKiller();
        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon != null && weapon.getType() != Material.AIR) {
                message = get("death-messages.killed-item",
                        "%victim%", victimName,
                        "%killer%", killer.getName(),
                        "%item%", formatItem(weapon));
            } else {
                message = get("death-messages.killed",
                        "%victim%", victimName,
                        "%killer%", killer.getName());
            }
        } else {
            String cause = "default";
            EntityDamageEvent lastDamage = victim.getLastDamageCause();
            if (lastDamage != null) {
                cause = lastDamage.getCause().name().toLowerCase();
            }
            message = causeMessage(cause, victimName);
        }

        if (message != null) {
            event.setDeathMessage(ColorUtil.colorize(message));
        }
    }

    private String causeMessage(String cause, String victimName) {
        return switch (cause) {
            case "fall", "fly_into_wall" -> get("death-messages.fall", "%victim%", victimName);
            case "fire", "fire_tick", "lava", "melting", "hot_floor" -> get("death-messages.lava", "%victim%", victimName);
            case "drown" -> get("death-messages.drown", "%victim%", victimName);
            case "void", "falling_block" -> get("death-messages.void", "%victim%", victimName);
            case "block_explosion", "entity_explosion" -> get("death-messages.explosion", "%victim%", victimName);
            case "contact", "stinging" -> get("death-messages.cacti", "%victim%", victimName);
            case "starvation" -> get("death-messages.starve", "%victim%", victimName);
            case "magic", "poison", "wither", "dragon_breath", "thorns" -> get("death-messages.magic", "%victim%", victimName);
            default -> get("death-messages.default", "%victim%", victimName);
        };
    }

    private String get(String key, String... replacements) {
        String message = plugin.getConfig().getString(key);
        if (message == null) return null;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    private String formatItem(ItemStack item) {
        String name = item.getType().name().replace("_", " ").toLowerCase();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return Arrays.stream(name.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
