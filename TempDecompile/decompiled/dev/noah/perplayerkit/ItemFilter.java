/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Container
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.BundleMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class ItemFilter {
    public static Set<String> whitelist;
    private static ItemFilter instance;
    private Plugin plugin;
    private boolean isEnabled;

    public ItemFilter(Plugin plugin) {
        whitelist = new HashSet<String>();
        this.plugin = plugin;
        instance = this;
        this.isEnabled = plugin.getConfig().getBoolean("anti-exploit.only-allow-kitroom-items", false);
    }

    public static ItemFilter get() {
        if (instance == null) {
            throw new IllegalStateException("ItemFilter has not been initialized yet!");
        }
        return instance;
    }

    public ItemStack[] filterItemStack(ItemStack[] input) {
        ItemStack[] output;
        if (!this.isEnabled) {
            return input;
        }
        for (ItemStack item : output = (ItemStack[])input.clone()) {
            BundleMeta bundleMeta;
            List bundleItems;
            ItemMeta container;
            BlockStateMeta blockStateMeta;
            BlockState blockState;
            if (!ItemFilter.isSafe(item)) {
                item.setType(Material.AIR);
            }
            if (item == null) continue;
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta && (blockState = (blockStateMeta = (BlockStateMeta)itemMeta).getBlockState()) instanceof Container) {
                container = (Container)blockState;
                container.getInventory().setContents(this.filterItemStack(container.getInventory().getContents()));
                blockStateMeta.setBlockState((BlockState)container);
                item.setItemMeta((ItemMeta)blockStateMeta);
            }
            if (!((container = item.getItemMeta()) instanceof BundleMeta) || (bundleItems = (bundleMeta = (BundleMeta)container).getItems()).isEmpty()) continue;
            ArrayList<ItemStack> filteredItems = new ArrayList<ItemStack>();
            for (ItemStack bundleItem : bundleItems) {
                if (!ItemFilter.isSafe(bundleItem)) continue;
                filteredItems.add(bundleItem);
            }
            bundleMeta.setItems(filteredItems);
            item.setItemMeta((ItemMeta)bundleMeta);
        }
        return output;
    }

    public static boolean isSafe(ItemStack i) {
        if (i != null) {
            if (!whitelist.contains(i.getType().toString())) {
                return false;
            }
            if (i.getAmount() != -1 && i.getAmount() > i.getMaxStackSize()) {
                return false;
            }
            for (Enchantment e : i.getEnchantments().keySet()) {
                if (i.getEnchantmentLevel(e) <= e.getMaxLevel()) continue;
                return false;
            }
            if (i.hasItemMeta()) {
                ItemMeta meta = i.getItemMeta();
                if (meta != null && meta.hasAttributeModifiers()) {
                    return false;
                }
                return meta != null && meta.getItemFlags().isEmpty();
            }
        }
        return true;
    }

    public void addToWhitelist(Collection<ItemStack[]> items) {
        for (ItemStack[] itemStacks : items) {
            for (ItemStack item : itemStacks) {
                if (item == null) continue;
                whitelist.add(item.getType().toString());
            }
        }
    }

    public void clearWhitelist() {
        whitelist.clear();
    }
}
