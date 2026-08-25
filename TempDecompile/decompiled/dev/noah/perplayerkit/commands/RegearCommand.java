/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.StyleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class RegearCommand
implements CommandExecutor,
Listener {
    public static final ItemStack REGEAR_SHULKER_ITEM = ItemUtil.createItem(Material.WHITE_SHULKER_BOX, 1, StyleManager.get().getPrimaryColor() + "Regear Shulker", "<gray>\u25cf Restocks Your Kit</gray>", "<gray>\u25cf Use </gray>" + StyleManager.get().getPrimaryColor() + "<gray>/rg to get another regear shulker</gray>");
    public static final ItemStack REGEAR_SHELL_ITEM = ItemUtil.createItem(Material.SHULKER_SHELL, 1, StyleManager.get().getPrimaryColor() + "Regear Shell", "<gray>\u25cf Restocks Your Kit</gray>", "<gray>\u25cf Click to use!</gray>");
    private final Plugin plugin;
    private final CooldownManager commandCooldownManager;
    private final CooldownManager damageCooldownManager;
    private final boolean allowRegearWhileUsingElytra;
    private final boolean preventPuttingItemsInRegearInventory;

    public RegearCommand(Plugin plugin) {
        this.plugin = plugin;
        int commandCooldownInSeconds = plugin.getConfig().getInt("regear.command-cooldown", 5);
        int damageCooldownInSeconds = plugin.getConfig().getInt("regear.damage-timer", 5);
        this.commandCooldownManager = new CooldownManager(commandCooldownInSeconds);
        this.damageCooldownManager = new CooldownManager(damageCooldownInSeconds);
        this.allowRegearWhileUsingElytra = plugin.getConfig().getBoolean("regear.allow-while-using-elytra", true);
        this.preventPuttingItemsInRegearInventory = plugin.getConfig().getBoolean("regear.prevent-putting-items-in-regear-inventory", false);
    }

    @EventHandler
    public void onPlayerTakesDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        this.damageCooldownManager.setCooldown(player);
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player player = (Player)sender;
        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }
        String effectiveMode = label.equalsIgnoreCase("rg") ? this.plugin.getConfig().getString("regear.rg-mode", "command") : (label.equalsIgnoreCase("regear") ? this.plugin.getConfig().getString("regear.regear-mode", "command") : this.plugin.getConfig().getString("regear.rg-mode", "command"));
        if (effectiveMode.equalsIgnoreCase("shulker")) {
            int slot = player.getInventory().firstEmpty();
            if (slot == -1) {
                BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>Your inventory is full, can't give you a regear shulker!"));
                return true;
            }
            player.getInventory().setItem(slot, REGEAR_SHULKER_ITEM);
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<green>Regear Shulker given!"));
            return true;
        }
        if (effectiveMode.equalsIgnoreCase("command")) {
            int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());
            if (slot == -1) {
                BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
                return true;
            }
            if (!this.allowRegearWhileUsingElytra && player.isGliding() && player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
                BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You cannot regear while using an elytra!"));
                return true;
            }
            if (this.damageCooldownManager.isOnCooldown(player)) {
                int secondsLeft = this.damageCooldownManager.getTimeLeft(player);
                BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
                return true;
            }
            if (this.commandCooldownManager.isOnCooldown(player)) {
                int secondsLeft = this.commandCooldownManager.getTimeLeft(player);
                BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You must wait " + secondsLeft + " seconds before using this command again!"));
                return true;
            }
            KitManager.get().regearKit(player, slot);
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<green>Regeared!"));
            BroadcastManager.get().broadcastPlayerRegeared(player);
            this.commandCooldownManager.setCooldown(player);
            return true;
        }
        BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>This command is not configured correctly, please contact an administrator."));
        return true;
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (!event.getItemInHand().equals((Object)REGEAR_SHULKER_ITEM)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());
        if (slot == -1) {
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
            return;
        }
        if (this.damageCooldownManager.isOnCooldown(player)) {
            int secondsLeft = this.damageCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
            return;
        }
        player.getInventory().setItem(event.getHand(), null);
        RegearInventoryHolder holder = new RegearInventoryHolder(player);
        Inventory inventory = holder.getInventory();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onShulkerShellClick(InventoryClickEvent event) {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();
        if (!(inventoryHolder instanceof RegearInventoryHolder)) {
            return;
        }
        RegearInventoryHolder holder = (RegearInventoryHolder)inventoryHolder;
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) {
            return;
        }
        if (!currentItem.equals((Object)REGEAR_SHELL_ITEM)) {
            if (this.preventPuttingItemsInRegearInventory) {
                event.setCancelled(true);
            }
            return;
        }
        Player player = holder.player();
        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());
        if (slot == -1) {
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
            return;
        }
        if (this.damageCooldownManager.isOnCooldown(player)) {
            int secondsLeft = this.damageCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
            return;
        }
        player.closeInventory();
        KitManager.get().regearKit(player, slot);
        player.updateInventory();
        BroadcastManager.get().sendComponentMessage(player, (Component)MiniMessage.miniMessage().deserialize("<green>Regeared!"));
        BroadcastManager.get().broadcastPlayerRegeared(player);
    }

    public record RegearInventoryHolder(Player player) implements InventoryHolder
    {
        @NotNull
        public Inventory getInventory() {
            Inventory inventory = Bukkit.createInventory((InventoryHolder)this, (int)27, (String)(StyleManager.get().getPrimaryColor() + "Regear Shulker"));
            inventory.setItem(13, REGEAR_SHELL_ITEM);
            return inventory;
        }
    }
}
