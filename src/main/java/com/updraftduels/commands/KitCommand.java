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
package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Kit;
import com.updraftduels.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public class KitCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public KitCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            if (label.toLowerCase().matches("^k[1-9]$")) {
                int slot = Integer.parseInt(label.substring(1));
                Kit kit = plugin.getKitManager().getKitBySlot(player.getUniqueId(), slot);
                if (kit == null) {
                    player.sendMessage(ColorUtil.colorizePrefix("&cYou don't have a kit in slot " + slot + "."));
                    return true;
                }
                player.getInventory().clear();
                player.getInventory().setContents(kit.getContentsArray());
                player.getInventory().setArmorContents(kit.getArmorContents());
                player.getInventory().setItemInOffHand(kit.getOffHand() != null ? kit.getOffHand() : new ItemStack(Material.AIR));
                player.sendMessage(ColorUtil.colorizePrefix("&aEquipped kit &f" + kit.getName() + " &a(&7slot " + slot + "&a)"));
                return true;
            }
            if (label.equalsIgnoreCase("kits")) {
                plugin.getGuiManager().openPersonalKitsGUI(player);
            } else {
                plugin.getGuiManager().openPublicKitsGUI(player);
            }
            return true;
        }

        if (args[0].toLowerCase().matches("^k[1-9]$")) {
            int slot = Integer.parseInt(args[0].substring(1));
            Kit kit = plugin.getKitManager().getKitBySlot(player.getUniqueId(), slot);
            if (kit == null) {
                player.sendMessage(ColorUtil.colorizePrefix("&cYou don't have a kit in slot " + slot + "."));
                return true;
            }
            player.getInventory().clear();
            player.getInventory().setContents(kit.getContentsArray());
            player.getInventory().setArmorContents(kit.getArmorContents());
            player.getInventory().setItemInOffHand(kit.getOffHand() != null ? kit.getOffHand() : new ItemStack(Material.AIR));
            player.sendMessage(ColorUtil.colorizePrefix("&aEquipped kit &f" + kit.getName() + " &a(&7slot " + slot + "&a)"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "room" -> plugin.getGuiManager().openKitRoomGUI(player);
            case "gui" -> plugin.getGuiManager().openKitsGUI(player);
            case "create" -> handleCreate(player, args);
            case "edit", "editor" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.kit.create")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/kit create <name> [prefix] [icon]")));
            return;
        }
        String name = ColorUtil.strip(args[1]);
        if (plugin.getKitManager().getKit(name) != null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.already-exists")));
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        String iconMaterial = (handItem != null && handItem.getType() != org.bukkit.Material.AIR)
                ? handItem.getType().name() : "CHEST";
        String prefix = args.length >= 3 ? args[2] : "";

        ItemStack[] contents = player.getInventory().getContents().clone();
        ItemStack[] armor = player.getInventory().getArmorContents().clone();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (plugin.getKitManager().createKitFromInventory(name, player.getUniqueId(), false, contents, armor, offHand)) {
            Kit kit = plugin.getKitManager().getKit(name);
            if (kit != null) {
                kit.setPrefix(prefix);
                kit.setIconMaterial(iconMaterial);
                plugin.getKitManager().saveKit(kit);
            }
            String iconMsg = plugin.getMessages().get("kit.created", "%name%", name);
            player.sendMessage(ColorUtil.colorizePrefix(iconMsg));
            if (!prefix.isEmpty()) {
                player.sendMessage(ColorUtil.colorizePrefix("&7Prefix: &f" + prefix));
            }
            player.sendMessage(ColorUtil.colorizePrefix("&7Icon: &f" + iconMaterial));
            player.sendMessage(ColorUtil.colorizePrefix("&7Your kit will appear in &f/kits&7."));
        }
    }

    private void handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/kit edit <name>")));
            return;
        }
        Kit kit = plugin.getKitManager().getKit(args[1]);
        if (kit == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.not-found", "%name%", args[1])));
            return;
        }
        if (kit.isPublic()) {
            if (!player.hasPermission("updraftduels.kit.managepublic") && !kit.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.no-access")));
                return;
            }
        } else if (!kit.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.no-access")));
            return;
        }
        plugin.getGuiManager().openKitEditorGUI(player, kit);
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/kit delete <name>")));
            return;
        }
        Kit kit = plugin.getKitManager().getKit(args[1]);
        if (kit == null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.not-found", "%name%", args[1])));
            return;
        }
        if (kit.isPublic()) {
            if (!player.hasPermission("updraftduels.kit.managepublic") && !kit.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.no-access")));
                return;
            }
        } else if (!kit.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.no-access")));
            return;
        }
        plugin.getKitManager().deleteKit(args[1]);
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.deleted", "%name%", args[1])));
    }

    private void handleList(Player player) {
        List<Kit> publicKits = plugin.getKitManager().getPublicKits();
        List<Kit> personalKits = plugin.getKitManager().getPersonalKits(player.getUniqueId());
        personalKits = personalKits.stream().sorted(Comparator.comparingInt(Kit::getSlot)).toList();
        player.sendMessage(ColorUtil.colorizePrefix("&fKits:"));
        String personalKitNames = personalKits.isEmpty() ? "None" : personalKits.stream()
                .map(k -> "&a" + k.getSlot() + "&7) &f" + k.getName())
                .collect(Collectors.joining(", "));
        String publicKitNames = publicKits.isEmpty() ? "None" : String.join(", ", publicKits.stream().map(Kit::getName).toList());
        player.sendMessage(ColorUtil.colorizePrefix("&fPersonal: &f" + personalKitNames));
        player.sendMessage(ColorUtil.colorizePrefix("&fPublic: &f" + publicKitNames));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fKit Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/k1, /k2, ... /k9 &7- Quick equip kit by slot"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit &7- Open the public kits GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kits &7- Open your personal kits GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit gui &7- Open the kits menu"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit room &7- Open the kit room"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit create <name> &7- Save current inventory as kit"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit edit <name> &7- Open the kit editor"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit editor <name> &7- Open the kit editor"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit delete <name> &7- Delete a kit"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk create|edit|delete <name> &7- Manage public kits"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/kit list &7- List all kits"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("room", "gui", "create", "edit", "editor", "delete", "list"));
            for (int i = 1; i <= 9; i++) completions.add("k" + i);
            return filter(completions, args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            List<String> kitNames = plugin.getKitManager().getAllVisibleKits(player.getUniqueId()).stream()
                    .map(Kit::getName).collect(Collectors.toList());
            return filter(kitNames, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
