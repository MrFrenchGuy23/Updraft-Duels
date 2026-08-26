/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

public class StyleManager {
    private static StyleManager instance;
    private final Plugin plugin;
    private Material glassMaterial;
    private String titleColor;

    public StyleManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.loadConfig();
    }

    public static StyleManager get() {
        return instance;
    }

    public void loadConfig() {
        try {
            this.glassMaterial = Material.valueOf((String)this.plugin.getConfig().getString("interface.glass-material", "BLUE_STAINED_GLASS_PANE"));
        }
        catch (IllegalArgumentException e) {
            this.glassMaterial = Material.BLUE_STAINED_GLASS_PANE;
        }
        String colorTag = this.plugin.getConfig().getString("interface.main-color", "<blue>");
        this.titleColor = this.miniMessageToLegacy(colorTag);
    }

    private String miniMessageToLegacy(String miniMessage) {
        try {
            Object component = MiniMessage.miniMessage().deserialize(miniMessage + "test");
            return LegacyComponentSerializer.legacySection().serialize((Component)component).replace("test", "");
        }
        catch (Exception e) {
            return "<blue>";
        }
    }

    public static String convertMiniMessage(String miniMessage) {
        try {
            Object component = MiniMessage.miniMessage().deserialize(miniMessage + "test");
            return LegacyComponentSerializer.legacySection().serialize((Component)component).replace("test", "");
        }
        catch (Exception e) {
            return miniMessage;
        }
    }

    public Material getGlassMaterial() {
        return this.glassMaterial;
    }

    public String getPrimaryColor() {
        return this.titleColor;
    }
}
