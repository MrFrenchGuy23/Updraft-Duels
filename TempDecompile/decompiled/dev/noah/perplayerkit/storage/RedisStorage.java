/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.StorageManager;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisStorage
implements StorageManager {
    private final String host;
    private final int port;
    private final String password;
    private JedisPool pool;
    private Plugin plugin;

    public RedisStorage(Plugin plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("redis.host");
        this.port = plugin.getConfig().getInt("redis.port") == 0 ? Integer.parseInt(plugin.getConfig().getString("redis.port", "6379")) : plugin.getConfig().getInt("redis.port");
        this.password = plugin.getConfig().getString("redis.password");
    }

    @Override
    public void connect() {
        if (this.pool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            this.pool = this.password == null || this.password.isEmpty() ? new JedisPool((GenericObjectPoolConfig<Jedis>)poolConfig, this.host, this.port) : new JedisPool((GenericObjectPoolConfig<Jedis>)poolConfig, this.host, this.port, 2000, this.password);
        }
    }

    @Override
    public boolean isConnected() {
        if (this.pool == null) {
            return false;
        }
        Jedis jedis = this.getConnection();
        try {
            boolean bl = "PONG".equals(jedis.ping());
            if (jedis != null) {
                jedis.close();
            }
            return bl;
        }
        catch (Throwable throwable) {
            try {
                if (jedis != null) {
                    try {
                        jedis.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public void init() {
    }

    @Override
    public void close() {
        if (this.pool != null) {
            this.pool.close();
        }
    }

    @Override
    public void keepAlive() {
        try (Jedis jedis = this.getConnection();){
            jedis.ping();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        try (Jedis jedis = this.getConnection();){
            jedis.set(kitID, data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        Jedis jedis = this.getConnection();
        try {
            String string;
            String data = jedis.get(kitID);
            String string2 = string = data == null ? "Error" : data;
            if (jedis != null) {
                jedis.close();
            }
            return string;
        }
        catch (Throwable throwable) {
            try {
                if (jedis != null) {
                    try {
                        jedis.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }

    @Override
    public boolean doesKitExistByID(String kitID) {
        Jedis jedis = this.getConnection();
        try {
            boolean bl = jedis.exists(kitID);
            if (jedis != null) {
                jedis.close();
            }
            return bl;
        }
        catch (Throwable throwable) {
            try {
                if (jedis != null) {
                    try {
                        jedis.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public void deleteKitByID(String kitID) {
        try (Jedis jedis = this.getConnection();){
            jedis.del(kitID);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Jedis getConnection() {
        if (this.pool == null) {
            throw new IllegalStateException("Redis pool is not initialized. Call connect() first.");
        }
        return this.pool.getResource();
    }

    @Override
    public Set<String> getAllKitIDs() {
        HashSet<String> kitIDs = new HashSet<String>();
        try (Jedis jedis = this.getConnection();){
            kitIDs.addAll(jedis.keys("*"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return kitIDs;
    }
}
