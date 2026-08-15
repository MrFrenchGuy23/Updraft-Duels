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
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class PublicKitCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public PublicKitCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openPublicKitsGUI(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "edit" -> handleEdit(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.kit.createpublic")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/pk create <name> [prefix] [icon]")));
            return;
        }
        String name = ColorUtil.strip(args[1]);
        if (plugin.getKitManager().getKit(name) != null) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.already-exists")));
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        String iconMaterial = (handItem != null && handItem.getType() != Material.AIR)
                ? handItem.getType().name() : "CHEST";
        String prefix = args.length >= 3 ? args[2] : "";

        ItemStack[] contents = player.getInventory().getContents().clone();
        ItemStack[] armor = player.getInventory().getArmorContents().clone();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (plugin.getKitManager().createKitFromInventory(name, player.getUniqueId(), true, contents, armor, offHand)) {
            Kit kit = plugin.getKitManager().getKit(name);
            if (kit != null) {
                kit.setPrefix(prefix);
                kit.setIconMaterial(iconMaterial);
                plugin.getKitManager().saveKit(kit);
            }
            player.sendMessage(ColorUtil.colorizePrefix("&aPublic kit &f" + name + " &ahas been created!"));
            if (!prefix.isEmpty()) {
                player.sendMessage(ColorUtil.colorizePrefix("&7Prefix: &f" + prefix));
            }
            player.sendMessage(ColorUtil.colorizePrefix("&7Icon: &f" + iconMaterial));
        }
    }

    private void handleEdit(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.kit.managepublic")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/pk edit <name>")));
            return;
        }
        Kit kit = plugin.getKitManager().getKit(args[1]);
        if (kit == null || !kit.isPublic()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.not-found", "%name%", args[1])));
            return;
        }
        plugin.getGuiManager().openKitEditorGUI(player, kit);
    }

    private void handleDelete(Player player, String[] args) {
        if (!player.hasPermission("updraftduels.kit.managepublic")) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.usage", "%usage%", "/pk delete <name>")));
            return;
        }
        Kit kit = plugin.getKitManager().getKit(args[1]);
        if (kit == null || !kit.isPublic()) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("kit.not-found", "%name%", args[1])));
            return;
        }
        plugin.getKitManager().deleteKit(args[1]);
        player.sendMessage(ColorUtil.colorizePrefix("&aPublic kit &f" + args[1] + " &ahas been deleted."));
    }

    private void handleList(Player player) {
        List<Kit> publicKits = plugin.getKitManager().getPublicKits();
        player.sendMessage(ColorUtil.colorizePrefix("&fPublic Kits:"));
        String names = publicKits.isEmpty() ? "None" : String.join(", ", publicKits.stream().map(Kit::getName).toList());
        player.sendMessage(ColorUtil.colorizePrefix("&f" + names));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorizePrefix("&fPublic Kit Commands:"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk &7- Open the public kits GUI"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk create <name> &7- Create a public kit"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk edit <name> &7- Open the public kit editor"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk delete <name> &7- Delete a public kit"));
        player.sendMessage(ColorUtil.colorizePrefix("&e/pk list &7- List all public kits"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            return filter(List.of("create", "edit", "delete", "list"), args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            List<String> kitNames = plugin.getKitManager().getPublicKits().stream()
                    .map(Kit::getName)
                    .collect(Collectors.toList());
            return filter(kitNames, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
