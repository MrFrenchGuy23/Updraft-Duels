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
import com.updraftduels.model.Arena;
import com.updraftduels.model.DuelType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {
    private final UpdraftDuels plugin;
    private final Map<String, Queue<UUID>> queues;
    private final Map<UUID, String> playerQueues;
    private final Map<UUID, Boolean> rankedPreferences;
    private final Map<UUID, String> kitNames;
    private final Queue<UUID> rtpQueue;

    public QueueManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.queues = new ConcurrentHashMap<>();
        this.playerQueues = new ConcurrentHashMap<>();
        this.rankedPreferences = new ConcurrentHashMap<>();
        this.kitNames = new ConcurrentHashMap<>();
        this.rtpQueue = new LinkedList<>();
    }

    public boolean joinQueue(UUID uuid, String arenaName) {
        if (playerQueues.containsKey(uuid)) return false;

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || arena.isInUse()) return false;

        Queue<UUID> queue = queues.computeIfAbsent(arenaName.toLowerCase(), k -> new LinkedList<>());
        queue.add(uuid);
        playerQueues.put(uuid, arenaName.toLowerCase());

        matchPlayers(arenaName.toLowerCase());
        return true;
    }

    public boolean joinRTPQueue(UUID uuid) {
        if (playerQueues.containsKey(uuid)) return false;
        if (plugin.getAntiSpamManager().isOnCooldown(uuid, "queue-join")) return false;

        rtpQueue.add(uuid);
        playerQueues.put(uuid, "rtp");
        plugin.getAntiSpamManager().setCooldown(uuid, "queue-join");

        matchRTPPlayers();
        return true;
    }

    public boolean joinGamemodeQueue(UUID uuid, String gamemode) {
        return joinGamemodeQueue(uuid, gamemode, false);
    }

    public boolean joinGamemodeQueue(UUID uuid, String gamemode, boolean ranked) {
        if (playerQueues.containsKey(uuid)) return false;
        if (plugin.getAntiSpamManager().isOnCooldown(uuid, "queue-join")) return false;

        String key = "gm:" + gamemode.toLowerCase() + (ranked ? ":ranked" : ":unranked");
        rankedPreferences.put(uuid, ranked);
        kitNames.put(uuid, gamemode);
        Queue<UUID> queue = queues.computeIfAbsent(key, k -> new LinkedList<>());
        queue.add(uuid);
        playerQueues.put(uuid, key);

        plugin.getAntiSpamManager().setCooldown(uuid, "queue-join");

        matchGamemodePlayers(key, gamemode);
        return true;
    }

    private void matchGamemodePlayers(String queueKey, String gamemode) {
        Queue<UUID> queue = queues.get(queueKey);
        if (queue == null) return;

        queue.removeIf(uuid -> {
            if (Bukkit.getPlayer(uuid) == null || plugin.getDuelManager().isInDuel(uuid)) {
                playerQueues.remove(uuid);
                rankedPreferences.remove(uuid);
                kitNames.remove(uuid);
                return true;
            }
            return false;
        });

        if (queue.size() < 2) return;

        UUID player1 = queue.poll();
        UUID player2 = queue.poll();

        if (player1 == null || player2 == null) return;

        playerQueues.remove(player1);
        playerQueues.remove(player2);

        boolean ranked = rankedPreferences.remove(player1, true) || rankedPreferences.remove(player2, true);
        rankedPreferences.remove(player1);
        rankedPreferences.remove(player2);

        String kit1 = kitNames.remove(player1);
        String kit2 = kitNames.remove(player2);
        String kitName = kit1 != null ? kit1 : (kit2 != null ? kit2 : gamemode);

        Arena arena = plugin.getArenaManager().getRandomAvailableArenaForGamemode(kitName);
        if (arena == null) {
            queue.add(player1);
            queue.add(player2);
            playerQueues.put(player1, queueKey);
            playerQueues.put(player2, queueKey);
            if (ranked) {
                rankedPreferences.put(player1, true);
                rankedPreferences.put(player2, true);
            }
            kitNames.put(player1, kitName);
            kitNames.put(player2, kitName);
            return;
        }

        createDuelFromMatch(queueKey, kitName, arena.getName(), ranked, player1, player2);
    }

    private void createDuelFromMatch(String queueKey, String kitName, String arenaName, boolean ranked,
                                     UUID player1, UUID player2) {
        var request = plugin.getDuelManager().createRequest(player1, player2,
                DuelType.SOLO, kitName, ranked);
        request.setArenaName(arenaName);
        boolean accepted = plugin.getDuelManager().acceptRequest(request.getRequestId(), player2);

        if (!accepted) {
            requeueGamemodePlayers(queueKey, kitName, ranked, player1, player2);
            return;
        }

        var p1 = Bukkit.getPlayer(player1);
        var p2 = Bukkit.getPlayer(player2);
        if (p1 != null) {
            showMatchFoundTitle(p1, arenaName);
            p1.sendMessage(plugin.getMessages().get("queue.match-found", "%arena%", arenaName));
        }
        if (p2 != null) {
            showMatchFoundTitle(p2, arenaName);
            p2.sendMessage(plugin.getMessages().get("queue.match-found", "%arena%", arenaName));
        }
    }

    private void requeueGamemodePlayers(String queueKey, String kitName, boolean ranked,
                                        UUID player1, UUID player2) {
        Queue<UUID> queue = queues.computeIfAbsent(queueKey, k -> new LinkedList<>());
        if (player1 != null && Bukkit.getPlayer(player1) != null
                && !plugin.getDuelManager().isInDuel(player1) && !playerQueues.containsKey(player1)) {
            queue.add(player1);
            playerQueues.put(player1, queueKey);
            rankedPreferences.put(player1, ranked);
            kitNames.put(player1, kitName);
        }
        if (player2 != null && Bukkit.getPlayer(player2) != null
                && !plugin.getDuelManager().isInDuel(player2) && !playerQueues.containsKey(player2)) {
            queue.add(player2);
            playerQueues.put(player2, queueKey);
            rankedPreferences.put(player2, ranked);
            kitNames.put(player2, kitName);
        }
    }

    public void onPlayerDisconnect(UUID uuid) {
        leaveQueue(uuid);
    }

    public int getGamemodeQueueSize(String gamemode) {
        String key = gamemode.toLowerCase();
        return getQueueSize("gm:" + key + ":unranked") + getQueueSize("gm:" + key + ":ranked");
    }

    public int getGamemodeFightingCount(String gamemode) {
        String kit = gamemode;
        FileConfiguration config = plugin.getExtraConfig("gamemodes.yml");
        if (config != null && config.contains(gamemode)) {
            kit = config.getString(gamemode + ".kit", gamemode);
        }
        String kitName = kit;
        return (int) plugin.getDuelManager().getActiveDuels().stream()
                .filter(d -> d.getState() == com.updraftduels.model.DuelState.IN_PROGRESS)
                .filter(d -> kitName.equalsIgnoreCase(d.getRulesetId())
                        || ("kit:" + kitName).equalsIgnoreCase(d.getRulesetId()))
                .mapToInt(d -> d.getAllParticipants().size())
                .sum();
    }

    public boolean leaveQueue(UUID uuid) {
        String arenaName = playerQueues.remove(uuid);
        rankedPreferences.remove(uuid);
        kitNames.remove(uuid);
        if (arenaName == null) return false;

        if (arenaName.equals("rtp")) {
            rtpQueue.remove(uuid);
            return true;
        }

        Queue<UUID> queue = queues.get(arenaName);
        if (queue != null) {
            queue.remove(uuid);
            if (queue.isEmpty()) {
                queues.remove(arenaName);
            }
        }
        return true;
    }

    private void matchRTPPlayers() {
        if (rtpQueue.size() < 2) return;

        UUID player1 = rtpQueue.poll();
        UUID player2 = rtpQueue.poll();

        if (player1 == null || player2 == null) {
            if (player1 != null) rtpQueue.add(player1);
            if (player2 != null) rtpQueue.add(player2);
            return;
        }

        playerQueues.remove(player1);
        playerQueues.remove(player2);

        World world = pickRTPWorld();
        if (world == null) {
            String msg = com.updraftduels.util.ColorUtil.colorizePrefix(
                    "&cNo RTP world is configured or loaded. Check rtpqueue.world in config.yml.");
            Player p1 = Bukkit.getPlayer(player1);
            Player p2 = Bukkit.getPlayer(player2);
            if (p1 != null) p1.sendMessage(msg);
            if (p2 != null) p2.sendMessage(msg);
            requeueRTPPlayers(player1, player2);
            return;
        }
        String worldName = world.getName();

        int countdown = Math.max(1, plugin.getConfig().getInt("rtpqueue.countdown-seconds", 5));
        boolean started = plugin.getDuelManager().startRTPDuel(player1, player2, world, countdown);

        if (!started) {
            requeueRTPPlayers(player1, player2);
            return;
        }

        var p1 = Bukkit.getPlayer(player1);
        var p2 = Bukkit.getPlayer(player2);
        if (p1 != null) {
            showRTPMatchActionBar(p1, worldName);
            p1.sendMessage(plugin.getMessages().get("queue.rtp-match", "%arena%", worldName));
        }
        if (p2 != null) {
            showRTPMatchActionBar(p2, worldName);
            p2.sendMessage(plugin.getMessages().get("queue.rtp-match", "%arena%", worldName));
        }
    }

    private void requeueRTPPlayers(UUID player1, UUID player2) {
        if (player1 != null && Bukkit.getPlayer(player1) != null && !playerQueues.containsKey(player1)) {
            rtpQueue.add(player1);
            playerQueues.put(player1, "rtp");
        }
        if (player2 != null && Bukkit.getPlayer(player2) != null && !playerQueues.containsKey(player2)) {
            rtpQueue.add(player2);
            playerQueues.put(player2, "rtp");
        }
    }

    private void showRTPMatchActionBar(Player player, String worldName) {
        String message = plugin.getConfig().getString("rtpqueue.match-message",
                "&aMatch Found &7| &fTeleporting to the RTP world")
                .replace("%world%", worldName);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(com.updraftduels.util.ColorUtil.colorize(message)));
    }

    private World pickRTPWorld() {
        FileConfiguration config = plugin.getConfig();
        List<String> names = new ArrayList<>();
        Object value = config.get("rtpqueue.world");
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) names.add(o.toString());
            }
        } else if (value instanceof String s && !s.isEmpty()) {
            names.add(s);
        }

        List<World> loaded = names.stream()
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull)
                .toList();
        if (loaded.isEmpty()) return null;
        return loaded.get(new Random().nextInt(loaded.size()));
    }

    private void matchPlayers(String arenaName) {
        Queue<UUID> queue = queues.get(arenaName);
        if (queue == null || queue.size() < 2) return;

        UUID player1 = queue.poll();
        UUID player2 = queue.poll();

        if (player1 == null || player2 == null) return;

        playerQueues.remove(player1);
        playerQueues.remove(player2);

        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null || arena.isInUse()) {
            if (player1 != null) {
                queue.add(player1);
                playerQueues.put(player1, arenaName);
            }
            if (player2 != null) {
                queue.add(player2);
                playerQueues.put(player2, arenaName);
            }
            return;
        }

        var request = plugin.getDuelManager().createRequest(player1, player2,
                com.updraftduels.model.DuelType.SOLO, "default");
        request.setArenaName(arena.getName());
        boolean accepted = plugin.getDuelManager().acceptRequest(request.getRequestId(), player2);

        if (!accepted) {
            if (player1 != null && Bukkit.getPlayer(player1) != null && !playerQueues.containsKey(player1)) {
                queue.add(player1);
                playerQueues.put(player1, arenaName);
            }
            if (player2 != null && Bukkit.getPlayer(player2) != null && !playerQueues.containsKey(player2)) {
                queue.add(player2);
                playerQueues.put(player2, arenaName);
            }
            return;
        }

        var p1 = Bukkit.getPlayer(player1);
        var p2 = Bukkit.getPlayer(player2);
        if (p1 != null) {
            showMatchFoundTitle(p1, arena.getName());
            p1.sendMessage(plugin.getMessages().get("queue.match-found", "%arena%", arena.getName()));
        }
        if (p2 != null) {
            showMatchFoundTitle(p2, arena.getName());
            p2.sendMessage(plugin.getMessages().get("queue.match-found", "%arena%", arena.getName()));
        }
    }

    private void showMatchFoundTitle(Player player, String arenaName) {
        String title = plugin.getConfig().getString("titles.match-found.title", "&aMatch Found");
        String subtitle = plugin.getConfig().getString("titles.match-found.subtitle", "&7Teleporting to &f%arena%")
                .replace("%arena%", arenaName);
        int fadeIn = plugin.getConfig().getInt("titles.match-found.fade-in", 10);
        int stay = plugin.getConfig().getInt("titles.match-found.stay", 40);
        int fadeOut = plugin.getConfig().getInt("titles.match-found.fade-out", 10);
        player.sendTitle(com.updraftduels.util.ColorUtil.colorize(title),
                com.updraftduels.util.ColorUtil.colorize(subtitle), fadeIn, stay, fadeOut);
    }

    public int getQueueSize(String arenaName) {
        Queue<UUID> queue = queues.get(arenaName.toLowerCase());
        return queue != null ? queue.size() : 0;
    }

    public int getRTPQueueSize() {
        return rtpQueue.size();
    }

    public String getQueuedArena(UUID uuid) {
        return playerQueues.get(uuid);
    }

    public boolean isInQueue(UUID uuid) {
        return playerQueues.containsKey(uuid);
    }

    public boolean isRankedMode(UUID uuid) {
        return rankedPreferences.getOrDefault(uuid, false);
    }

    public void setRankedMode(UUID uuid, boolean ranked) {
        rankedPreferences.put(uuid, ranked);
    }

    public Map<String, Queue<UUID>> getAllQueues() {
        return queues;
    }
}
