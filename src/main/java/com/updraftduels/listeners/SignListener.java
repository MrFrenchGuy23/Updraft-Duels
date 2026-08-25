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
package com.updraftduels.listeners;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Arena;
import com.updraftduels.util.ColorUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignListener implements Listener {
    private final UpdraftDuels plugin;
    private final Map<Block, String> queueSigns;

    public SignListener(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.queueSigns = new ConcurrentHashMap<>();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String[] lines = event.getLines();

        if (lines[0] != null && lines[0].equalsIgnoreCase("[duel]")) {
            if (!player.hasPermission("updraftduels.admin")) {
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.no-permission")));
                event.setCancelled(true);
                return;
            }

            String signType = lines[1] != null ? lines[1].toLowerCase() : "";
            String signData = lines[2] != null ? lines[2] : "";

            event.setLine(0, ChatColor.DARK_BLUE + "[Duel]");
            event.setLine(1, ChatColor.GRAY + formatSignType(signType));
            event.setLine(2, ChatColor.YELLOW + signData);

            Block block = event.getBlock();
            queueSigns.put(block, signType + ":" + signData);

            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("signs.created",
                    "%type%", signType)));
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material blockType = block.getType();
        if (!blockType.name().contains("SIGN")) return;

        Sign sign = (Sign) block.getState();
        String line0 = ChatColor.stripColor(sign.getLine(0));
        if (line0 == null || !line0.equals("[Duel]")) return;

        Player player = event.getPlayer();
        String line1 = ChatColor.stripColor(sign.getLine(1));
        String line2 = ChatColor.stripColor(sign.getLine(2));

        if (line1 == null) return;

        switch (line1.toLowerCase()) {
            case "rtp", "random" -> {
                if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                    player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
                    return;
                }
                if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(
                                    com.updraftduels.util.ColorUtil.colorize(
                                            plugin.getMessages().get("queue.already-in-queue"))));
                    return;
                }
                plugin.getQueueManager().joinRTPQueue(player.getUniqueId());
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                                com.updraftduels.util.ColorUtil.colorize(
                                        plugin.getMessages().get("queue.rtp-joined")
                                                + " &7| &e/leave &7to leave queue")));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.3f);
                updateSignCount(block, sign, plugin.getQueueManager().getRTPQueueSize());
            }
            case "arena" -> {
                if (line2 == null || line2.isEmpty()) return;
                if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                    player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
                    return;
                }
                if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(
                                    com.updraftduels.util.ColorUtil.colorize(
                                            plugin.getMessages().get("queue.already-in-queue"))));
                    return;
                }
                if (plugin.getQueueManager().joinQueue(player.getUniqueId(), line2)) {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(
                                    com.updraftduels.util.ColorUtil.colorize(
                                            plugin.getMessages().get("queue.joined", "%arena%", line2)
                                                    + " &7| &e/leave &7to leave queue")));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.3f);
                } else {
                    player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("queue.queue-full")));
                }
                updateSignCount(block, sign, plugin.getQueueManager().getQueueSize(line2));
            }
            case "stats" -> {
                plugin.getGuiManager().openProfileGUI(player, player);
            }
        }
    }

    private void updateSignCount(Block block, Sign sign, int count) {
        sign.setLine(3, ChatColor.GREEN + "Queued: " + ChatColor.WHITE + count);
        sign.update();
    }

    private String formatSignType(String type) {
        return switch (type) {
            case "rtp", "random" -> "RTP Queue";
            case "arena" -> "Arena Queue";
            case "stats" -> "Stats";
            default -> type;
        };
    }
}
