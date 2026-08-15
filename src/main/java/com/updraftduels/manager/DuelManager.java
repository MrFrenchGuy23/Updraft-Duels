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
import com.updraftduels.model.*;
import com.updraftduels.storage.DatabaseManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Duel> activeDuels;
    private final Map<UUID, DuelRequest> pendingRequests;
    private final Map<UUID, List<DuelRequest>> incomingRequests;
    private final Map<UUID, List<DuelRequest>> outgoingRequests;
    private final Map<UUID, Long> frozenPlayers;
    private final Map<UUID, Integer> pendingCountdowns;
    private final Set<UUID> lobbyTeleportAfterDuel;
    private final Map<UUID, Location> pendingRespawnLocations;
    private final Map<UUID, String> activeDuelContext;
    private final Map<UUID, PendingDuelSelection> pendingDuelSelections;
    private final Map<UUID, OfflineRestoreState> offlineRestores;

    public DuelManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.activeDuels = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.incomingRequests = new ConcurrentHashMap<>();
        this.outgoingRequests = new ConcurrentHashMap<>();
        this.frozenPlayers = new ConcurrentHashMap<>();
        this.pendingCountdowns = new ConcurrentHashMap<>();
        this.lobbyTeleportAfterDuel = ConcurrentHashMap.newKeySet();
        this.pendingRespawnLocations = new ConcurrentHashMap<>();
        this.activeDuelContext = new ConcurrentHashMap<>();
        this.pendingDuelSelections = new ConcurrentHashMap<>();
        this.offlineRestores = new ConcurrentHashMap<>();
    }

    public void startDuelSelection(UUID sender, UUID target) {
        pendingDuelSelections.put(sender, new PendingDuelSelection(sender, target));
    }

    public PendingDuelSelection getPendingDuelSelection(UUID sender) {
        return pendingDuelSelections.get(sender);
    }

    public void removePendingDuelSelection(UUID sender) {
        pendingDuelSelections.remove(sender);
    }

    public boolean sendDuelRequest(Player sender, Player target, String kitName, int rounds) {
        if (kitName == null || kitName.isEmpty() || kitName.equals("default")) {
            String selected = plugin.getRulesetManager().getSelectedRuleset(sender.getUniqueId());
            if (selected != null && plugin.getRulesetManager().hasRuleset(selected)) kitName = selected;
        }
        if (kitName == null || kitName.isEmpty()) kitName = "default";
        if (plugin.getAntiSpamManager().isOnCooldown(sender.getUniqueId(), "duel-request")) {
            plugin.getAntiSpamManager().sendCooldownMessage(sender, "duel-request");
            return false;
        }
        createRequest(sender.getUniqueId(), target.getUniqueId(), DuelType.SOLO, kitName, false, rounds);
        plugin.getAntiSpamManager().setCooldown(sender.getUniqueId(), "duel-request");

        sender.sendMessage(com.updraftduels.util.ColorUtil.colorize(
                plugin.getMessages().get("duel.request-sent", "%player%", target.getName())));
        sender.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                "&7Kit: &f" + kitName + " &8| &7Rounds: &f" + rounds));

        String acceptCmd = "/duel accept " + sender.getName();
        com.updraftduels.util.ChatUtil.sendClickable(target,
                com.updraftduels.util.ColorUtil.colorize(
                        plugin.getMessages().get("duel.request-received", "%player%", sender.getName())),
                acceptCmd, "&aClick to accept duel!");
        return true;
    }

    public DuelRequest createRequest(UUID sender, UUID receiver, DuelType type, String rulesetId) {
        return createRequest(sender, receiver, type, rulesetId, false, 1);
    }

    public DuelRequest createRequest(UUID sender, UUID receiver, DuelType type, String rulesetId, boolean ranked) {
        return createRequest(sender, receiver, type, rulesetId, ranked, 1);
    }

    public DuelRequest createRequest(UUID sender, UUID receiver, DuelType type, String rulesetId, boolean ranked, int rounds) {
        cleanupExpiredRequests();
        DuelRequest request = new DuelRequest(sender, receiver, type, rulesetId, ranked, rounds);
        pendingRequests.put(request.getRequestId(), request);

        outgoingRequests.computeIfAbsent(sender, k -> new ArrayList<>()).add(request);
        incomingRequests.computeIfAbsent(receiver, k -> new ArrayList<>()).add(request);

        return request;
    }

    private void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();
        for (DuelRequest req : new ArrayList<>(pendingRequests.values())) {
            if (req.isExpired()) {
                pendingRequests.remove(req.getRequestId());
                outgoingRequests.getOrDefault(req.getSenderUUID(), new ArrayList<>()).remove(req);
                incomingRequests.getOrDefault(req.getReceiverUUID(), new ArrayList<>()).remove(req);
            }
        }
    }

    public boolean acceptRequest(UUID requestId, UUID acceptorUUID) {
        DuelRequest request = pendingRequests.get(requestId);
        if (request == null || request.isProcessed() || request.isExpired()) return false;
        if (!request.getReceiverUUID().equals(acceptorUUID)) return false;

        request.setAccepted(true);

        if (!startDuel(request)) {
            request.setProcessed(true);
            pendingRequests.remove(request.getRequestId());
            outgoingRequests.getOrDefault(request.getSenderUUID(), new ArrayList<>()).remove(request);
            incomingRequests.getOrDefault(request.getReceiverUUID(), new ArrayList<>()).remove(request);
            return false;
        }

        outgoingRequests.computeIfAbsent(request.getSenderUUID(), k -> new ArrayList<>()).remove(request);
        incomingRequests.computeIfAbsent(request.getReceiverUUID(), k -> new ArrayList<>()).remove(request);
        return true;
    }

    public boolean denyRequest(UUID requestId, UUID denierUUID) {
        DuelRequest request = pendingRequests.get(requestId);
        if (request == null || request.isProcessed()) return false;
        if (!request.getReceiverUUID().equals(denierUUID) && !request.getSenderUUID().equals(denierUUID)) return false;

        request.setProcessed(true);
        pendingRequests.remove(requestId);
        outgoingRequests.computeIfAbsent(request.getSenderUUID(), k -> new ArrayList<>()).remove(request);
        incomingRequests.computeIfAbsent(request.getReceiverUUID(), k -> new ArrayList<>()).remove(request);
        return true;
    }

    public void denyAllIncoming(UUID playerUUID) {
        List<DuelRequest> requests = incomingRequests.getOrDefault(playerUUID, new ArrayList<>());
        for (DuelRequest req : new ArrayList<>(requests)) {
            denyRequest(req.getRequestId(), playerUUID);
        }
    }

    private boolean startDuel(DuelRequest request) {
        UUID senderUUID = request.getSenderUUID();
        UUID receiverUUID = request.getReceiverUUID();

        Player sender = Bukkit.getPlayer(senderUUID);
        Player receiver = Bukkit.getPlayer(receiverUUID);
        if (sender == null || receiver == null) return false;

        if (isInDuel(senderUUID) || isInDuel(receiverUUID)) return false;

        Arena arena = request.getArenaName() != null
                ? plugin.getArenaManager().getArena(request.getArenaName())
                : plugin.getArenaManager().getRandomAvailableArena();
        if (arena == null || arena.isInUse() || arena.isRegenerating() || !arena.isConfigured()) {
            arena = plugin.getArenaManager().getRandomAvailableArena();
            if (arena == null) return false;
        }

        String rulesetId = request.getRulesetId() != null ? request.getRulesetId() : "default";
        Duel duel = new Duel(UUID.randomUUID(), request.getType(), arena.getName());
        duel.setRulesetId(rulesetId);
        duel.setRanked(request.isRanked());
        duel.setRounds(request.getRounds());

        DuelTeam teamA = new DuelTeam(Team.TEAM_A);
        DuelTeam teamB = new DuelTeam(Team.TEAM_B);

        teamA.addMember(senderUUID);
        teamB.addMember(receiverUUID);

        duel.addTeam(teamA);
        duel.addTeam(teamB);

        activeDuels.put(duel.getId(), duel);

        plugin.getQueueManager().leaveQueue(senderUUID);
        plugin.getQueueManager().leaveQueue(receiverUUID);

        savePlayerState(sender, duel);
        savePlayerState(receiver, duel);

        pendingRequests.remove(request.getRequestId());

        int countdown = plugin.getConfig().getInt("duel.countdown-seconds", 3);
        startCountdown(duel, arena, countdown);

        return true;
    }

    public boolean startPartyDuel(Party party1, Party party2, Arena arena, String rulesetId, int rounds) {
        for (UUID member : party1.getMembers()) {
            if (isInDuel(member) || isInQueue(member)) return false;
        }
        for (UUID member : party2.getMembers()) {
            if (isInDuel(member) || isInQueue(member)) return false;
        }
        DuelType type = DuelType.fromTeamSize(party1.getSize());
        if (type == null) return false;

        Duel duel = new Duel(UUID.randomUUID(), type, arena.getName());
        duel.setRulesetId(rulesetId != null ? rulesetId : "default");
        duel.setRounds(Math.max(1, rounds));

        DuelTeam teamA = new DuelTeam(Team.TEAM_A);
        DuelTeam teamB = new DuelTeam(Team.TEAM_B);

        for (UUID uuid : party1.getMembers()) {
            teamA.addMember(uuid);
        }
        for (UUID uuid : party2.getMembers()) {
            teamB.addMember(uuid);
        }

        duel.addTeam(teamA);
        duel.addTeam(teamB);

        activeDuels.put(duel.getId(), duel);

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                savePlayerState(player, duel);
            }
        }

        int countdown = plugin.getConfig().getInt("duel.countdown-seconds", 3);
        startCountdown(duel, arena, countdown);
        return true;
    }

    public boolean startRTPDuel(UUID player1UUID, UUID player2UUID, World world, int countdownSeconds) {
        Player p1 = Bukkit.getPlayer(player1UUID);
        Player p2 = Bukkit.getPlayer(player2UUID);
        if (p1 == null || p2 == null) return false;
        if (isInDuel(player1UUID) || isInDuel(player2UUID)) return false;

        Duel duel = new Duel(UUID.randomUUID(), DuelType.SOLO, world.getName());
        duel.setRulesetId("default");
        duel.setRounds(1);

        DuelTeam teamA = new DuelTeam(Team.TEAM_A);
        teamA.addMember(player1UUID);
        DuelTeam teamB = new DuelTeam(Team.TEAM_B);
        teamB.addMember(player2UUID);
        duel.addTeam(teamA);
        duel.addTeam(teamB);

        activeDuels.put(duel.getId(), duel);

        savePlayerState(p1, duel);
        savePlayerState(p2, duel);

        int radius = plugin.getConfig().getInt("rtpqueue.radius", 1000);
        int minDistance = Math.max(10, plugin.getConfig().getInt("rtpqueue.min-distance", 100));
        int countdown = Math.max(1, countdownSeconds);

        findRandomRTPLocationAsync(world, radius, null, 0).whenComplete((loc1, ex) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    final Location first = loc1 != null ? loc1 : world.getSpawnLocation();
                    findRandomRTPLocationAsync(world, radius, first, minDistance).whenComplete((loc2, ex2) ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (!activeDuels.containsKey(duel.getId())) return;
                                Player a = Bukkit.getPlayer(player1UUID);
                                Player b = Bukkit.getPlayer(player2UUID);
                                if (a == null || b == null) return;
                                Location second = loc2 != null ? loc2 : world.getSpawnLocation();
                                a.teleport(first);
                                b.teleport(second);
                                a.setFallDistance(0);
                                b.setFallDistance(0);
                                startCountdown(duel, null, countdown, true);
                            }));
                }));
        return true;
    }

    private CompletableFuture<Location> findRandomRTPLocationAsync(World world, int radius, Location avoid, double minDistance) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        tryNextRTPCandidate(world, radius, avoid, minDistance, 0, future);
        return future;
    }

    private void tryNextRTPCandidate(World world, int radius, Location avoid, double minDistance,
                                     int attempt, CompletableFuture<Location> future) {
        if (attempt >= 40) {
            future.complete(world.getSpawnLocation());
            return;
        }

        double angle = Math.random() * 2 * Math.PI;
        double dist = radius * Math.sqrt(Math.random());
        int x = (int) Math.round(Math.cos(angle) * dist);
        int z = (int) Math.round(Math.sin(angle) * dist);
        if (avoid != null) {
            double dx = x - avoid.getX();
            double dz = z - avoid.getZ();
            if (dx * dx + dz * dz < minDistance * minDistance) {
                tryNextRTPCandidate(world, radius, avoid, minDistance, attempt + 1, future);
                return;
            }
        }

        int cx = x >> 4;
        int cz = z >> 4;
        if (world.isChunkLoaded(cx, cz)) {
            Location loc = resolveRTPLocation(world, x, z);
            if (loc != null) {
                future.complete(loc);
            } else {
                tryNextRTPCandidate(world, radius, avoid, minDistance, attempt + 1, future);
            }
            return;
        }

        world.getChunkAtAsync(cx, cz).whenComplete((chunk, ex) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (ex != null) {
                        tryNextRTPCandidate(world, radius, avoid, minDistance, attempt + 1, future);
                        return;
                    }
                    Location loc = resolveRTPLocation(world, x, z);
                    if (loc != null) {
                        future.complete(loc);
                    } else {
                        tryNextRTPCandidate(world, radius, avoid, minDistance, attempt + 1, future);
                    }
                }));
    }

    private Location resolveRTPLocation(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        if (y <= 0) return null;
        org.bukkit.block.Block below = world.getBlockAt(x, y - 1, z);
        org.bukkit.Material belowType = below.getType();
        if (belowType.isSolid() && belowType != org.bukkit.Material.WATER
                && belowType != org.bukkit.Material.LAVA && belowType != org.bukkit.Material.LAVA_CAULDRON) {
            return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private void savePlayerState(Player player, Duel duel) {
        duel.getOriginalLocations().put(player.getUniqueId(), player.getLocation().clone());
        duel.getOriginalContents().put(player.getUniqueId(), player.getInventory().getContents().clone());
        duel.getOriginalArmorContents().put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
        duel.getOriginalOffHandContents().put(player.getUniqueId(), player.getInventory().getItemInOffHand().clone());
        duel.getOriginalEnderChestContents().put(player.getUniqueId(), player.getEnderChest().getContents());
        duel.getOriginalHealth().put(player.getUniqueId(), player.getHealth());
        duel.getOriginalFoodLevel().put(player.getUniqueId(), player.getFoodLevel());
        duel.getOriginalGameModes().put(player.getUniqueId(), player.getGameMode());
        duel.getOriginalAllowFlight().put(player.getUniqueId(), player.getAllowFlight());
        duel.getOriginalFlying().put(player.getUniqueId(), player.isFlying());
    }

    private void startCountdown(Duel duel, Arena arena, int seconds) {
        startCountdown(duel, arena, seconds, false);
    }

    private void startCountdown(Duel duel, Arena arena, int seconds, boolean rtpMode) {
        duel.setState(DuelState.COUNTDOWN);
        if (arena != null) {
            arena.setInUse(true);
            arena.setCurrentDuelId(duel.getId());
        }

        if (!rtpMode) {
            plugin.getGateManager().closeGate();
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            if (!rtpMode) {
                DuelTeam team = duel.getTeamOf(uuid);
                Team teamIndex = duel.getTeamIndex(uuid);
                Location spawn = arena.getSpawn(teamIndex);
                if (spawn == null) spawn = arena.getCenter();
                if (spawn != null) {
                    int memberIndex = team != null ? team.getMembers().indexOf(uuid) : 0;
                    Location offset = spawn.clone().add(memberIndex * 0.75, 0, 0);
                    player.teleport(offset);
                    if (duelDebug()) {
                        String box = (arena.getPos1() != null && arena.getPos2() != null)
                                ? " | boxX[" + String.format("%.1f", Math.min(arena.getPos1().getX(), arena.getPos2().getX()))
                                + ".." + String.format("%.1f", Math.max(arena.getPos1().getX(), arena.getPos2().getX())) + "]"
                                + " boxZ[" + String.format("%.1f", Math.min(arena.getPos1().getZ(), arena.getPos2().getZ()))
                                + ".." + String.format("%.1f", Math.max(arena.getPos1().getZ(), arena.getPos2().getZ())) + "]"
                                + " boxY[" + String.format("%.1f", Math.min(arena.getPos1().getY(), arena.getPos2().getY()))
                                + ".." + String.format("%.1f", Math.max(arena.getPos1().getY(), arena.getPos2().getY())) + "]"
                                : " | box=unset";
                        plugin.getLogger().info("[DuelDebug] " + player.getName()
                                + " teleported to arena spawn | arena=" + arena.getName()
                                + " team=" + (teamIndex != null ? teamIndex.name() : "?")
                                + " world=" + (offset.getWorld() != null ? offset.getWorld().getName() : "null")
                                + " x=" + String.format("%.1f", offset.getX())
                                + " y=" + String.format("%.1f", offset.getY())
                                + " z=" + String.format("%.1f", offset.getZ()) + box);
                    }
                } else {
                    if (duelDebug()) plugin.getLogger().info("[DuelDebug] " + player.getName()
                            + " teleport FAILED: arena '" + arena.getName() + "' has no spawn and no center");
                }
            }

            if (plugin.getConfig().getBoolean("general.freeze-on-countdown", true)) {
                frozenPlayers.put(uuid, System.currentTimeMillis());
                player.setWalkSpeed(0f);
                player.setFlySpeed(0f);
            }

            if (!rtpMode) {
                player.getInventory().clear();
                player.getEnderChest().clear();
            }
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setGameMode(GameMode.SURVIVAL);
        }

        applyTeamNametags(duel);

        pendingCountdowns.put(duel.getId(), seconds);

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String teleportMsg = plugin.getMessages().get("duel.teleporting", "%delay%", String.valueOf(seconds));
                player.sendMessage(teleportMsg);
            }
        }

        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (duel.getState() != DuelState.COUNTDOWN) {
                task.cancel();
                return;
            }

            int remaining = pendingCountdowns.getOrDefault(duel.getId(), 0);
            if (remaining <= 0) {
                task.cancel();
                beginDuel(duel, arena, ruleset, rtpMode);
                return;
            }

            for (UUID uuid : duel.getAllParticipants()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    String countdownMsg = plugin.getMessages().get("duel.countdown", "%count%", String.valueOf(remaining));
                    player.sendMessage(countdownMsg);
                    player.sendTitle("", ChatColor.YELLOW + "Starting in " + ChatColor.WHITE + remaining, 0, 25, 0);
                }
            }

            pendingCountdowns.put(duel.getId(), remaining - 1);
        }, 0L, 20L);
    }

    public void applyTeamNametags(Duel duel) {
        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
            if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
                player.setScoreboard(board);
            }
            registerDuelTeams(board, duel);
        }
    }

    public void clearTeamNametags(Duel duel) {
        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
            if (board == null || board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            unregisterDuelTeams(board);
        }
    }

    private void registerDuelTeams(org.bukkit.scoreboard.Scoreboard board, Duel duel) {
        try {
            unregisterDuelTeams(board);
            org.bukkit.scoreboard.Team teamA = board.registerNewTeam("ud_a");
            org.bukkit.scoreboard.Team teamB = board.registerNewTeam("ud_b");
            teamA.setColor(ChatColor.RED);
            teamB.setColor(ChatColor.BLUE);
            for (UUID uuid : duel.getAllParticipants()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                if (duel.getTeamIndex(uuid) == Team.TEAM_A) {
                    teamA.addEntry(player.getName());
                } else if (duel.getTeamIndex(uuid) == Team.TEAM_B) {
                    teamB.addEntry(player.getName());
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void unregisterDuelTeams(org.bukkit.scoreboard.Scoreboard board) {
        org.bukkit.scoreboard.Team teamA = board.getTeam("ud_a");
        if (teamA != null) teamA.unregister();
        org.bukkit.scoreboard.Team teamB = board.getTeam("ud_b");
        if (teamB != null) teamB.unregister();
    }

    private void beginDuel(Duel duel, Arena arena, Ruleset ruleset) {
        beginDuel(duel, arena, ruleset, false);
    }

    private void beginDuel(Duel duel, Arena arena, Ruleset ruleset, boolean rtpMode) {
        if (!rtpMode) {
            plugin.getGateManager().openGate(() -> {
                if (duel.getState() != DuelState.COUNTDOWN || !activeDuels.containsKey(duel.getId())) {
                    return;
                }
                beginDuelCore(duel, arena, ruleset);
            });
            return;
        }
        beginDuelCore(duel, arena, ruleset);
    }

    private void beginDuelCore(Duel duel, Arena arena, Ruleset ruleset) {
        duel.setState(DuelState.IN_PROGRESS);
        duel.setStartTime(System.currentTimeMillis());

        if (duel.isRanked()) {
            int timeLimit = plugin.getConfig().getInt("duel.ranked-time-limit-seconds", 300);
            if (timeLimit > 0) {
                int ticks = timeLimit * 20;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (duel.getState() == DuelState.IN_PROGRESS && activeDuels.containsKey(duel.getId())) {
                        int scoreA = duel.getScoreA();
                        int scoreB = duel.getScoreB();
                        if (scoreA > scoreB) {
                            endDuel(duel, Team.TEAM_A);
                        } else if (scoreB > scoreA) {
                            endDuel(duel, Team.TEAM_B);
                        } else {
                            for (UUID uuid : duel.getAllParticipants()) {
                                Player p = Bukkit.getPlayer(uuid);
                                if (p != null) {
                                    p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&7Time's up, the score is tied."));
                                }
                            }
                            endDuel(duel, null);
                        }
                    }
                }, ticks);

                for (UUID uuid : duel.getAllParticipants()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                                "&7Time limit: &f" + timeLimit / 60 + ":" + String.format("%02d", timeLimit % 60) + " &7minutes"));
                    }
                }
            }
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            frozenPlayers.remove(uuid);
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
            player.closeInventory();

            applyKit(player, duel, ruleset);
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String opponentName = getOpponentName(duel, uuid);
                activeDuelContext.put(uuid, opponentName);
                player.sendTitle(ChatColor.GREEN + "Duel Started", ChatColor.GRAY + "Good luck!", 10, 40, 10);
                String startMsg = plugin.getMessages().get("duel.duel-started");
                player.sendMessage(startMsg);
            }
        }
    }

    private void applyKit(Player player, Duel duel, Ruleset ruleset) {
        if (ruleset != null && ruleset.isFistsOnly()) {
            return;
        }

        if (duel.getRulesetId() != null && duel.getRulesetId().startsWith("kit:")) {
            String kitName = duel.getRulesetId().substring(4);
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit != null) {
                player.getInventory().setContents(kit.getContentsArray());
                player.getInventory().setArmorContents(kit.getArmorContents());
                player.getInventory().setItemInOffHand(kit.getOffHand() != null ? kit.getOffHand() : new ItemStack(Material.AIR));
                return;
            }
        }

        if (duel.getRulesetId() != null && duel.getRulesetId().equals("own_inventory")) {
            org.bukkit.inventory.ItemStack[] original = duel.getOriginalContents().get(player.getUniqueId());
            if (original != null) {
                player.getInventory().setContents(original.clone());
            }
            org.bukkit.inventory.ItemStack[] originalArmor = duel.getOriginalArmorContents().get(player.getUniqueId());
            if (originalArmor != null) {
                player.getInventory().setArmorContents(originalArmor.clone());
            }
            org.bukkit.inventory.ItemStack originalOffHand = duel.getOriginalOffHandContents().get(player.getUniqueId());
            if (originalOffHand != null) {
                player.getInventory().setItemInOffHand(originalOffHand.clone());
            }
            return;
        }

        if (duel.getRulesetId() != null) {
            Kit gamemodeKit = plugin.getKitManager().getKit(duel.getRulesetId());
            if (gamemodeKit != null) {
                player.getInventory().setContents(gamemodeKit.getContentsArray());
                player.getInventory().setArmorContents(gamemodeKit.getArmorContents());
                player.getInventory().setItemInOffHand(gamemodeKit.getOffHand() != null ? gamemodeKit.getOffHand() : new ItemStack(Material.AIR));
            }
        }
    }

    public void eliminatePlayer(UUID uuid) {
        Duel duel = getDuelOf(uuid);
        if (duel == null || duel.getState() != DuelState.IN_PROGRESS) return;

        DuelTeam team = duel.getTeamOf(uuid);
        if (team != null) {
            team.eliminate(uuid);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.setHealth(0.0);
        }

        checkWinConditions(duel);
    }

    public void checkWinConditions(Duel duel) {
        if (duel.getState() != DuelState.IN_PROGRESS) return;

        List<UUID> aliveA = duel.getTeam(0) != null ? duel.getTeam(0).getAliveMembers() : new ArrayList<>();
        List<UUID> aliveB = duel.getTeam(1) != null ? duel.getTeam(1).getAliveMembers() : new ArrayList<>();

        boolean aEliminated = aliveA.isEmpty();
        boolean bEliminated = aliveB.isEmpty();

        if (duel.getRounds() <= 1) {
            if (aEliminated && bEliminated) {
                endDuel(duel, null);
            } else if (aEliminated) {
                endDuel(duel, Team.TEAM_B);
            } else if (bEliminated) {
                endDuel(duel, Team.TEAM_A);
            }
            return;
        }

        if (aEliminated || bEliminated) {
            if (aEliminated && bEliminated) {
                duel.incrementScoreA();
                duel.incrementScoreB();
            } else if (aEliminated) {
                duel.incrementScoreB();
            } else {
                duel.incrementScoreA();
            }

            announceScore(duel);
            int neededWins = (duel.getRounds() / 2) + 1;
            if (duel.getScoreA() >= neededWins || duel.getScoreB() >= neededWins) {
                endDuel(duel, duel.getScoreA() >= neededWins ? Team.TEAM_A : Team.TEAM_B);
            } else {
                startNextRound(duel);
            }
        }
    }

    private void announceScore(Duel duel) {
        for (UUID uuid : duel.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                        "&7Score: &f" + duel.getScoreA() + " - " + duel.getScoreB()
                                + " &7| Round &f" + duel.getCurrentRound() + "/" + duel.getRounds()));
            }
        }
    }

    private void startNextRound(Duel duel) {
        duel.setCurrentRound(duel.getCurrentRound() + 1);
        for (DuelTeam team : duel.getTeams()) {
            team.resetAlive();
        }

        Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
        if (arena == null) return;

        plugin.getArenaManager().regenerateArena(arena);

        Ruleset ruleset = plugin.getRulesetManager().getRuleset(duel.getRulesetId());

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            player.getInventory().clear();
            player.getEnderChest().clear();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.setFireTicks(0);
            player.setNoDamageTicks(20);
            player.setGameMode(GameMode.SURVIVAL);

            DuelTeam team = duel.getTeamOf(uuid);
            Team teamIndex = duel.getTeamIndex(uuid);
            Location spawn = arena.getSpawn(teamIndex);
            if (spawn == null) spawn = arena.getCenter();
            if (spawn != null) {
                int memberIndex = team != null ? team.getMembers().indexOf(uuid) : 0;
                player.teleport(spawn.clone().add(memberIndex * 0.75, 0, 0));
            }

            applyKit(player, duel, ruleset);
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                        "&aRound &f" + duel.getCurrentRound() + " &astarted."));
            }
        }
    }

    public void endDuel(Duel duel, Team winnerTeam) {
        duel.setState(DuelState.FINISHED);
        duel.setEndTime(System.currentTimeMillis());

        clearTeamNametags(duel);

        Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
        if (arena != null) {
            arena.setInUse(false);
            arena.setCurrentDuelId(null);
        }

        Map<UUID, Integer> eloChanges = new ConcurrentHashMap<>();

        String matchWinnerName = null;
        String matchLoserName = null;
        double matchWinnerHealth = 0;
        double matchLoserHealth = 0;

        DatabaseManager db = plugin.getDatabase();
        List<CompletableFuture<Void>> statsFutures = new ArrayList<>();

        String titleWinnerName = "Unknown";
        if (winnerTeam != null) {
            UUID w = findWinnerUUID(duel, winnerTeam);
            if (w != null) {
                Player wp = Bukkit.getPlayer(w);
                titleWinnerName = wp != null ? wp.getName() : (Bukkit.getOfflinePlayer(w).getName() != null ? Bukkit.getOfflinePlayer(w).getName() : "Unknown");
            }
        }

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                saveOfflineRestore(duel, uuid);
                continue;
            }

            Team playerTeamIndex = duel.getTeamIndex(uuid);
            boolean isWinner = (winnerTeam != null && playerTeamIndex == winnerTeam);
            boolean isDraw = (winnerTeam == null);

            String opponentName = getOpponentName(duel, uuid);
            double endHealth = player.getHealth();

            if (isWinner) {
                if (matchWinnerName == null) {
                    matchWinnerName = player.getName();
                    matchWinnerHealth = endHealth;
                }
                plugin.getCosmeticsManager().playVictoryAnimation(player.getLocation(),
                        plugin.getCosmeticsManager().getVictoryAnimation(uuid));
                player.sendMessage(com.updraftduels.util.ColorUtil.colorize(plugin.getMessages().get("duel.you-won")));
                player.sendTitle(com.updraftduels.util.ColorUtil.colorize("&2&lVICTORY"),
                        com.updraftduels.util.ColorUtil.colorize("&a" + titleWinnerName + " won the match"), 10, 60, 10);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else if (isDraw) {
                player.sendMessage(com.updraftduels.util.ColorUtil.colorize(plugin.getMessages().get("duel.draw")));
            } else {
                if (matchLoserName == null) {
                    matchLoserName = player.getName();
                    matchLoserHealth = endHealth;
                }
                plugin.getCosmeticsManager().playDefeatAnimation(player.getLocation());
                player.sendMessage(com.updraftduels.util.ColorUtil.colorize(plugin.getMessages().get("duel.you-lost")));
                player.sendTitle(com.updraftduels.util.ColorUtil.colorize("&c&lDEFEAT"),
                        com.updraftduels.util.ColorUtil.colorize("&7" + titleWinnerName + " won the match"), 10, 60, 10);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }

            restorePlayerState(player, duel);
            plugin.getScoreboardManager().removeScoreboard(player);

            Location lobbyLoc = plugin.getLobbyLocation();
            if (player.isDead()) {
                Location respawnTarget = lobbyLoc != null ? lobbyLoc : duel.getOriginalLocations().get(uuid);
                if (respawnTarget != null) {
                    pendingRespawnLocations.put(uuid, respawnTarget);
                    lobbyTeleportAfterDuel.add(uuid);
                } else {
                    pendingRespawnLocations.remove(uuid);
                }
                scheduleAutoRespawn(uuid);
            } else if (lobbyLoc != null) {
                player.teleport(lobbyLoc);
                lobbyTeleportAfterDuel.add(uuid);
            }

            UUID uuidFinal = uuid;
            boolean isWinnerFinal = isWinner;
            boolean isDrawFinal = isDraw;
            statsFutures.add(db.getOrCreateStats(uuid, player.getName()).thenAccept(stats -> {
                if (stats != null) {
                    int prevMatches = stats.getGamesPlayed();
                    int prevDivisionIndex = plugin.getRankManager().getDivisionIndex(prevMatches);

                    if (isWinnerFinal) {
                        stats.incrementWins();
                    } else if (!isDrawFinal) {
                        stats.incrementLosses();
                    } else {
                        stats.incrementGamesPlayed();
                    }
                    int change = 0;
                    if (duel.isRanked()) {
                        change = updateElo(stats, duel, isWinnerFinal, isDrawFinal);
                        stats.setRankTier(plugin.getRankManager().getColoredRankForElo(stats.getElo()));
                    }
                    eloChanges.put(uuidFinal, change);
                    db.saveStats(stats);

                    int newMatches = stats.getGamesPlayed();
                    int newDivisionIndex = plugin.getRankManager().getDivisionIndex(newMatches);
                    boolean divisionUp = newDivisionIndex > prevDivisionIndex;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String duration = duel.getFormattedDuration();
                        String matchScore = duel.getScoreA() + " - " + duel.getScoreB();

                        if (divisionUp) {
                            var newDivisionInfo = plugin.getRankManager().getDivision(newMatches);
                            player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                                    "&bYou advanced to the " + newDivisionInfo.getColor() + newDivisionInfo.getName() + " &bdivision"));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        }

                        var divisionInfo = plugin.getRankManager().getDivision(newMatches);
                        if (!divisionInfo.isMaxed()) {
                            String bar = com.updraftduels.util.ColorUtil.colorize(
                                    plugin.getRankManager().getProgressBar(divisionInfo.getMatchesIntoDivision(), divisionInfo.getMatchesNeeded()));
                            player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                                    "&7" + bar + " &f" + divisionInfo.getMatchesIntoDivision() + "/" + divisionInfo.getMatchesNeeded()));
                        }

                        player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                                "&7Score: &f" + matchScore + " &7| Time: &f" + duration));
                    });
                }
            }));

            frozenPlayers.remove(uuid);
            pendingCountdowns.remove(duel.getId());
            activeDuelContext.remove(uuid);
        }

        for (UUID spectatorUUID : new ArrayList<>(duel.getSpectators())) {
            Player spectator = Bukkit.getPlayer(spectatorUUID);
            if (spectator != null) {
                plugin.getSpectatorManager().stopSpectating(spectator);
                org.bukkit.Location lobby = plugin.getLobbyLocation();
                if (lobby != null) {
                    spectator.teleport(lobby);
                } else {
                    spectator.teleport(spectator.getWorld().getSpawnLocation());
                }
                spectator.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&7Spectating ended."));
            }
        }

        String resultsWinner = matchWinnerName != null ? matchWinnerName
                : (winnerTeam != null ? nameOf(findWinnerUUID(duel, winnerTeam)) : "Nobody");
        String resultsLoser = matchLoserName != null ? matchLoserName
                : (winnerTeam != null ? nameOf(findLoserUUID(duel, winnerTeam)) : "Nobody");

        for (UUID partUUID : duel.getAllParticipants()) {
            Player p = Bukkit.getPlayer(partUUID);
            if (p == null) continue;
            if (winnerTeam == null) {
                p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&7The duel ended in a draw."));
            } else {
                p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                        "&aWinner: &f" + resultsWinner + " &7(&c♥ " + formatHealth(matchWinnerHealth) + "&7)"));
                p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                        "&cLoser: &f" + resultsLoser + " &7(&c♥ " + formatHealth(matchLoserHealth) + "&7)"));
            }
        }

        activeDuels.remove(duel.getId());

        if (arena != null) {
            plugin.getArenaManager().regenerateArena(arena);
        }

        CompletableFuture.allOf(statsFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, ex) -> Bukkit.getScheduler().runTask(plugin,
                        () -> completeDuelBookkeeping(duel, winnerTeam, eloChanges)));
    }

    private void completeDuelBookkeeping(Duel duel, Team winnerTeam, Map<UUID, Integer> eloChanges) {
        if (winnerTeam == null) {
            if (duel.getTournamentId() != null) {
                plugin.getTournamentManager().onDuelEnd(duel, null);
            }
            return;
        }

        plugin.getTournamentManager().onDuelEnd(duel, findWinnerUUID(duel, winnerTeam));

        UUID loserUUID = findLoserUUID(duel, winnerTeam);
        UUID winnerUUID = findWinnerUUID(duel, winnerTeam);
        if (winnerUUID == null) return;

        Player winnerPlayer = Bukkit.getPlayer(winnerUUID);
        if (winnerPlayer != null) {
            String killEffect = plugin.getCosmeticsManager().getKillEffect(winnerUUID);
            plugin.getCosmeticsManager().playKillEffect(winnerPlayer.getLocation(), killEffect);
        }

        String winnerName = winnerUUID != null ? Bukkit.getOfflinePlayer(winnerUUID).getName() : "Unknown";
        String loserName = loserUUID != null ? Bukkit.getOfflinePlayer(loserUUID).getName() : "Unknown";
        String deathMsgType = loserUUID != null ? plugin.getCosmeticsManager().getDeathMessage(loserUUID) : "default";
        String deathTemplate = plugin.getCosmeticsManager().getDeathMessageTemplate(deathMsgType);
        if (deathTemplate != null) {
            String formatted = plugin.getCosmeticsManager().formatDeathMessage(
                    deathTemplate, winnerName, loserName, duel.getDeathCause() != null ? duel.getDeathCause() : "combat");
            if (formatted != null) {
                String colored = com.updraftduels.util.ColorUtil.colorize(formatted);
                for (UUID partUUID : duel.getAllParticipants()) {
                    Player p = Bukkit.getPlayer(partUUID);
                    if (p != null) p.sendMessage(colored);
                }
            }
        }

        plugin.getSeasonManager().recordActivity(winnerUUID);
        if (loserUUID != null) plugin.getSeasonManager().recordActivity(loserUUID);

        String deathCause = duel.getDeathCause() != null ? duel.getDeathCause() : "combat";
        int winnerHP = winnerPlayer != null ? (int) winnerPlayer.getHealth() : 20;

        int winnerEloChange = eloChanges.getOrDefault(winnerUUID, 0);
        int loserEloChange = eloChanges.getOrDefault(loserUUID, 0);
        plugin.getHistoryManager().recordDuel(new DuelHistoryEntry(
                duel.getId(), duel.getArenaName(), duel.getRulesetId(),
                winnerUUID, loserUUID,
                winnerName != null ? winnerName : "Unknown",
                loserName != null ? loserName : "Unknown",
                winnerEloChange, loserEloChange, duel.getDurationMillis(),
                winnerHP, deathCause, 0));
    }

    private void scheduleAutoRespawn(UUID uuid) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isDead()) return;
            Location target = pendingRespawnLocations.get(uuid);
            if (target != null) {
                Location originalRespawn = player.getRespawnLocation();
                player.setRespawnLocation(target);
                player.spigot().respawn();
                if (originalRespawn != null) {
                    player.setRespawnLocation(originalRespawn);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && !p.isDead()) {
                        p.teleport(target);
                        lobbyTeleportAfterDuel.remove(uuid);
                    }
                });
            } else {
                player.spigot().respawn();
            }
        }, 2L);
    }

    private String formatHealth(double health) {
        health = Math.max(0, Math.min(health, 20.0));
        if (health % 1.0 == 0) return String.valueOf((int) health);
        return String.format("%.1f", health);
    }

    private String nameOf(UUID uuid) {
        if (uuid == null) return "Unknown";
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    private UUID findWinnerUUID(Duel duel, Team winnerTeam) {
        DuelTeam winTeam = null;
        for (DuelTeam team : duel.getTeams()) {
            if (team.getTeam() == winnerTeam) {
                winTeam = team;
                break;
            }
        }
        if (winTeam == null && !duel.getTeams().isEmpty()) {
            winTeam = duel.getTeams().get(0);
        }
        if (winTeam == null || winTeam.getMembers().isEmpty()) return null;
        return winTeam.getMembers().get(0);
    }

    private UUID findLoserUUID(Duel duel, Team winnerTeam) {
        for (DuelTeam team : duel.getTeams()) {
            if (team.getTeam() != winnerTeam) {
                if (!team.getMembers().isEmpty()) return team.getMembers().get(0);
            }
        }
        return null;
    }

    private int updateElo(DuelPlayerStats stats, Duel duel, boolean isWinner, boolean isDraw) {
        if (isDraw) return 0;

        List<Integer> opponentElos = new ArrayList<>();
        DuelTeam playerTeam = duel.getTeamOf(stats.getUuid());
        for (DuelTeam team : duel.getTeams()) {
            if (team == playerTeam) continue;
            for (UUID uuid : team.getMembers()) {
                DuelPlayerStats opponentStats = plugin.getDatabase().getCachedStats(uuid);
                opponentElos.add(opponentStats != null
                        ? opponentStats.getElo()
                        : plugin.getConfig().getInt("general.default-elo", 1000));
            }
        }

        if (opponentElos.isEmpty()) return 0;

        int kFactor = 32;
        double avgOpponentElo = opponentElos.stream().mapToInt(Integer::intValue).average().orElse(stats.getElo());
        double expectedA = 1.0 / (1.0 + Math.pow(10, (avgOpponentElo - stats.getElo()) / 400.0));
        double score = isWinner ? 1.0 : 0.0;
        int eloChange = (int) (kFactor * (score - expectedA));
        int minElo = plugin.getConfig().getInt("general.min-elo", 0);
        stats.setElo(Math.max(minElo, stats.getElo() + eloChange));
        return eloChange;
    }

    private String getOpponentName(Duel duel, UUID uuid) {
        DuelTeam playerTeam = duel.getTeamOf(uuid);
        for (DuelTeam team : duel.getTeams()) {
            if (team == playerTeam) continue;
            for (UUID member : team.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null) return p.getName();
                return member.toString().substring(0, 8);
            }
        }
        return "Unknown";
    }

    public void restorePlayerState(Player player, Duel duel) {
        UUID uuid = player.getUniqueId();

        Location originalLoc = duel.getOriginalLocations().get(uuid);
        if (originalLoc != null && !player.isDead() && player.getHealth() > 0.0) {
            player.teleport(originalLoc);
        } else if (originalLoc != null) {
            pendingRespawnLocations.put(uuid, originalLoc);
        }

        org.bukkit.inventory.ItemStack[] originalInv = duel.getOriginalContents().get(uuid);
        if (originalInv != null) {
            player.getInventory().setContents(originalInv.clone());
        } else {
            player.getInventory().clear();
        }
        org.bukkit.inventory.ItemStack[] originalArmor = duel.getOriginalArmorContents().get(uuid);
        if (originalArmor != null) {
            player.getInventory().setArmorContents(originalArmor.clone());
        }
        org.bukkit.inventory.ItemStack originalOffHand = duel.getOriginalOffHandContents().get(uuid);
        if (originalOffHand != null) {
            player.getInventory().setItemInOffHand(originalOffHand.clone());
        }

        org.bukkit.inventory.ItemStack[] originalEnderChest = duel.getOriginalEnderChestContents().get(uuid);
        if (originalEnderChest != null) {
            player.getEnderChest().setContents(originalEnderChest.clone());
        } else {
            player.getEnderChest().clear();
        }

        Double originalHealth = duel.getOriginalHealth().get(uuid);
        if (originalHealth != null) {
            player.setHealth(Math.min(originalHealth, 20.0));
        }

        Integer originalFood = duel.getOriginalFoodLevel().get(uuid);
        if (originalFood != null) {
            player.setFoodLevel(originalFood);
        }

        org.bukkit.GameMode originalMode = duel.getOriginalGameModes().get(uuid);
        player.setGameMode(originalMode != null ? originalMode : GameMode.SURVIVAL);
        Boolean originalAllowFlight = duel.getOriginalAllowFlight().get(uuid);
        player.setAllowFlight(originalAllowFlight != null ? originalAllowFlight : false);
        Boolean originalFlying = duel.getOriginalFlying().get(uuid);
        if (originalFlying != null) {
            player.setFlying(originalFlying);
        }
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setFireTicks(0);
        player.setMaximumNoDamageTicks(20);
        player.setNoDamageTicks(20);

        String msg = plugin.getMessages().get("general.inventory-restore");
        player.sendMessage(msg);
    }

    public void onPlayerDisconnect(UUID uuid) {
        pendingDuelSelections.remove(uuid);
        pendingRespawnLocations.remove(uuid);
        lobbyTeleportAfterDuel.remove(uuid);
        denyAllIncoming(uuid);
        for (DuelRequest req : new ArrayList<>(outgoingRequests.getOrDefault(uuid, new ArrayList<>()))) {
            denyRequest(req.getRequestId(), uuid);
        }
        Duel duel = getDuelOf(uuid);
        if (duel == null) return;

        if (duel.getState() == DuelState.WAITING) {
            cancelDuel(duel);
            return;
        }

        if (duel.getState() == DuelState.COUNTDOWN) {
            cancelDuel(duel);
            return;
        }

        if (duel.getState() == DuelState.IN_PROGRESS) {
            for (DuelTeam team : duel.getTeams()) {
                if (team.getMembers().contains(uuid)) {
                    team.eliminate(uuid);
                }
            }

            for (UUID participantUUID : duel.getAllParticipants()) {
                Player participant = Bukkit.getPlayer(participantUUID);
                if (participant != null && !participantUUID.equals(uuid)) {
                    participant.sendMessage(plugin.getMessages().get("party.member-disconnected",
                            "%player%", Bukkit.getOfflinePlayer(uuid).getName()));
                }
            }

            checkWinConditions(duel);
        }

        frozenPlayers.remove(uuid);
        pendingCountdowns.remove(duel.getId());
    }

    public void cancelDuel(Duel duel) {
        duel.setState(DuelState.CANCELLED);

        plugin.getGateManager().openGate();

        clearTeamNametags(duel);

        for (UUID uuid : duel.getAllParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                restorePlayerState(player, duel);
                plugin.getScoreboardManager().removeScoreboard(player);
                player.sendTitle(ChatColor.RED + "Duel Cancelled", "", 10, 40, 10);
            } else {
                saveOfflineRestore(duel, uuid);
            }
            frozenPlayers.remove(uuid);
        }

        pendingCountdowns.remove(duel.getId());

        Arena arena = plugin.getArenaManager().getArena(duel.getArenaName());
        if (arena != null) {
            arena.setInUse(false);
            arena.setCurrentDuelId(null);
        }

        activeDuels.remove(duel.getId());
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.values().stream().anyMatch(d -> d.isParticipant(uuid) && d.getState() != DuelState.FINISHED);
    }

    private boolean isInQueue(UUID uuid) {
        return plugin.getQueueManager().isInQueue(uuid);
    }

    public Duel getDuelOf(UUID uuid) {
        return activeDuels.values().stream()
                .filter(d -> d.isParticipant(uuid) && d.getState() != DuelState.FINISHED)
                .findFirst().orElse(null);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public boolean shouldTeleportToLobby(UUID uuid) {
        return lobbyTeleportAfterDuel.remove(uuid);
    }

    public Location consumeRespawnLocation(UUID uuid) {
        return pendingRespawnLocations.remove(uuid);
    }

    public Collection<Duel> getActiveDuels() {
        return activeDuels.values();
    }

    public Map<UUID, DuelRequest> getPendingRequests() {
        return pendingRequests;
    }

    public List<DuelRequest> getIncomingRequests(UUID uuid) {
        return incomingRequests.getOrDefault(uuid, new ArrayList<>());
    }

    public List<DuelRequest> getOutgoingRequests(UUID uuid) {
        return outgoingRequests.getOrDefault(uuid, new ArrayList<>());
    }

    public String getDuelContext(UUID uuid) {
        return activeDuelContext.get(uuid);
    }

    private void saveOfflineRestore(Duel duel, UUID uuid) {
        OfflineRestoreState state = new OfflineRestoreState();
        state.location = duel.getOriginalLocations().get(uuid);
        state.contents = duel.getOriginalContents().get(uuid);
        state.armor = duel.getOriginalArmorContents().get(uuid);
        state.offHand = duel.getOriginalOffHandContents().get(uuid);
        state.enderChest = duel.getOriginalEnderChestContents().get(uuid);
        state.health = duel.getOriginalHealth().getOrDefault(uuid, 20.0);
        state.food = duel.getOriginalFoodLevel().getOrDefault(uuid, 20);
        offlineRestores.put(uuid, state);
    }

    public void restoreOfflinePlayer(Player player) {
        OfflineRestoreState state = offlineRestores.remove(player.getUniqueId());
        if (state == null) return;

        if (state.location != null) {
            player.teleport(state.location);
        }
        if (state.contents != null) {
            player.getInventory().setContents(state.contents.clone());
        }
        if (state.armor != null) {
            player.getInventory().setArmorContents(state.armor.clone());
        }
        if (state.offHand != null) {
            player.getInventory().setItemInOffHand(state.offHand.clone());
        }
        if (state.enderChest != null) {
            player.getEnderChest().setContents(state.enderChest.clone());
        }
        player.setHealth(Math.min(state.health, 20.0));
        player.setFoodLevel(state.food);
        player.setGameMode(GameMode.SURVIVAL);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                "&aYour inventory and location were restored after the duel ended while you were offline."));
    }

    private boolean duelDebug() {
        return plugin.getConfig().getBoolean("duel.debug", false);
    }

    private static class OfflineRestoreState {
        Location location;
        ItemStack[] contents;
        ItemStack[] armor;
        ItemStack offHand;
        ItemStack[] enderChest;
        double health;
        int food;
    }
}
