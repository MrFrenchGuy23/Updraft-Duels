/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerUtil {
    public static void repairItem(ItemStack i) {
        if (i != null) {
            ItemMeta meta = i.getItemMeta();
            Damageable damageable = (Damageable)meta;
            if (damageable != null && damageable.hasDamage()) {
                damageable.setDamage(0);
            }
            i.setItemMeta((ItemMeta)damageable);
        }
    }

    public static void repairAll(Player p) {
        for (ItemStack i : p.getInventory().getContents()) {
            PlayerUtil.repairItem(i);
        }
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "All items repaired!");
    }

    public static void healPlayer(Player p) {
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20.0f);
        if (PerPlayerKit.getPlugin().getConfig().getBoolean("feature.heal-remove-effects", false)) {
            p.getActivePotionEffects().forEach(potionEffect -> p.removePotionEffect(potionEffect.getType()));
        }
        p.sendMessage(String.valueOf(ChatColor.GREEN) + "You have been healed!");
    }

    public static void healPlayerSilent(Player p) {
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20.0f);
    }
}
