/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDatabase {
    public boolean isConnected();

    public void connect() throws ClassNotFoundException, SQLException;

    public void disconnect() throws SQLException;

    public Connection getConnection() throws SQLException;
}
