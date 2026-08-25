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
import com.updraftduels.util.ColorUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RTPQueueCommand implements CommandExecutor {
    private final UpdraftDuels plugin;

    public RTPQueueCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("duel.already-in-duel")));
            return true;
        }
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                            com.updraftduels.util.ColorUtil.colorize(
                                    plugin.getMessages().get("queue.already-in-queue"))));
            return true;
        }

        if (plugin.getQueueManager().joinRTPQueue(player.getUniqueId())) {
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                            com.updraftduels.util.ColorUtil.colorize(
                                    plugin.getMessages().get("queue.rtp-joined")
                                            + " &7| &e/leave &7to leave queue")));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.3f);

            net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(
                    com.updraftduels.util.ColorUtil.colorize(
                            "&b&l" + player.getName() + " &7is queueing in &b&lRTP Queue &7click to fight him!"));
            net.md_5.bungee.api.chat.ClickEvent clickEvent = new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/rtpqueue");
            net.md_5.bungee.api.chat.HoverEvent hoverEvent = new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    new net.md_5.bungee.api.chat.TextComponent(com.updraftduels.util.ColorUtil.colorize("&eClick to join the RTP Queue!")));
            msg.setClickEvent(clickEvent);
            msg.setHoverEvent(hoverEvent);

            for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                online.spigot().sendMessage(msg);
            }
        } else {
            player.sendMessage(ColorUtil.colorizePrefix("&cPlease wait a moment before joining a queue."));
        }
        return true;
    }
}
