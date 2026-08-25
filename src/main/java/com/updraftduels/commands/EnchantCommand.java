package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EnchantCommand implements CommandExecutor, TabCompleter {
    private final UpdraftDuels plugin;

    public EnchantCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("updraftduels.enchant")) {
            player.sendMessage(plugin.getMessages().get("general.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessages().get("enchant.usage", "%usage%", "/enchant <enchantment> [level]"));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(plugin.getMessages().get("enchant.no-item"));
            return true;
        }

        Enchantment enchantment = getEnchantment(args[0]);
        if (enchantment == null) {
            player.sendMessage(plugin.getMessages().get("enchant.invalid-enchantment", "%enchantment%", args[0]));
            return true;
        }

        int level = 1;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
                if (level < 1 || level > 255) {
                    player.sendMessage(plugin.getMessages().get("enchant.invalid-level", "%level%", args[1]));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessages().get("enchant.invalid-level", "%level%", args[1]));
                return true;
            }
        }

        item.addUnsafeEnchantment(enchantment, level);
        player.sendMessage(plugin.getMessages().get("enchant.success",
                "%enchantment%", enchantment.getName(),
                "%level%", String.valueOf(level)));
        return true;
    }

    private Enchantment getEnchantment(String name) {
        String lower = name.toLowerCase().replace("_", "");
        for (Enchantment e : Enchantment.values()) {
            if (e.getName().toLowerCase().replace("_", "").equals(lower)
                    || e.getKey().getKey().toLowerCase().replace("_", "").equals(lower)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Arrays.stream(Enchantment.values())
                    .map(e -> e.getKey().getKey())
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "4", "5", "10", "127", "255");
        }
        return new ArrayList<>();
    }
}
