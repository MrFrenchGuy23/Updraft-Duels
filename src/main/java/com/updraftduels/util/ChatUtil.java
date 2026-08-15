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
package com.updraftduels.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;

public final class ChatUtil {

    private ChatUtil() {}

    public static void sendClickable(Player player, String text, String command, String hoverText) {
        String prefix = MessageManager.getPrefix();
        if (prefix == null) prefix = "";
        String fullText = prefix + text;
        String[] lines = fullText.split("\\R");

        BaseComponent[] components = new BaseComponent[lines.length];
        for (int i = 0; i < lines.length; i++) {
            TextComponent line = new TextComponent(ColorUtil.colorize(lines[i]));
            if (i == lines.length - 1) {
                line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ColorUtil.colorize(hoverText))));
            }
            components[i] = line;
        }
        player.spigot().sendMessage(components);
    }

    public static void sendClickableMessage(Player player, String text, String command, String hoverText) {
        sendClickable(player, text, command, hoverText);
    }

    public static void sendPrefixed(Player player, String prefix, String message) {
        player.sendMessage(ColorUtil.colorizePrefix(prefix + message));
    }
}
