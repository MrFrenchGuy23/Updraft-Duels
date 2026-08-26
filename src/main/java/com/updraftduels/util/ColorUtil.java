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

import org.bukkit.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    public static String colorize(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            Color color = Color.decode("#" + hex);
            net.md_5.bungee.api.ChatColor bungeeColor = net.md_5.bungee.api.ChatColor.of(color);
            matcher.appendReplacement(sb, bungeeColor.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static String colorizePrefix(String text) {
        return colorize(MessageManager.getPrefix() + text);
    }

    public static String strip(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    public static String gradient(String text, Color start, Color end) {
        if (text == null || text.isEmpty()) return "";
        int length = text.length();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
            int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
            int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);
            Color c = new Color(Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)));
            net.md_5.bungee.api.ChatColor bungee = net.md_5.bungee.api.ChatColor.of(c);
            result.append(bungee).append(text.charAt(i));
        }
        return result.toString();
    }

    public static String gradient(String text, String hexStart, String hexEnd) {
        return gradient(text, Color.decode(hexStart), Color.decode(hexEnd));
    }

    public static String separator(int length) {
        StringBuilder sb = new StringBuilder();
        Color dark = Color.decode("#1a1a2e");
        Color mid = Color.decode("#16213e");
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int r = (int) (dark.getRed() + (mid.getRed() - dark.getRed()) * ratio);
            int g = (int) (dark.getGreen() + (mid.getGreen() - dark.getGreen()) * ratio);
            int b = (int) (dark.getBlue() + (mid.getBlue() - dark.getBlue()) * ratio);
            Color c = new Color(Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)), Math.min(255, Math.max(0, b)));
            net.md_5.bungee.api.ChatColor bungee = net.md_5.bungee.api.ChatColor.of(c);
            sb.append(bungee).append("-");
        }
        return sb.toString();
    }
}
