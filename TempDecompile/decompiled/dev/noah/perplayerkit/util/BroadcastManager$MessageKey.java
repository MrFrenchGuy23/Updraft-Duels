/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.util;

public static enum BroadcastManager.MessageKey {
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

    private BroadcastManager.MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
