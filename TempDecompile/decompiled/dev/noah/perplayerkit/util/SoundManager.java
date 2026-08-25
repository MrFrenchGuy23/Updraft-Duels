/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
    private static final String BASE_KEY = "sounds.";

    private static Sound getSound(String key, String defaultName) {
        String path = BASE_KEY + key;
        String soundName = PerPlayerKit.getPlugin().getConfig().getString(path, defaultName);
        try {
            return Sound.valueOf((String)soundName);
        }
        catch (IllegalArgumentException ex) {
            PerPlayerKit.getPlugin().getLogger().warning("Invalid sound '" + soundName + "' for config key '" + path + "'. Using default '" + defaultName + "'.");
            return Sound.valueOf((String)defaultName);
        }
    }

    public static void playSuccess(Player player) {
        SoundManager.play(player, SoundManager.getSound("success", "ENTITY_PLAYER_LEVELUP"));
    }

    public static void playFailure(Player player) {
        SoundManager.play(player, SoundManager.getSound("failure", "ENTITY_ITEM_BREAK"));
    }

    public static void playClick(Player player) {
        SoundManager.play(player, SoundManager.getSound("click", "UI_BUTTON_CLICK"));
    }

    public static void playOpenGui(Player player) {
        SoundManager.play(player, SoundManager.getSound("open_gui", "UI_BUTTON_CLICK"));
    }

    public static void playCloseGui(Player player) {
        SoundManager.play(player, SoundManager.getSound("close_gui", "UI_BUTTON_CLICK"));
    }

    private static void play(Player player, Sound sound) {
        if (!PerPlayerKit.getPlugin().getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}
