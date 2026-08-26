/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import org.bukkit.plugin.Plugin;

public class SQLite
implements SQLDatabase {
    private final Plugin plugin;
    private final String databasePath;
    private HikariDataSource dataSource;

    public SQLite(Plugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath().replace('\\', '/') + "/database.db";
    }

    @Override
    public boolean isConnected() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }

    @Override
    public void connect() throws ClassNotFoundException, SQLException {
        if (!this.isConnected()) {
            this.plugin.getDataFolder().mkdirs();
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + this.databasePath + "?journal_mode=WAL&synchronous=NORMAL&cache_size=10000&temp_store=MEMORY&mmap_size=268435456&foreign_keys=ON&busy_timeout=30000");
            config.setDriverClassName("org.sqlite.JDBC");
            config.setPoolName("SQLite-Pool");
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(30000L);
            config.setIdleTimeout(300000L);
            config.setMaxLifetime(1800000L);
            config.setLeakDetectionThreshold(60000L);
            config.setKeepaliveTime(30000L);
            config.setInitializationFailTimeout(10000L);
            config.setValidationTimeout(5000L);
            config.setConnectionTestQuery("SELECT 1");
            config.setAutoCommit(true);
            config.setReadOnly(false);
            config.setIsolateInternalQueries(false);
            config.setRegisterMbeans(false);
            config.setAllowPoolSuspension(true);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
            config.addDataSourceProperty("useServerPrepStmts", "false");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            this.dataSource = new HikariDataSource(config);
        }
    }

    @Override
    public void disconnect() throws SQLException {
        if (this.isConnected()) {
            this.dataSource.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!this.isConnected()) {
            try {
                this.connect();
            }
            catch (ClassNotFoundException e) {
                throw new SQLException("Failed to load SQLite driver", e);
            }
        }
        return this.dataSource.getConnection();
    }
}
