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

public class MySQL
implements SQLDatabase {
    private Plugin plugin;
    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private boolean useSSL = false;
    private HikariDataSource dataSource;

    public MySQL(Plugin plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("mysql.host");
        this.port = plugin.getConfig().getString("mysql.port");
        this.database = plugin.getConfig().getString("mysql.dbname");
        this.username = plugin.getConfig().getString("mysql.username");
        this.password = plugin.getConfig().getString("mysql.password");
        this.useSSL = plugin.getConfig().getBoolean("mysql.useSSL", false);
    }

    @Override
    public boolean isConnected() {
        return this.dataSource != null && !this.dataSource.isClosed();
    }

    @Override
    public void connect() {
        if (!this.isConnected()) {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(this.plugin.getConfig().getInt("mysql.maximumPoolSize", 10));
            config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=" + this.useSSL);
            config.setUsername(this.username);
            config.setPassword(this.password);
            this.dataSource = new HikariDataSource(config);
        }
    }

    @Override
    public void disconnect() {
        if (this.isConnected()) {
            this.dataSource.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!this.isConnected()) {
            this.connect();
        }
        return this.dataSource.getConnection();
    }
}
