/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import dev.noah.perplayerkit.util.SoundManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class KitManager {
    private static KitManager instance;
    private final PerPlayerKit plugin;
    private final HashMap<String, ItemStack[]> kitByKitIDMap;
    private final HashMap<UUID, Integer> lastKitUsedByPlayer;
    private final List<PublicKit> publicKitList;

    public KitManager(PerPlayerKit plugin) {
        this.plugin = plugin;
        this.lastKitUsedByPlayer = new HashMap();
        this.publicKitList = new ArrayList<PublicKit>();
        this.kitByKitIDMap = new HashMap();
        instance = this;
    }

    public static KitManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitManager not initialized");
        }
        return instance;
    }

    public ItemStack[] getItemStackArrayById(String id) {
        return this.kitByKitIDMap.get(id);
    }

    public List<PublicKit> getPublicKitList() {
        return this.publicKitList;
    }

    public int getLastKitLoaded(UUID uuid) {
        if (this.lastKitUsedByPlayer.containsKey(uuid)) {
            return this.lastKitUsedByPlayer.get(uuid);
        }
        return -1;
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit) {
        Player player;
        if (Bukkit.getPlayer((UUID)uuid) != null && (player = Bukkit.getPlayer((UUID)uuid)) != null) {
            boolean notEmpty = false;
            for (ItemStack i : kit) {
                if (i == null || notEmpty) continue;
                notEmpty = true;
            }
            if (notEmpty) {
                if (kit[36] != null && !kit[36].getType().toString().contains("BOOTS")) {
                    kit[36] = null;
                }
                if (kit[37] != null && !kit[37].getType().toString().contains("LEGGINGS")) {
                    kit[37] = null;
                }
                if (kit[38] != null && !kit[38].getType().toString().contains("CHESTPLATE") && !kit[38].getType().toString().contains("ELYTRA")) {
                    kit[38] = null;
                }
                if (kit[39] != null && !kit[39].getType().toString().contains("HELMET")) {
                    kit[39] = null;
                }
                this.kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), kit);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + slot + " saved!");
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.savePlayerKitToDB(uuid, slot));
                return true;
            }
            player.sendMessage(String.valueOf(ChatColor.RED) + "You cant save an empty kit!");
        }
        return false;
    }

    public boolean savePublicKit(Player player, String publickit, ItemStack[] kit) {
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i == null || notEmpty) continue;
            notEmpty = true;
        }
        if (notEmpty) {
            if (kit[36] != null && !kit[36].getType().toString().contains("BOOTS")) {
                kit[36] = null;
            }
            if (kit[37] != null && !kit[37].getType().toString().contains("LEGGINGS")) {
                kit[37] = null;
            }
            if (kit[38] != null && !kit[38].getType().toString().contains("CHESTPLATE") && !kit[38].getType().toString().contains("ELYTRA")) {
                kit[38] = null;
            }
            if (kit[39] != null && !kit[39].getType().toString().contains("HELMET")) {
                kit[39] = null;
            }
            this.kitByKitIDMap.put(IDUtil.getPublicKitId(publickit), kit);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Public Kit " + publickit + " saved!");
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.savePublicKitToDB(publickit));
            return true;
        }
        player.sendMessage(String.valueOf(ChatColor.RED) + "You cant save an empty kit!");
        return false;
    }

    public boolean savePublicKit(String id, ItemStack[] kit) {
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i == null || notEmpty) continue;
            notEmpty = true;
        }
        if (notEmpty) {
            if (kit[36] != null && !kit[36].getType().toString().contains("BOOTS")) {
                kit[36] = null;
            }
            if (kit[37] != null && !kit[37].getType().toString().contains("LEGGINGS")) {
                kit[37] = null;
            }
            if (kit[38] != null && !kit[38].getType().toString().contains("CHESTPLATE") && !kit[38].getType().toString().contains("ELYTRA")) {
                kit[38] = null;
            }
            if (kit[39] != null && !kit[39].getType().toString().contains("HELMET")) {
                kit[39] = null;
            }
            this.kitByKitIDMap.put(IDUtil.getPublicKitId(id), kit);
            return true;
        }
        return false;
    }

    public boolean saveEC(UUID uuid, int slot, ItemStack[] kit) {
        Player player;
        if (Bukkit.getPlayer((UUID)uuid) != null && (player = Bukkit.getPlayer((UUID)uuid)) != null) {
            boolean notEmpty = false;
            for (ItemStack i : kit) {
                if (i == null || notEmpty) continue;
                notEmpty = true;
            }
            if (notEmpty) {
                this.kitByKitIDMap.put(IDUtil.getECId(uuid, slot), kit);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Enderchest " + slot + " saved!");
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.saveEnderchestToDB(uuid, slot));
                return true;
            }
            player.sendMessage(String.valueOf(ChatColor.RED) + "You cant save an empty enderchest!");
        }
        return false;
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit, boolean silent) {
        if (silent) {
            Player player;
            if (Bukkit.getPlayer((UUID)uuid) != null && (player = Bukkit.getPlayer((UUID)uuid)) != null) {
                boolean notEmpty = false;
                for (ItemStack i : kit) {
                    if (i == null || notEmpty) continue;
                    notEmpty = true;
                }
                if (notEmpty) {
                    if (kit[36] != null && !kit[36].getType().toString().contains("BOOTS")) {
                        kit[36] = null;
                    }
                    if (kit[37] != null && !kit[37].getType().toString().contains("LEGGINGS")) {
                        kit[37] = null;
                    }
                    if (kit[38] != null && !kit[38].getType().toString().contains("CHESTPLATE") && !kit[38].getType().toString().contains("ELYTRA")) {
                        kit[38] = null;
                    }
                    if (kit[39] != null && !kit[39].getType().toString().contains("HELMET")) {
                        kit[39] = null;
                    }
                    this.kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), ItemFilter.get().filterItemStack(kit));
                    return true;
                }
                player.sendMessage(String.valueOf(ChatColor.RED) + "You cant save an empty kit!");
            }
            return false;
        }
        return this.savekit(uuid, slot, kit);
    }

    public boolean regearKit(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        if (this.kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)) == null) {
            return false;
        }
        boolean invertWhitelist = this.plugin.getConfig().getBoolean("regear.invert-whitelist", false);
        HashSet whitelist = new HashSet(this.plugin.getConfig().getStringList("regear.whitelist"));
        ItemStack[] kit = this.kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot));
        ItemStack[] playerInventory = player.getInventory().getContents();
        for (int i = 0; i < Math.min(playerInventory.length, kit.length); ++i) {
            if (kit[i] == null || (!invertWhitelist ? !whitelist.contains(kit[i].getType().toString()) : whitelist.contains(kit[i].getType().toString()))) continue;
            if (playerInventory[i] != null && !playerInventory[i].getType().isAir() && playerInventory[i].getType() != kit[i].getType()) continue;
            playerInventory[i] = kit[i];
        }
        player.getInventory().setContents(playerInventory);
        return true;
    }

    private boolean loadKitInternal(Player player, String kitId, String notFoundMessage, boolean isEnderChest, Runnable afterLoad) {
        if (player == null) {
            return false;
        }
        ItemStack[] kit = this.kitByKitIDMap.get(kitId);
        if (kit == null) {
            if (notFoundMessage != null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + notFoundMessage);
                SoundManager.playFailure(player);
            }
            return false;
        }
        if (isEnderChest) {
            player.getEnderChest().setContents(kit);
        } else {
            player.getInventory().setContents(kit);
        }
        if (afterLoad != null) {
            afterLoad.run();
        }
        SoundManager.playSuccess(player);
        this.applyKitLoadEffects(player, isEnderChest);
        return true;
    }

    public boolean loadKit(Player player, int slot) {
        return this.loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot), "Kit " + slot + " does not exist!", false, () -> {
            BroadcastManager.get().broadcastPlayerLoadedPrivateKit(player);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + slot + " loaded!");
            this.lastKitUsedByPlayer.put(player.getUniqueId(), slot);
        });
    }

    public boolean loadKitSilent(Player player, int slot) {
        return this.loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot), null, false, null);
    }

    public boolean loadPublicKit(Player player, String id) {
        return this.loadKitInternal(player, IDUtil.getPublicKitId(id), "Kit does not exist!", false, () -> {
            BroadcastManager.get().broadcastPlayerLoadedPublicKit(player);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Public Kit loaded!");
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "You can save a custom version this kit by importing into the kit editor");
        });
    }

    public boolean loadPublicKitSilent(Player player, String id) {
        return this.loadKitInternal(player, IDUtil.getPublicKitId(id), null, false, null);
    }

    public boolean loadEnderchest(Player player, int slot) {
        return this.loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot), "Enderchest " + slot + " does not exist!", true, () -> {
            BroadcastManager.get().broadcastPlayerLoadedEnderChest(player);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Enderchest " + slot + " loaded!");
        });
    }

    public boolean loadEnderchestSilent(Player player, int slot) {
        return this.loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot), null, true, null);
    }

    public boolean loadLastKit(Player player) {
        if (this.lastKitUsedByPlayer.containsKey(player.getUniqueId())) {
            return this.loadKit(player, this.lastKitUsedByPlayer.get(player.getUniqueId()));
        }
        return false;
    }

    public boolean hasKit(UUID uuid, int slot) {
        return this.kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)) != null;
    }

    public boolean hasEC(UUID uuid, int slot) {
        return this.kitByKitIDMap.get(IDUtil.getECId(uuid, slot)) != null;
    }

    public ItemStack[] getPlayerEC(UUID uuid, int slot) {
        return this.kitByKitIDMap.get(IDUtil.getECId(uuid, slot));
    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        return this.kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot));
    }

    public boolean hasPublicKit(String id) {
        return this.kitByKitIDMap.get(IDUtil.getPublicKitId(id)) != null;
    }

    public ItemStack[] getPublicKit(String id) {
        return this.kitByKitIDMap.get(IDUtil.getPublicKitId(id));
    }

    public void loadPlayerDataFromDB(UUID uuid) {
        ItemStack[] kit2;
        String data;
        int slot;
        for (slot = 1; slot < 10; ++slot) {
            data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPlayerKitId(uuid, slot));
            if (data.equalsIgnoreCase("error")) continue;
            try {
                kit2 = Serializer.itemStackArrayFromBase64(data);
                this.kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), ItemFilter.get().filterItemStack(Serializer.itemStackArrayFromBase64(data)));
                continue;
            }
            catch (IOException kit2) {
                // empty catch block
            }
        }
        for (slot = 1; slot < 10; ++slot) {
            data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getECId(uuid, slot));
            if (data.equalsIgnoreCase("error")) continue;
            try {
                kit2 = Serializer.itemStackArrayFromBase64(data);
                this.kitByKitIDMap.put(IDUtil.getECId(uuid, slot), ItemFilter.get().filterItemStack(Serializer.itemStackArrayFromBase64(data)));
                continue;
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public void savePlayerKitsToDB(UUID uuid) {
        for (int i = 1; i < 10; ++i) {
            this.saveKitToDB(IDUtil.getPlayerKitId(uuid, i), true);
            this.saveKitToDB(IDUtil.getECId(uuid, i), true);
        }
    }

    public void savePlayerKitToDB(UUID uuid, int slot) {
        this.saveKitToDB(IDUtil.getPlayerKitId(uuid, slot), false);
    }

    public void saveEnderchestToDB(UUID uuid, int slot) {
        this.saveKitToDB(IDUtil.getECId(uuid, slot), false);
    }

    public void savePublicKitToDB(String id) {
        this.saveKitToDB(IDUtil.getPublicKitId(id), false);
    }

    private void saveKitToDB(String key, boolean removeAfterSave) {
        if (this.kitByKitIDMap.get(key) != null) {
            PerPlayerKit.storageManager.saveKitDataByID(key, Serializer.itemStackArrayToBase64(ItemFilter.get().filterItemStack(this.kitByKitIDMap.get(key))));
            if (removeAfterSave) {
                this.kitByKitIDMap.remove(key);
            }
        }
    }

    public void loadPublicKitFromDB(String id) {
        String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPublicKitId(id));
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                this.kitByKitIDMap.put(IDUtil.getPublicKitId(id), ItemFilter.get().filterItemStack(kit));
            }
            catch (IOException ignored) {
                this.plugin.getLogger().info("Error loading public kit " + id);
            }
        }
    }

    public boolean deleteKit(UUID uuid, int slot) {
        if (this.hasKit(uuid, slot)) {
            this.kitByKitIDMap.remove(IDUtil.getPlayerKitId(uuid, slot));
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> PerPlayerKit.storageManager.deleteKitByID(IDUtil.getPlayerKitId(uuid, slot)));
            return true;
        }
        return false;
    }

    public boolean deleteEnderchest(UUID uuid, int slot) {
        if (this.hasEC(uuid, slot)) {
            this.kitByKitIDMap.remove(IDUtil.getECId(uuid, slot));
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> PerPlayerKit.storageManager.deleteKitByID(IDUtil.getECId(uuid, slot)));
            return true;
        }
        return false;
    }

    private void applyKitLoadEffects(Player player, boolean isEnderChest) {
        if (player.isDead()) {
            return;
        }
        if (isEnderChest) {
            if (this.plugin.getConfig().getBoolean("feature.heal-on-enderchest-load", false)) {
                player.setHealth(20.0);
            }
            if (this.plugin.getConfig().getBoolean("feature.feed-on-enderchest-load", false)) {
                player.setFoodLevel(20);
            }
            if (this.plugin.getConfig().getBoolean("feature.set-saturation-on-enderchest-load", false)) {
                player.setSaturation(20.0f);
            }
            if (this.plugin.getConfig().getBoolean("feature.remove-potion-effects-on-enderchest-load", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        } else {
            if (this.plugin.getConfig().getBoolean("feature.set-health-on-kit-load", false)) {
                player.setHealth(20.0);
            }
            if (this.plugin.getConfig().getBoolean("feature.set-hunger-on-kit-load", false)) {
                player.setFoodLevel(20);
            }
            if (this.plugin.getConfig().getBoolean("feature.set-saturation-on-kit-load", false)) {
                player.setSaturation(20.0f);
            }
            if (this.plugin.getConfig().getBoolean("feature.remove-potion-effects-on-kit-load", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        }
    }
}
