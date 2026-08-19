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
package com.updraftduels;

import com.updraftduels.commands.*;
import com.updraftduels.gui.GUIManager;
import com.updraftduels.listeners.AntiExploitListener;
import com.updraftduels.listeners.AntiCheatListener;
import com.updraftduels.listeners.DuelListener;
import com.updraftduels.listeners.DeathMessageListener;
import com.updraftduels.listeners.GUIListener;
import com.updraftduels.listeners.SignListener;
import com.updraftduels.manager.*;
import com.updraftduels.model.Duel;
import com.updraftduels.placeholder.DuelPlaceholderExpansion;
import com.updraftduels.storage.DatabaseManager;
import com.updraftduels.util.MessageManager;
import com.updraftduels.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UpdraftDuels extends JavaPlugin {
    private static UpdraftDuels instance;

    private DatabaseManager database;
    private DuelManager duelManager;
    private ArenaManager arenaManager;
    private KitManager kitManager;
    private RulesetManager rulesetManager;
    private PartyManager partyManager;
    private FriendManager friendManager;
    private QueueManager queueManager;
    private ScoreboardManager scoreboardManager;
    private CosmeticsManager cosmeticsManager;
    private TournamentManager tournamentManager;
    private SeasonManager seasonManager;
    private VotingManager votingManager;
    private SpectatorManager spectatorManager;
    private HistoryManager historyManager;
    private GUIManager guiManager;
    private AntiSpamManager antiSpamManager;
    private RankManager rankManager;
    private PlaytimeManager playtimeManager;
    private GateManager gateManager;
    private MessageManager messages;
    private UpdateChecker updateChecker;
    private QueueCommand queueCommand;
    private PartyCommand partyCommand;

    private Location lobbyLocation;
    private FileConfiguration messagesConfig;
    private final Map<String, FileConfiguration> extraConfigs = new HashMap<>();
    private final Map<UUID, Boolean> autoGG = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> autoRequeue = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> partyInvites = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> spectators = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> chatMentions = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> scoreboard = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> duelRequests = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("gamemodes.yml", false);
        loadExtraConfig("gamemodes.yml");
        loadMessagesConfig();
        loadLobbyLocation();

        messages = new MessageManager(this);

        database = new DatabaseManager(this);
        database.connect();

        duelManager = new DuelManager(this);
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        rulesetManager = new RulesetManager(this);
        partyManager = new PartyManager(this);
        friendManager = new FriendManager(this);
        queueManager = new QueueManager(this);
        scoreboardManager = new ScoreboardManager(this);
        cosmeticsManager = new CosmeticsManager(this);
        cosmeticsManager.startLobbyTrailTask();
        tournamentManager = new TournamentManager(this);
        seasonManager = new SeasonManager(this);
        votingManager = new VotingManager(this);
        spectatorManager = new SpectatorManager(this);
        historyManager = new HistoryManager(this);
        antiSpamManager = new AntiSpamManager(this);
        rankManager = new RankManager(this);
        playtimeManager = new PlaytimeManager(this);
        gateManager = new GateManager(this);
        guiManager = new GUIManager(this);
        updateChecker = new UpdateChecker(this);

        registerCommands();
        registerListeners();

        arenaManager.loadArenas();
        kitManager.loadKits();

        scoreboardManager.startUpdating();

        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            playtimeManager.startTracking(p);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        long decayHours = Math.max(1, getConfig().getInt("season.decay-check-hours", 24));
        long decayInterval = decayHours * 60 * 60 * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> seasonManager.runDecayCheck(), decayInterval, decayInterval);

        Bukkit.getScheduler().runTaskTimer(this, () -> duelManager.cleanupExpiredRequests(), 200L, 200L);

        if (updateChecker.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(this, () -> updateChecker.check(), 60L);
        }

        getLogger().info("UpdraftDuels has been enabled!");
    }

    @Override
    public void onDisable() {
        scoreboardManager.stopUpdating();

        playtimeManager.saveAll();

        for (Duel duel : new ArrayList<>(duelManager.getActiveDuels())) {
            duelManager.cancelDuel(duel);
        }

        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("updraftduels.cosmetics")) {
                database.saveCosmetics(player.getUniqueId(),
                        cosmeticsManager.getKillEffect(player.getUniqueId()),
                        cosmeticsManager.getVictoryAnimation(player.getUniqueId()),
                        cosmeticsManager.getTrail(player.getUniqueId()),
                        cosmeticsManager.getDeathMessage(player.getUniqueId()));
            }
        }

        cosmeticsManager.saveIfDirty();

        gateManager.shutdown();

        if (database != null) {
            database.disconnect();
        }

        getLogger().info("UpdraftDuels has been disabled.");
    }

    private void registerCommands() {
        DuelCommand duelCmd = new DuelCommand(this);
        getCommand("duel").setExecutor(duelCmd);
        getCommand("duel").setTabCompleter(duelCmd);

        ArenaCommand arenaCmd = new ArenaCommand(this);
        getCommand("duelarena").setExecutor(arenaCmd);
        getCommand("duelarena").setTabCompleter(arenaCmd);

        KitCommand kitCmd = new KitCommand(this);
        getCommand("kit").setExecutor(kitCmd);
        getCommand("kit").setTabCompleter(kitCmd);

        PublicKitCommand publicKitCmd = new PublicKitCommand(this);
        getCommand("pk").setExecutor(publicKitCmd);
        getCommand("pk").setTabCompleter(publicKitCmd);

        PartyCommand partyCmd = new PartyCommand(this);
        partyCommand = partyCmd;
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);

        FriendCommand friendCmd = new FriendCommand(this);
        getCommand("friend").setExecutor(friendCmd);
        getCommand("friend").setTabCompleter(friendCmd);

        ProfileCommand profileCmd = new ProfileCommand(this);
        getCommand("profile").setExecutor(profileCmd);
        getCommand("profile").setTabCompleter(profileCmd);

        UduelsCommand uduelsCmd = new UduelsCommand(this);
        getCommand("uduels").setExecutor(uduelsCmd);
        getCommand("uduels").setTabCompleter(uduelsCmd);

        TournamentCommand tournamentCmd = new TournamentCommand(this);
        getCommand("tournament").setExecutor(tournamentCmd);
        getCommand("tournament").setTabCompleter(tournamentCmd);

        CosmeticsCommand cosmeticsCmd = new CosmeticsCommand(this);
        getCommand("cosmetics").setExecutor(cosmeticsCmd);
        getCommand("cosmetics").setTabCompleter(cosmeticsCmd);

        SeasonCommand seasonCmd = new SeasonCommand(this);
        getCommand("season").setExecutor(seasonCmd);
        getCommand("season").setTabCompleter(seasonCmd);

        SettingsCommand settingsCmd = new SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCmd);
        getCommand("settings").setTabCompleter(settingsCmd);

        QueueCommand queueCmd = new QueueCommand(this);
        queueCommand = queueCmd;
        getCommand("queue").setExecutor(queueCmd);
        getCommand("queue").setTabCompleter(queueCmd);

        RTPQueueCommand rtpQueueCmd = new RTPQueueCommand(this);
        getCommand("rtpqueue").setExecutor(rtpQueueCmd);

        RankedCommand rankedCmd = new RankedCommand(this);
        getCommand("ranked").setExecutor(rankedCmd);

        LeaderboardCommand leaderboardCmd = new LeaderboardCommand(this);
        getCommand("leaderboard").setExecutor(leaderboardCmd);
        getCommand("leaderboard").setTabCompleter(leaderboardCmd);

        RefillCommand refillCmd = new RefillCommand(this);
        getCommand("refill").setExecutor(refillCmd);
        getCommand("refill").setTabCompleter(refillCmd);

        AnvilCommand anvilCmd = new AnvilCommand(this);
        getCommand("anvil").setExecutor(anvilCmd);
        getCommand("anvil").setTabCompleter(anvilCmd);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DuelListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new AntiExploitListener(this), this);
        getServer().getPluginManager().registerEvents(new AntiCheatListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);
        getServer().getPluginManager().registerEvents(playtimeManager, this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
    }

    private void loadMessagesConfig() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void loadExtraConfig(String name) {
        File file = new File(getDataFolder(), name);
        extraConfigs.put(name, YamlConfiguration.loadConfiguration(file));
    }

    public FileConfiguration getExtraConfig(String name) {
        return extraConfigs.get(name);
    }

    public void reloadPlugin() {
        reloadConfig();
        messages.reload();
        loadMessagesConfig();
        loadExtraConfig("gamemodes.yml");
        rankManager.loadRanks();
        arenaManager.loadArenas();
        kitManager.loadKits();
        rulesetManager.reload();
        gateManager.load();
        seasonManager.reload();
    }

    public static UpdraftDuels getInstance() { return instance; }
    public DatabaseManager getDatabase() { return database; }
    public DuelManager getDuelManager() { return duelManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public KitManager getKitManager() { return kitManager; }
    public RulesetManager getRulesetManager() { return rulesetManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public FriendManager getFriendManager() { return friendManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public QueueCommand getQueueCommand() { return queueCommand; }
    public PartyCommand getPartyCommand() { return partyCommand; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public GateManager getGateManager() { return gateManager; }
    public MessageManager getMessages() { return messages; }
    public CosmeticsManager getCosmeticsManager() { return cosmeticsManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public VotingManager getVotingManager() { return votingManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public AntiSpamManager getAntiSpamManager() { return antiSpamManager; }
    public RankManager getRankManager() { return rankManager; }
    public PlaytimeManager getPlaytimeManager() { return playtimeManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }

    public boolean isAutoGG(UUID uuid) { return autoGG.getOrDefault(uuid, false); }
    public void setAutoGG(UUID uuid, boolean enabled) { autoGG.put(uuid, enabled); }

    public boolean isAutoRequeue(UUID uuid) { return autoRequeue.getOrDefault(uuid, false); }
    public void setAutoRequeue(UUID uuid, boolean enabled) { autoRequeue.put(uuid, enabled); }

    public boolean isPartyInvites(UUID uuid) { return partyInvites.getOrDefault(uuid, true); }
    public void setPartyInvites(UUID uuid, boolean enabled) { partyInvites.put(uuid, enabled); }

    public boolean isSpectators(UUID uuid) { return spectators.getOrDefault(uuid, true); }
    public void setSpectators(UUID uuid, boolean enabled) { spectators.put(uuid, enabled); }

    public boolean isChatMentions(UUID uuid) { return chatMentions.getOrDefault(uuid, true); }
    public void setChatMentions(UUID uuid, boolean enabled) { chatMentions.put(uuid, enabled); }

    public boolean isScoreboard(UUID uuid) { return scoreboard.getOrDefault(uuid, true); }
    public void setScoreboard(UUID uuid, boolean enabled) { scoreboard.put(uuid, enabled); }

    public boolean isDuelRequests(UUID uuid) { return duelRequests.getOrDefault(uuid, true); }
    public void setDuelRequests(UUID uuid, boolean enabled) { duelRequests.put(uuid, enabled); }

    public void clearPlayerSettings(UUID uuid) {
        autoGG.remove(uuid);
        autoRequeue.remove(uuid);
        partyInvites.remove(uuid);
        spectators.remove(uuid);
        chatMentions.remove(uuid);
        scoreboard.remove(uuid);
        duelRequests.remove(uuid);
    }

    public Location getLobbyLocation() { return lobbyLocation; }

    public void requireWins(Player player, int required, Runnable onMet) {
        database.getOrCreateStats(player.getUniqueId(), player.getName()).thenAccept(stats -> {
            if (stats != null && stats.getWins() >= required) {
                Bukkit.getScheduler().runTask(this, onMet);
            } else {
                int have = stats != null ? stats.getWins() : 0;
                Bukkit.getScheduler().runTask(this, () ->
                        player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(
                                "&cYou need &f" + required + " &cwins to use this. You have &f" + have + "&c.")));
            }
        });
    }

    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
        saveLobbyLocation();
    }

    private void loadLobbyLocation() {
        if (getConfig().contains("lobby.location.x")) {
            String worldName = getConfig().getString("lobby.location.world", "world");
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = getConfig().getDouble("lobby.location.x", 0);
                double y = getConfig().getDouble("lobby.location.y", 0);
                double z = getConfig().getDouble("lobby.location.z", 0);
                float yaw = (float) getConfig().getDouble("lobby.location.yaw", 0);
                float pitch = (float) getConfig().getDouble("lobby.location.pitch", 0);
                lobbyLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    private void saveLobbyLocation() {
        if (lobbyLocation == null) return;
        getConfig().set("lobby.location.world", lobbyLocation.getWorld().getName());
        getConfig().set("lobby.location.x", lobbyLocation.getX());
        getConfig().set("lobby.location.y", lobbyLocation.getY());
        getConfig().set("lobby.location.z", lobbyLocation.getZ());
        getConfig().set("lobby.location.yaw", (double) lobbyLocation.getYaw());
        getConfig().set("lobby.location.pitch", (double) lobbyLocation.getPitch());
        saveConfig();
    }
}
