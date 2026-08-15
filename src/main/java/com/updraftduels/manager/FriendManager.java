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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

public class FriendManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Set<UUID>> friendsCache;
    private final Map<UUID, Set<UUID>> autoAcceptCache;

    public FriendManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.friendsCache = new ConcurrentHashMap<>();
        this.autoAcceptCache = new ConcurrentHashMap<>();
    }

    public void addFriend(UUID uuid, UUID friendUUID) {
        friendsCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(friendUUID);
        friendsCache.computeIfAbsent(friendUUID, k -> ConcurrentHashMap.newKeySet()).add(uuid);

        plugin.getDatabase().saveFriend(uuid, friendUUID, false);
        plugin.getDatabase().saveFriend(friendUUID, uuid, false);
    }

    public void removeFriend(UUID uuid, UUID friendUUID) {
        Set<UUID> friends = friendsCache.get(uuid);
        if (friends != null) friends.remove(friendUUID);

        Set<UUID> reverseFriends = friendsCache.get(friendUUID);
        if (reverseFriends != null) reverseFriends.remove(uuid);

        Set<UUID> autoAccept = autoAcceptCache.get(uuid);
        if (autoAccept != null) autoAccept.remove(friendUUID);
        Set<UUID> reverseAutoAccept = autoAcceptCache.get(friendUUID);
        if (reverseAutoAccept != null) reverseAutoAccept.remove(uuid);

        plugin.getDatabase().removeFriend(uuid, friendUUID);
        plugin.getDatabase().removeFriend(friendUUID, uuid);
    }

    public Set<UUID> getFriends(UUID uuid) {
        Set<UUID> cached = friendsCache.get(uuid);
        if (cached != null) return cached;

        loadFriends(uuid);
        return Collections.emptySet();
    }

    public boolean isFriend(UUID uuid, UUID friendUUID) {
        return getFriends(uuid).contains(friendUUID);
    }

    public void toggleAutoAccept(UUID uuid, UUID friendUUID) {
        Set<UUID> autoAccept = autoAcceptCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        boolean toggled;
        synchronized (autoAccept) {
            toggled = !autoAccept.contains(friendUUID);
            if (toggled) {
                autoAccept.add(friendUUID);
            } else {
                autoAccept.remove(friendUUID);
            }
        }
        plugin.getDatabase().saveFriend(uuid, friendUUID, toggled);
    }

    public boolean getAutoAccept(UUID uuid, UUID friendUUID) {
        Set<UUID> autoAccept = autoAcceptCache.get(uuid);
        if (autoAccept == null) {
            loadFriends(uuid);
            return false;
        }
        return autoAccept.contains(friendUUID);
    }

    public boolean shouldAutoAccept(UUID uuid, UUID challengerUUID) {
        return getAutoAccept(uuid, challengerUUID) && isFriend(uuid, challengerUUID);
    }

    public void notifyFriendOnline(Player player) {
        for (UUID friendUUID : getFriends(player.getUniqueId())) {
            Player friend = Bukkit.getPlayer(friendUUID);
            if (friend != null && friend.isOnline()) {
                String msg = plugin.getMessages().get("friend.friend-online", "%player%", player.getName());
                friend.sendMessage(msg);
            }
        }
    }

    public Map<UUID, Boolean> getFriendsWithStatus(UUID uuid) {
        Map<UUID, Boolean> result = new LinkedHashMap<>();
        for (UUID friendUUID : getFriends(uuid)) {
            Player friend = Bukkit.getPlayer(friendUUID);
            result.put(friendUUID, friend != null && friend.isOnline());
        }
        return result;
    }

    public void loadFriends(UUID uuid) {
        plugin.getDatabase().getFriends(uuid).thenAccept(friends -> {
            friendsCache.put(uuid, friends);
            if (plugin.getConfig().getBoolean("friends.notify-online", true)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    notifyFriendOnline(player);
                }
            }
        });
        plugin.getDatabase().getAutoAccepted(uuid).thenAccept(auto -> {
            autoAcceptCache.put(uuid, auto);
        });
    }
}
