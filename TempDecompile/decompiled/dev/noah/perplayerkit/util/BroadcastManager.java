/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.util.CooldownManager;
import java.util.ArrayList;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BroadcastManager {
    private static final int LINE_LENGTH = 60;
    private static final String FIGURE_SPACE = "\u2007";
    private static BroadcastManager instance;
    private final int broadcastDistance = 200;
    private final Plugin plugin;
    private final CooldownManager repairBroadcastCooldown = new CooldownManager(5L);
    private final CooldownManager kitroomBroadcastCooldown = new CooldownManager(15L);
    private final BukkitAudiences audience;
    private final Component prefix;

    public BroadcastManager(Plugin plugin) {
        this.plugin = plugin;
        this.audience = BukkitAudiences.create(plugin);
        this.prefix = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("prefix", "<gray>[<aqua>Kits</aqua>]</gray> "));
        instance = this;
    }

    public static BroadcastManager get() {
        if (instance == null) {
            throw new IllegalStateException("BroadcastManager has not been initialized yet!");
        }
        return instance;
    }

    public static Component generateBroadcastComponent(String message) {
        String strikeThroughLine = "<gray>" + " ".repeat(3) + "<st>" + FIGURE_SPACE.repeat(60) + "</st>";
        int messageLength = MiniMessage.miniMessage().stripTags(message).length();
        int padding = (60 - messageLength) / 2;
        String formattedMessage = strikeThroughLine + "\n\n" + " ".repeat(3) + FIGURE_SPACE.repeat(Math.max(padding, 0)) + message + "\n\n" + strikeThroughLine;
        return MiniMessage.miniMessage().deserialize(formattedMessage);
    }

    private boolean isKitLoadingMessage(MessageKey key) {
        return key == MessageKey.PLAYER_LOADED_PRIVATE_KIT || key == MessageKey.PLAYER_LOADED_PUBLIC_KIT || key == MessageKey.PLAYER_LOADED_ENDER_CHEST;
    }

    private void broadcastMessage(Player player, String message, String permission) {
        World world = player.getWorld();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = PlaceholderAPI.setPlaceholders((Player)player, (String)message);
        }
        for (Player broadcastPlayer : world.getPlayers()) {
            if (!(broadcastPlayer.getLocation().distance(player.getLocation()) < 200.0) || !broadcastPlayer.hasPermission(permission)) continue;
            this.audience.player(broadcastPlayer).sendMessage(this.prefix.append((Component)MiniMessage.miniMessage().deserialize(message)));
        }
    }

    private void broadcastMessage(Player player, MessageKey key, CooldownManager cooldownManager) {
        if (!this.plugin.getConfig().getBoolean("feature.broadcast-on-player-action", true)) {
            return;
        }
        if (this.plugin.getConfig().getBoolean("messages.disable-kit-messages", false)) {
            return;
        }
        if (this.isKitLoadingMessage(key) && !this.plugin.getConfig().getBoolean("feature.broadcast-kit-messages", true)) {
            return;
        }
        if (cooldownManager != null && cooldownManager.isOnCooldown(player)) {
            return;
        }
        String messageConfigPath = key.getKey();
        String enabledPath = messageConfigPath + ".enabled";
        String messagePath = messageConfigPath + ".message";
        String permissionPath = messageConfigPath + ".permission";
        if (!this.plugin.getConfig().getBoolean(enabledPath, true)) {
            return;
        }
        String message = this.plugin.getConfig().getString(messagePath, "<gray><aqua>%player%</aqua> performed an action.</gray>");
        String permission = this.plugin.getConfig().getString(permissionPath, "perplayerkit.kitnotify");
        String playerName = this.plugin.getConfig().getBoolean("use-display-name", false) ? player.getDisplayName() : player.getName();
        message = message.replace("%player%", playerName);
        this.broadcastMessage(player, message, permission);
        if (cooldownManager != null) {
            cooldownManager.setCooldown(player);
        }
    }

    public void broadcastPlayerRepaired(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_REPAIRED, this.repairBroadcastCooldown);
    }

    public void broadcastPlayerHealed(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_HEALED, null);
    }

    public void broadcastPlayerOpenedKitRoom(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_OPENED_KIT_ROOM, this.kitroomBroadcastCooldown);
    }

    public void broadcastPlayerLoadedPrivateKit(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_LOADED_PRIVATE_KIT, null);
    }

    public void broadcastPlayerLoadedPublicKit(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_LOADED_PUBLIC_KIT, null);
    }

    public void broadcastPlayerLoadedEnderChest(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_LOADED_ENDER_CHEST, null);
    }

    public void broadcastPlayerCopiedKit(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_COPIED_KIT, null);
    }

    public void broadcastPlayerCopiedEC(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_COPIED_EC, null);
    }

    public void broadcastPlayerRegeared(Player player) {
        this.broadcastMessage(player, MessageKey.PLAYER_REGEARED, null);
    }

    public void startScheduledBroadcast() {
        ArrayList messages = new ArrayList();
        this.plugin.getConfig().getStringList("scheduled-broadcast.messages").forEach(message -> messages.add(BroadcastManager.generateBroadcastComponent(message)));
        int[] index = new int[]{0};
        if (this.plugin.getConfig().getBoolean("scheduled-broadcast.enabled")) {
            Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    this.audience.player(player).sendMessage((Component)messages.get(index[0]));
                }
                index[0] = (index[0] + 1) % messages.size();
            }, 0L, (long)this.plugin.getConfig().getInt("scheduled-broadcast.period") * 20L);
        }
    }

    public void sendComponentMessage(Player player, Component message) {
        this.audience.player(player).sendMessage(message);
    }

    public static enum MessageKey {
        PLAYER_REPAIRED("messages.player-repaired"),
        PLAYER_HEALED("messages.player-healed"),
        PLAYER_OPENED_KIT_ROOM("messages.player-opened-kit-room"),
        PLAYER_LOADED_PRIVATE_KIT("messages.player-loaded-private-kit"),
        PLAYER_LOADED_PUBLIC_KIT("messages.player-loaded-public-kit"),
        PLAYER_LOADED_ENDER_CHEST("messages.player-loaded-enderchest"),
        PLAYER_COPIED_KIT("messages.player-copied-kit"),
        PLAYER_COPIED_EC("messages.player-copied-ec"),
        PLAYER_REGEARED("messages.player-regeared");

        private final String key;

        private MessageKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
