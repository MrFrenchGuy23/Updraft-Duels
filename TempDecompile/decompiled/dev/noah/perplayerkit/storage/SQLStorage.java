/*
 * Decompiled with CFR 0.152.
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SQLStorage
implements StorageManager {
    private final SQLDatabase db;

    public SQLStorage(SQLDatabase db) {
        this.db = db;
    }

    private void createTable() throws SQLException {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("CREATE TABLE IF NOT EXISTS kits (KITID VARCHAR(100), KITDATA TEXT(15000), PRIMARY KEY (KITID))");){
            ps.executeUpdate();
        }
    }

    @Override
    public void init() throws StorageOperationException {
        try {
            this.createTable();
        }
        catch (SQLException e) {
            throw new StorageOperationException("Failed to initialize the database", e);
        }
    }

    @Override
    public void connect() throws StorageConnectionException {
        try {
            this.db.connect();
        }
        catch (ClassNotFoundException | SQLException e) {
            throw new StorageConnectionException("Failed to connect to the database", e);
        }
    }

    @Override
    public boolean isConnected() {
        return this.db.isConnected();
    }

    @Override
    public void close() throws StorageConnectionException {
        try {
            this.db.disconnect();
        }
        catch (SQLException e) {
            throw new StorageConnectionException("Failed to close the database connection", e);
        }
    }

    @Override
    public void keepAlive() throws StorageConnectionException {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");){
            ps.executeQuery();
        }
        catch (SQLException e) {
            throw new StorageConnectionException("Failed to keep the connection alive", e);
        }
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO kits (KITID, KITDATA) VALUES (?,?)");){
            ps.setString(1, kitID);
            ps.setString(2, data);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public String getKitDataByID(String kitID) {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT KITDATA FROM kits WHERE KITID=?");){
            ps.setString(1, kitID);
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return "Error";
                String string = rs.getString("KITDATA");
                return string;
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return "Error";
    }

    /*
     * Exception decompiling
     */
    @Override
    public boolean doesKitExistByID(String kitID) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 2 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doClass(Driver.java:84)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:78)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    public void deleteKitByID(String kitID) {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kits WHERE KITID=?");){
            ps.setString(1, kitID);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getAllKitIDs() {
        HashSet<String> kitIDs = new HashSet<String>();
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT KITID FROM kits");
             ResultSet rs = ps.executeQuery();){
            while (rs.next()) {
                kitIDs.add(rs.getString("KITID"));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return kitIDs;
    }
}
