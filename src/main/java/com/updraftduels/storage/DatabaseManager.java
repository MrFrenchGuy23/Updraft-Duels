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
package com.updraftduels.storage;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.DuelPlayerStats;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private final UpdraftDuels plugin;
    private Connection connection;
    private final ExecutorService executor;
    private final java.util.concurrent.Executor gracefulExecutor;
    private final boolean useMySQL;
    private final Map<UUID, DuelPlayerStats> statsCache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, DuelPlayerStats> eldest) {
            return size() > 256;
        }
    });

    public DatabaseManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        this.gracefulExecutor = command -> {
            if (executor.isShutdown()) {
                command.run();
            } else {
                executor.execute(command);
            }
        };
        this.useMySQL = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL");
    }

    public void connect() {
        try {
            if (useMySQL) {
                String host = plugin.getConfig().getString("database.mysql.host", "localhost");
                int port = plugin.getConfig().getInt("database.mysql.port", 3306);
                String db = plugin.getConfig().getString("database.mysql.database", "updraftduels");
                String user = plugin.getConfig().getString("database.mysql.username", "root");
                String pass = plugin.getConfig().getString("database.mysql.password", "");
                boolean ssl = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
                String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true";
                connection = DriverManager.getConnection(url, user, pass);
            } else {
                String file = plugin.getConfig().getString("database.sqlite-file", "data.db");
                String url = "jdbc:sqlite:" + new java.io.File(plugin.getDataFolder(), file).getAbsolutePath();
                connection = DriverManager.getConnection(url);
            }
            createTables();
            plugin.getLogger().info("Database connected successfully (" + (useMySQL ? "MySQL" : "SQLite") + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS duel_stats (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "wins INT DEFAULT 0, " +
                "losses INT DEFAULT 0, " +
                "kills INT DEFAULT 0, " +
                "deaths INT DEFAULT 0, " +
                "elo INT DEFAULT 1000, " +
                "win_streak INT DEFAULT 0, " +
                "best_win_streak INT DEFAULT 0, " +
                "games_played INT DEFAULT 0, " +
                "playtime BIGINT DEFAULT 0)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS kits (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(64), " +
                "owner_uuid VARCHAR(36), " +
                "is_public BOOLEAN DEFAULT FALSE, " +
                "contents TEXT, " +
                "armor TEXT, " +
                "offhand TEXT, " +
                "permission_node VARCHAR(128), " +
                "prefix VARCHAR(32) DEFAULT '', " +
                "icon VARCHAR(32) DEFAULT 'CHEST', " +
                "slot INT DEFAULT 0)");

        try { stmt.executeUpdate("ALTER TABLE kits ADD COLUMN slot INT DEFAULT 0"); } catch (SQLException ignored) {}
        try { stmt.executeUpdate("ALTER TABLE kits ADD COLUMN offhand TEXT"); } catch (SQLException ignored) {}

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS arenas (" +
                "name VARCHAR(64) PRIMARY KEY, " +
                "world VARCHAR(64), " +
                "pos1_x DOUBLE, pos1_y DOUBLE, pos1_z DOUBLE, " +
                "pos2_x DOUBLE, pos2_y DOUBLE, pos2_z DOUBLE, " +
                "spawn_a_x DOUBLE, spawn_a_y DOUBLE, spawn_a_z DOUBLE, spawn_a_yaw FLOAT, " +
                "spawn_b_x DOUBLE, spawn_b_y DOUBLE, spawn_b_z DOUBLE, spawn_b_yaw FLOAT)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS friends (" +
                "uuid VARCHAR(36), " +
                "friend_uuid VARCHAR(36), " +
                "auto_accept BOOLEAN DEFAULT FALSE, " +
                "PRIMARY KEY (uuid, friend_uuid))");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS duel_history (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "duel_id VARCHAR(36), " +
                "arena VARCHAR(64), " +
                "ruleset VARCHAR(64), " +
                "winner_uuid VARCHAR(36), " +
                "loser_uuid VARCHAR(36), " +
                "winner_name VARCHAR(16), " +
                "loser_name VARCHAR(16), " +
                "winner_elo_change INT DEFAULT 0, " +
                "loser_elo_change INT DEFAULT 0, " +
                "duration_millis BIGINT DEFAULT 0, " +
                "winner_health INT DEFAULT 20, " +
                "death_cause VARCHAR(32), " +
                "timestamp BIGINT DEFAULT 0)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS cosmetic_selections (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "kill_effect VARCHAR(32) DEFAULT 'none', " +
                "victory_animation VARCHAR(32) DEFAULT 'none', " +
                "trail VARCHAR(32) DEFAULT 'none', " +
                "death_message VARCHAR(32) DEFAULT 'default')");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS seasons (" +
                "id INT PRIMARY KEY, " +
                "start_time BIGINT, " +
                "end_time BIGINT, " +
                "active BOOLEAN DEFAULT TRUE)");

        try {
            stmt.executeUpdate("ALTER TABLE duel_stats ADD COLUMN playtime BIGINT DEFAULT 0");
        } catch (SQLException ignored) {
        }

        stmt.close();
    }

    private String insertOrReplace() {
        return useMySQL ? "REPLACE" : "INSERT OR REPLACE";
    }

    private String resolveName(UUID uuid, String dbName) {
        if (dbName != null && !dbName.isBlank()) return dbName;
        org.bukkit.OfflinePlayer offline = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        String cached = offline.getName();
        return cached != null && !cached.isBlank() ? cached : uuid.toString().substring(0, 8);
    }

    public CompletableFuture<DuelPlayerStats> loadStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM duel_stats WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    DuelPlayerStats stats = new DuelPlayerStats(uuid, resolveName(uuid, rs.getString("name")));
                    stats.setWins(rs.getInt("wins"));
                    stats.setLosses(rs.getInt("losses"));
                    stats.setKills(rs.getInt("kills"));
                    stats.setDeaths(rs.getInt("deaths"));
                    stats.setElo(rs.getInt("elo"));
                    stats.setWinStreak(rs.getInt("win_streak"));
                    stats.setBestWinStreak(rs.getInt("best_win_streak"));
                    stats.setGamesPlayed(rs.getInt("games_played"));
                    stats.setPlaytime(rs.getLong("playtime"));
                    stats.updateRankTier();
                    rs.close();
                    ps.close();
                    return stats;
                }
                rs.close();
                ps.close();
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<DuelPlayerStats> getOrCreateStats(UUID uuid, String name) {
        DuelPlayerStats cached = statsCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return loadStats(uuid).thenCompose(stats -> {
            if (stats == null) {
                DuelPlayerStats newStats = new DuelPlayerStats(uuid, name);
                newStats.setElo(plugin.getConfig().getInt("general.default-elo", 1000));
                newStats.updateRankTier();
                return saveStats(newStats).thenApply(v -> {
                    statsCache.put(uuid, newStats);
                    return newStats;
                });
            }
            if (name != null && !name.equals(stats.getName())) {
                stats.setName(name);
            }
            statsCache.put(uuid, stats);
            return CompletableFuture.completedFuture(stats);
        });
    }

    public DuelPlayerStats getCachedStats(UUID uuid) {
        return statsCache.get(uuid);
    }

    public CompletableFuture<Void> saveStats(DuelPlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO duel_stats (uuid, name, wins, losses, kills, deaths, elo, win_streak, best_win_streak, games_played, playtime) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, stats.getUuid().toString());
                ps.setString(2, stats.getName());
                ps.setInt(3, stats.getWins());
                ps.setInt(4, stats.getLosses());
                ps.setInt(5, stats.getKills());
                ps.setInt(6, stats.getDeaths());
                ps.setInt(7, stats.getElo());
                ps.setInt(8, stats.getWinStreak());
                ps.setInt(9, stats.getBestWinStreak());
                ps.setInt(10, stats.getGamesPlayed());
                ps.setLong(11, stats.getPlaytime());
                ps.executeUpdate();
                ps.close();
                statsCache.put(stats.getUuid(), stats);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<List<DuelPlayerStats>> getTopPlayers(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<DuelPlayerStats> top = new ArrayList<>();
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM duel_stats ORDER BY elo DESC LIMIT ?");
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        DuelPlayerStats stats = new DuelPlayerStats(uuid, resolveName(uuid, rs.getString("name")));
                        stats.setWins(rs.getInt("wins"));
                        stats.setLosses(rs.getInt("losses"));
                        stats.setKills(rs.getInt("kills"));
                        stats.setDeaths(rs.getInt("deaths"));
                        stats.setElo(rs.getInt("elo"));
                        stats.setWinStreak(rs.getInt("win_streak"));
                        stats.setBestWinStreak(rs.getInt("best_win_streak"));
                        stats.setGamesPlayed(rs.getInt("games_played"));
                        stats.setPlaytime(rs.getLong("playtime"));
                        stats.updateRankTier();
                        top.add(stats);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return top;
        }, gracefulExecutor);
    }

    public CompletableFuture<List<DuelPlayerStats>> getTopPlayersByStat(String statColumn, int limit) {
        List<String> validColumns = List.of("kills", "deaths", "playtime", "elo", "wins", "losses", "games_played", "win_streak", "best_win_streak");
        if (!validColumns.contains(statColumn)) {
            return CompletableFuture.completedFuture(List.of());
        }
        String column = statColumn;
        return CompletableFuture.supplyAsync(() -> {
            List<DuelPlayerStats> top = new ArrayList<>();
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM duel_stats ORDER BY " + column + " DESC LIMIT ?");
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        DuelPlayerStats stats = new DuelPlayerStats(uuid, resolveName(uuid, rs.getString("name")));
                        stats.setWins(rs.getInt("wins"));
                        stats.setLosses(rs.getInt("losses"));
                        stats.setKills(rs.getInt("kills"));
                        stats.setDeaths(rs.getInt("deaths"));
                        stats.setElo(rs.getInt("elo"));
                        stats.setWinStreak(rs.getInt("win_streak"));
                        stats.setBestWinStreak(rs.getInt("best_win_streak"));
                        stats.setGamesPlayed(rs.getInt("games_played"));
                        stats.setPlaytime(rs.getLong("playtime"));
                        stats.updateRankTier();
                        top.add(stats);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return top;
        }, gracefulExecutor);
    }

    public CompletableFuture<Integer> getPlayerRank(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT(*) + 1 as rank FROM duel_stats WHERE elo > " +
                                "COALESCE((SELECT elo FROM duel_stats WHERE uuid = ?), 1000)");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                int rank = rs.next() ? rs.getInt("rank") : 0;
                rs.close();
                ps.close();
                return rank;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> saveFriend(UUID uuid, UUID friendUUID, boolean autoAccept) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO friends (uuid, friend_uuid, auto_accept) VALUES (?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, friendUUID.toString());
                ps.setBoolean(3, autoAccept);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> removeFriend(UUID uuid, UUID friendUUID) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM friends WHERE uuid = ? AND friend_uuid = ?");
                ps.setString(1, uuid.toString());
                ps.setString(2, friendUUID.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Set<UUID>> getFriends(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> friends = new HashSet<>();
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT friend_uuid FROM friends WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        friends.add(UUID.fromString(rs.getString("friend_uuid")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return friends;
        }, gracefulExecutor);
    }

    public CompletableFuture<Boolean> isFriend(UUID uuid, UUID friendUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT auto_accept FROM friends WHERE uuid = ? AND friend_uuid = ?");
                ps.setString(1, uuid.toString());
                ps.setString(2, friendUUID.toString());
                ResultSet rs = ps.executeQuery();
                boolean result = rs.next();
                rs.close();
                ps.close();
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Set<UUID>> getAutoAccepted(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> autoAccepted = new HashSet<>();
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT friend_uuid FROM friends WHERE uuid = ? AND auto_accept = 1");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        autoAccepted.add(UUID.fromString(rs.getString("friend_uuid")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return autoAccepted;
        }, gracefulExecutor);
    }

    public CompletableFuture<Boolean> getAutoAccept(UUID uuid, UUID friendUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT auto_accept FROM friends WHERE uuid = ? AND friend_uuid = ?");
                ps.setString(1, uuid.toString());
                ps.setString(2, friendUUID.toString());
                ResultSet rs = ps.executeQuery();
                boolean result = rs.next() && rs.getBoolean("auto_accept");
                rs.close();
                ps.close();
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> toggleAutoAccept(UUID uuid, UUID friendUUID) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "UPDATE friends SET auto_accept = NOT auto_accept WHERE uuid = ? AND friend_uuid = ?");
                ps.setString(1, uuid.toString());
                ps.setString(2, friendUUID.toString());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> saveKit(String id, String name, UUID ownerUUID, boolean isPublic, String contentsJson, String armorJson, String offHandJson, String permissionNode, String prefix, String iconMaterial, int slot) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO kits (id, name, owner_uuid, is_public, contents, armor, offhand, permission_node, prefix, icon, slot) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, ownerUUID.toString());
                ps.setBoolean(4, isPublic);
                ps.setString(5, contentsJson);
                ps.setString(6, armorJson);
                ps.setString(7, offHandJson);
                ps.setString(8, permissionNode);
                ps.setString(9, prefix != null ? prefix : "");
                ps.setString(10, iconMaterial != null ? iconMaterial : "CHEST");
                ps.setInt(11, slot);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> deleteKit(String id) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM kits WHERE id = ?");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<List<Map<String, Object>>> loadKits(UUID ownerUUID, boolean publicOnly) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> kits = new ArrayList<>();
            try {
                PreparedStatement ps;
                if (publicOnly) {
                    ps = connection.prepareStatement("SELECT * FROM kits WHERE is_public = TRUE");
                } else if (ownerUUID != null) {
                    ps = connection.prepareStatement("SELECT * FROM kits WHERE owner_uuid = ?");
                    ps.setString(1, ownerUUID.toString());
                } else {
                    ps = connection.prepareStatement("SELECT * FROM kits");
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", rs.getString("id"));
                    data.put("name", rs.getString("name"));
                    data.put("owner_uuid", rs.getString("owner_uuid"));
                    data.put("is_public", rs.getBoolean("is_public"));
                    data.put("contents", rs.getString("contents"));
                    data.put("armor", rs.getString("armor"));
                    data.put("offhand", rs.getString("offhand"));
                    data.put("permission_node", rs.getString("permission_node"));
                    data.put("prefix", rs.getString("prefix"));
                    data.put("icon", rs.getString("icon"));
                    data.put("slot", rs.getInt("slot"));
                    kits.add(data);
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return kits;
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> saveArena(String name, String world, double p1x, double p1y, double p1z, double p2x, double p2y, double p2z,
                                             double sAx, double sAy, double sAz, float sAyaw,
                                             double sBx, double sBy, double sBz, float sByaw) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO arenas (name, world, pos1_x, pos1_y, pos1_z, pos2_x, pos2_y, pos2_z, " +
                                "spawn_a_x, spawn_a_y, spawn_a_z, spawn_a_yaw, spawn_b_x, spawn_b_y, spawn_b_z, spawn_b_yaw) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, world);
                ps.setDouble(3, p1x); ps.setDouble(4, p1y); ps.setDouble(5, p1z);
                ps.setDouble(6, p2x); ps.setDouble(7, p2y); ps.setDouble(8, p2z);
                ps.setDouble(9, sAx); ps.setDouble(10, sAy); ps.setDouble(11, sAz); ps.setFloat(12, sAyaw);
                ps.setDouble(13, sBx); ps.setDouble(14, sBy); ps.setDouble(15, sBz); ps.setFloat(16, sByaw);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> deleteArena(String name) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM arenas WHERE name = ?");
                ps.setString(1, name);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<List<Map<String, Object>>> loadArenas() {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> arenas = new ArrayList<>();
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM arenas");
                while (rs.next()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", rs.getString("name"));
                    data.put("world", rs.getString("world"));
                    data.put("pos1_x", rs.getDouble("pos1_x"));
                    data.put("pos1_y", rs.getDouble("pos1_y"));
                    data.put("pos1_z", rs.getDouble("pos1_z"));
                    data.put("pos2_x", rs.getDouble("pos2_x"));
                    data.put("pos2_y", rs.getDouble("pos2_y"));
                    data.put("pos2_z", rs.getDouble("pos2_z"));
                    data.put("spawn_a_x", rs.getDouble("spawn_a_x"));
                    data.put("spawn_a_y", rs.getDouble("spawn_a_y"));
                    data.put("spawn_a_z", rs.getDouble("spawn_a_z"));
                    data.put("spawn_a_yaw", rs.getFloat("spawn_a_yaw"));
                    data.put("spawn_b_x", rs.getDouble("spawn_b_x"));
                    data.put("spawn_b_y", rs.getDouble("spawn_b_y"));
                    data.put("spawn_b_z", rs.getDouble("spawn_b_z"));
                    data.put("spawn_b_yaw", rs.getFloat("spawn_b_yaw"));
                    arenas.add(data);
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return arenas;
        }, gracefulExecutor);
    }

    public Connection getConnection() {
        return connection;
    }

    public CompletableFuture<Void> saveCosmetics(UUID uuid, String killEffect, String victoryAnim, String trail, String deathMsg) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO cosmetic_selections (uuid, kill_effect, victory_animation, trail, death_message) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, uuid.toString());
                ps.setString(2, killEffect);
                ps.setString(3, victoryAnim);
                ps.setString(4, trail);
                ps.setString(5, deathMsg);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> loadCosmetics(UUID uuid, com.updraftduels.manager.CosmeticsManager cosmeticsManager) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM cosmetic_selections WHERE uuid = ?");
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    cosmeticsManager.setKillEffect(uuid, rs.getString("kill_effect"));
                    cosmeticsManager.setVictoryAnimation(uuid, rs.getString("victory_animation"));
                    cosmeticsManager.setTrail(uuid, rs.getString("trail"));
                    cosmeticsManager.setDeathMessage(uuid, rs.getString("death_message"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<Void> saveDuelHistory(String id, String duelId, String arena, String ruleset,
                                                     String winnerUUID, String loserUUID, String winnerName, String loserName,
                                                     int winnerEloChange, int loserEloChange, long duration, int winnerHealth,
                                                     String deathCause, long timestamp) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement ps = connection.prepareStatement(
                        insertOrReplace() + " INTO duel_history (id, duel_id, arena, ruleset, winner_uuid, loser_uuid, " +
                                "winner_name, loser_name, winner_elo_change, loser_elo_change, duration_millis, " +
                                "winner_health, death_cause, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, duelId);
                ps.setString(3, arena);
                ps.setString(4, ruleset);
                ps.setString(5, winnerUUID);
                ps.setString(6, loserUUID);
                ps.setString(7, winnerName);
                ps.setString(8, loserName);
                ps.setInt(9, winnerEloChange);
                ps.setInt(10, loserEloChange);
                ps.setLong(11, duration);
                ps.setInt(12, winnerHealth);
                ps.setString(13, deathCause);
                ps.setLong(14, timestamp);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, gracefulExecutor);
    }

    public CompletableFuture<List<com.updraftduels.model.DuelHistoryEntry>> getDuelHistory(UUID playerUUID, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<com.updraftduels.model.DuelHistoryEntry> history = new ArrayList<>();
            try {
                PreparedStatement ps = connection.prepareStatement(
                        "SELECT * FROM duel_history WHERE winner_uuid = ? OR loser_uuid = ? ORDER BY timestamp DESC LIMIT ?");
                ps.setString(1, playerUUID.toString());
                ps.setString(2, playerUUID.toString());
                ps.setInt(3, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        com.updraftduels.model.DuelHistoryEntry entry = new com.updraftduels.model.DuelHistoryEntry(
                                UUID.fromString(rs.getString("duel_id")),
                                rs.getString("arena"),
                                rs.getString("ruleset"),
                                UUID.fromString(rs.getString("winner_uuid")),
                                UUID.fromString(rs.getString("loser_uuid")),
                                rs.getString("winner_name"),
                                rs.getString("loser_name"),
                                rs.getInt("winner_elo_change"),
                                rs.getInt("loser_elo_change"),
                                rs.getLong("duration_millis"),
                                rs.getInt("winner_health"),
                                rs.getString("death_cause"),
                                0);
                        history.add(entry);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return history;
        }, gracefulExecutor);
    }
}

