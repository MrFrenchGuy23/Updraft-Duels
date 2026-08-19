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
package com.updraftduels.manager;

import com.updraftduels.UpdraftDuels;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class RankManager {
    private final UpdraftDuels plugin;
    private final List<RankTier> ranks;

    public RankManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.ranks = new ArrayList<>();
        loadRanks();
    }

    public void loadRanks() {
        ranks.clear();
        ConfigurationSection rankSection = plugin.getConfig().getConfigurationSection("ranks");
        if (rankSection == null) return;

        List<String> order = rankSection.getStringList("order");
        String[] romanLevels = {"I", "II", "III"};

        for (String rankKey : order) {
            ConfigurationSection section = rankSection.getConfigurationSection(rankKey);
            if (section == null) continue;

            String color = section.getString("color", "&7");
            int eloIII = section.getInt("elo-iii", 0);
            int eloII = section.getInt("elo-ii", 0);
            int eloI = section.getInt("elo-i", 0);

            int[] eloThresholds = {eloIII, eloII, eloI};

            for (int level = 0; level < 3; level++) {
                String displayName = capitalize(rankKey) + " " + romanLevels[level];
                ranks.add(new RankTier(rankKey, displayName, color, eloThresholds[level], level));
            }
        }
        ranks.sort(Comparator.comparingInt(RankTier::getMinElo));
    }

    public String getRankForElo(int elo) {
        for (int i = ranks.size() - 1; i >= 0; i--) {
            if (elo >= ranks.get(i).getMinElo()) {
                return ranks.get(i).getDisplayName();
            }
        }
        if (!ranks.isEmpty()) {
            return ranks.get(0).getDisplayName();
        }
        return "Unranked";
    }

    public String getRankColorForElo(int elo) {
        for (int i = ranks.size() - 1; i >= 0; i--) {
            if (elo >= ranks.get(i).getMinElo()) {
                return ranks.get(i).getColor();
            }
        }
        if (!ranks.isEmpty()) {
            return ranks.get(0).getColor();
        }
        return "&7";
    }

    public String getColoredRankForElo(int elo) {
        return getRankColorForElo(elo) + getRankForElo(elo);
    }

    public List<RankTier> getRanks() { return ranks; }

    public DivisionInfo getDivision(int matches) {
        if (!plugin.getConfig().getBoolean("divisions.enabled", true)) {
            return new DivisionInfo("Unranked", "&7", 0, 0, 10);
        }
        int startMatches = plugin.getConfig().getInt("divisions.start-matches", 10);
        int increase = plugin.getConfig().getInt("divisions.increase-per-rank", 0);

        int totalRequired = 0;
        int prevRequired = 0;
        List<String> divisionOrder = new ArrayList<>();

        ConfigurationSection divisionsSection = plugin.getConfig().getConfigurationSection("divisions.ranks");
        if (divisionsSection != null) {
            divisionOrder = new ArrayList<>(divisionsSection.getKeys(false));
            Collections.sort(divisionOrder, Comparator.comparingInt(Integer::parseInt));
        }

        for (int i = 0; i < divisionOrder.size(); i++) {
            int neededForThisDivision = startMatches + (increase * i);
            prevRequired = totalRequired;
            totalRequired += neededForThisDivision;

            if (matches < totalRequired) {
                ConfigurationSection divisionSec = divisionsSection.getConfigurationSection(divisionOrder.get(i));
                String name = divisionSec.getString("name", "Division " + i);
                String color = divisionSec.getString("color", "&7");
                int matchesIntoDivision = matches - prevRequired;
                int matchesNeeded = neededForThisDivision;
                return new DivisionInfo(name, color, i, matchesIntoDivision, matchesNeeded);
            }
        }

        if (!divisionOrder.isEmpty()) {
            ConfigurationSection lastSec = divisionsSection.getConfigurationSection(divisionOrder.get(divisionOrder.size() - 1));
            String name = lastSec.getString("name", "Max Division");
            String color = lastSec.getString("color", "&d");
            return new DivisionInfo(name, color, divisionOrder.size(), -1, -1);
        }

        return new DivisionInfo("Unranked", "&7", 0, matches, startMatches);
    }

    public int getDivisionIndex(int matches) {
        return getDivision(matches).getIndex();
    }

    public String getProgressBar(int current, int total) {
        if (total < 0) return "&aMaxed";
        int bars = 20;
        int filled = total > 0 ? Math.min(bars, (int) ((double) current / total * bars)) : bars;
        int empty = bars - filled;
        StringBuilder bar = new StringBuilder();
        bar.append("&a");
        for (int i = 0; i < filled; i++) bar.append("\u2588");
        bar.append("&7");
        for (int i = 0; i < empty; i++) bar.append("\u2588");
        return bar.toString();
    }

    public static class DivisionInfo {
        private final String name;
        private final String color;
        private final int index;
        private final int matchesIntoDivision;
        private final int matchesNeeded;

        public DivisionInfo(String name, String color, int index, int matchesIntoDivision, int matchesNeeded) {
            this.name = name;
            this.color = color;
            this.index = index;
            this.matchesIntoDivision = matchesIntoDivision;
            this.matchesNeeded = matchesNeeded;
        }

        public String getName() { return name; }
        public String getColor() { return color; }
        public int getIndex() { return index; }
        public int getMatchesIntoDivision() { return matchesIntoDivision; }
        public int getMatchesNeeded() { return matchesNeeded; }
        public boolean isMaxed() { return matchesNeeded < 0; }
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static class RankTier {
        private final String key;
        private final String displayName;
        private final String color;
        private final int minElo;
        private final int levelIndex;

        public RankTier(String key, String displayName, String color, int minElo, int levelIndex) {
            this.key = key;
            this.displayName = displayName;
            this.color = color;
            this.minElo = minElo;
            this.levelIndex = levelIndex;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
        public int getMinElo() { return minElo; }
        public int getLevelIndex() { return levelIndex; }
    }
}
