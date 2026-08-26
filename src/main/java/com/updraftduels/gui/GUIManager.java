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
package com.updraftduels.gui;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.manager.QueueManager;
import com.updraftduels.manager.RankManager;
import com.updraftduels.manager.VotingManager;
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelPlayerStats;
import com.updraftduels.model.DuelState;
import com.updraftduels.model.Kit;
import com.updraftduels.model.Party;
import com.updraftduels.model.Ruleset;
import com.updraftduels.model.Tournament;
import com.updraftduels.util.ColorUtil;
import com.updraftduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GUIManager {
    private final UpdraftDuels plugin;

    public static final String KIT_EDITOR_TITLE = "Kit Editor";
    public static final String KIT_ROOM_TITLE = "Kit Room";
    public static final String KITS_TITLE = "Kits";
    public static final String PERSONAL_KITS_TITLE = "Personal Kits";
    public static final String PUBLIC_KITS_TITLE = "Public Kits";
    public static final String COSMETICS_TITLE = "Cosmetics";
    public static final String COSMETICS_KILL_TITLE = "Kill Effect";
    public static final String COSMETICS_VICTORY_TITLE = "Victory Animation";
    public static final String COSMETICS_TRAIL_TITLE = "Trail";
    public static final String COSMETICS_DEATH_TITLE = "Death Message";
    public static final String PROFILE_TITLE = "Profile";
    public static final String SETTINGS_TITLE = "Settings";
    public static final String RULESETS_TITLE = "Rulesets";
    public static final String RULESET_DETAILS_TITLE = "Ruleset Details";
    public static final String PARTY_TITLE = "Party";
    public static final String PARTY_DUEL_TITLE = "Party Duel";
    public static final String LEADERBOARD_TITLE = "Leaderboards";
    public static final String LEADERBOARD_KILLS_TITLE = "Kills";
    public static final String LEADERBOARD_DEATHS_TITLE = "Deaths";
    public static final String LEADERBOARD_PLAYTIME_TITLE = "Playtime";
    public static final String BRACKET_TITLE = "Tournament";
    public static final String TOURNAMENT_FORMAT_TITLE = "Tournament Format";
    public static final String SPECTATOR_TITLE = "Spectate";
    public static final String DUEL_KIT_TITLE = "Select a Kit";
    public static final String DUEL_KIT_TYPE_TITLE = "Choose your gamemode type";
    public static final String DUEL_ROUNDS_TITLE = "Select Rounds";
    public static final String QUEUE_TITLE = "Queue";
    public static final String CASUAL_QUEUE_TITLE = "Casual Queue";
    public static final String COMPETITIVE_QUEUE_TITLE = "Competitive Queue";
    public static final String BOTH_QUEUE_TITLE = "Queue";
    public static final String VOTE_TITLE = "Vote for Arena";

    private static final Material[] VOTE_WOOL = {
            Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL,
            Material.PINK_WOOL, Material.PURPLE_WOOL, Material.GREEN_WOOL
    };

    public GUIManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void openKitEditorGUI(Player player, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ColorUtil.colorize(KIT_EDITOR_TITLE + " - " + kit.getName()));

        ItemStack[] contents = kit.getContentsArray();
        for (int i = 0; i < Math.min(contents.length, 41); i++) {
            gui.setItem(i, contents[i] != null ? contents[i].clone() : null);
        }

        for (int i = 41; i < 54; i++) {
            gui.setItem(i, glassPane());
        }

        gui.setItem(45, armorLabel(Material.CHAINMAIL_BOOTS, "BOOTS"));
        gui.setItem(46, armorLabel(Material.CHAINMAIL_LEGGINGS, "LEGGINGS"));
        gui.setItem(47, armorLabel(Material.CHAINMAIL_CHESTPLATE, "CHESTPLATE"));
        gui.setItem(48, armorLabel(Material.CHAINMAIL_HELMET, "HELMET"));
        gui.setItem(49, armorLabel(Material.SHIELD, "OFFHAND"));

        gui.setItem(51, new ItemBuilder(Material.CHEST)
                .name("&aIMPORT")
                .lore("&7Import from inventory")
                .build());

        gui.setItem(52, new ItemBuilder(Material.BARRIER)
                .name("&cCLEAR KIT")
                .lore("&7Shift click to clear")
                .build());

        gui.setItem(53, new ItemBuilder(Material.OAK_DOOR)
                .name("&cBACK")
                .build());

        player.openInventory(gui);
    }

    private ItemStack armorLabel(Material material, String name) {
        return new ItemBuilder(material)
                .name("&7" + name)
                .build();
    }

    public void openKitRoomGUI(Player player) {
        openKitRoomGUI(player, 0);
    }

    public void openKitRoomGUI(Player player, int page) {
        KitRoomManager kitRoomManager = plugin.getKitRoomManager();
        Inventory gui = Bukkit.createInventory(null, 54,
                ColorUtil.colorize(KIT_ROOM_TITLE));

        for (int i = 0; i < 45; i++) {
            gui.setItem(i, null);
        }

        if (kitRoomManager.getPage(page) != null) {
            ItemStack[] items = kitRoomManager.getPage(page);
            for (int i = 0; i < Math.min(items.length, 45); i++) {
                gui.setItem(i, items[i] != null ? items[i].clone() : null);
            }
        }

        gui.setItem(45, new ItemBuilder(Material.BEACON)
                .name("&bREFILL")
                .build());

        gui.setItem(46, glassPane());
        gui.setItem(47, kitRoomPageButton(0, page));
        gui.setItem(48, kitRoomPageButton(1, page));
        gui.setItem(49, kitRoomPageButton(2, page));
        gui.setItem(50, kitRoomPageButton(3, page));
        gui.setItem(51, kitRoomPageButton(4, page));

        gui.setItem(52, glassPane());

        if (player.hasPermission("updraftduels.kit.editkitroom")) {
            gui.setItem(53, new ItemBuilder(Material.BARRIER)
                    .amount(page + 1)
                    .name("&cEDIT MENU")
                    .lore("&cShift right click to save")
                    .build());
        } else {
            gui.setItem(53, new ItemBuilder(Material.OAK_DOOR)
                    .name("&cBACK")
                    .build());
        }

        player.openInventory(gui);
    }

    private ItemStack kitRoomPageButton(int pageIndex, int currentPage) {
        Material material = (pageIndex == currentPage) ? Material.LIME_DYE : Material.GRAY_DYE;
        String prefix = (pageIndex == currentPage) ? "&a" : "&7";
        return new ItemBuilder(material)
                .name(prefix + "Page " + (pageIndex + 1))
                .build();
    }

    private ItemStack glassPane() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
    }

    private ItemStack glassPaneDark() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    }

    private ItemStack glassPaneAccent() {
        return new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build();
    }

    private void fillWithGlass(Inventory gui, int startSlot) {
        for (int i = startSlot; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                gui.setItem(i, glassPane());
            }
        }
    }

    private void fillWithGlassDark(Inventory gui, int startSlot) {
        for (int i = startSlot; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                gui.setItem(i, glassPaneDark());
            }
        }
    }

    private void fillBorder(Inventory gui) {
        int size = gui.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                    gui.setItem(i, glassPaneDark());
                }
            }
        }
    }

    private ItemStack backButton() {
        return new ItemBuilder(Material.ARROW)
                .name("&7&lBack")
                .lore("", "&7Click to return")
                .build();
    }

    private ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("&c&lClose")
                .lore("", "&7Click to close")
                .build();
    }

    private String LoreSeparator() {
        return "&8&m                              ";
    }

    private String loreLine(String text) {
        return "&7" + text;
    }

    private String valueLine(String text) {
        return "&f" + text;
    }

    public List<Kit> getVisiblePublicKits(Player player) {
        return plugin.getKitManager().getPublicKits().stream()
                .filter(kit -> kit.getPermissionNode() == null
                        || player.hasPermission("updraftduels.kit.public." + kit.getName().toLowerCase()))
                .sorted(Comparator.comparing(Kit::getName))
                .toList();
    }

    public void openDuelKitGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(DUEL_KIT_TYPE_TITLE));

        gui.setItem(11, new ItemBuilder(Material.CHEST)
                .name("&f&lPublic Kits")
                .lore("", "&7Choose from public kits", "&7created by admins", "", "&8&m                              ")
                .build());

        gui.setItem(15, new ItemBuilder(Material.ENDER_CHEST)
                .name("&f&lPersonal Kits")
                .lore("", "&7Use your own saved kits", "&7for this duel", "", "&8&m                              ")
                .build());

        gui.setItem(22, closeButton());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openDuelPublicKitGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(DUEL_KIT_TITLE));

        List<Kit> kits = getVisiblePublicKits(player);
        int slot = 0;
        for (Kit kit : kits) {
            if (slot >= 45) break;
            Material material = Material.matchMaterial(kit.getIconMaterial());
            if (material == null) material = Material.CHEST;
            gui.setItem(slot++, new ItemBuilder(material)
                    .name("&f&l" + kit.getName())
                    .lore("", "&7Prefix: &f" + (kit.getPrefix().isEmpty() ? "None" : kit.getPrefix()),
                            "", "&8&m                              ")
                    .hideVanillaLore()
                    .build());
        }

        gui.setItem(49, closeButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openDuelPersonalKitGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(DUEL_KIT_TITLE));

        List<Kit> kits = plugin.getKitManager().getPersonalKits(player.getUniqueId());
        int slot = 0;
        for (Kit kit : kits) {
            if (slot >= 45) break;
            Material material = Material.matchMaterial(kit.getIconMaterial());
            if (material == null) material = Material.CHEST;
            gui.setItem(slot++, new ItemBuilder(material)
                    .name("&f&l" + kit.getName())
                    .lore("", "&7Prefix: &f" + (kit.getPrefix().isEmpty() ? "None" : kit.getPrefix()),
                            "", "&8&m                              ")
                    .hideVanillaLore()
                    .build());
        }

        gui.setItem(49, closeButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openDuelRoundsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(DUEL_ROUNDS_TITLE));

        gui.setItem(10, new ItemBuilder(Material.IRON_SWORD)
                .name("&f&l1 Round")
                .lore("", "&7A quick single round", "", "&8&m                              ")
                .build());
        gui.setItem(11, new ItemBuilder(Material.STONE_SWORD)
                .name("&f&l2 Rounds")
                .lore("", "&7Two rounds", "", "&8&m                              ")
                .build());
        gui.setItem(12, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&f&l4 Rounds")
                .lore("", "&7Four rounds", "", "&8&m                              ")
                .build());
        gui.setItem(13, new ItemBuilder(Material.NETHERITE_SWORD)
                .name("&f&l6 Rounds")
                .lore("", "&7Six rounds", "", "&8&m                              ")
                .build());
        gui.setItem(14, new ItemBuilder(Material.MACE)
                .name("&f&l10 Rounds")
                .lore("", "&7Ten rounds", "", "&8&m                              ")
                .build());
        gui.setItem(15, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&d&lCustom")
                .lore("", "&7Type a number in chat", "", "&8&m                              ")
                .build());

        gui.setItem(18, backButton());
        gui.setItem(22, closeButton());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openQueueGUI(Player player) {
        player.openInventory(buildQueueGUI(player, QueueManager.MatchmakingMode.BOTH));
    }

    public void openCasualQueueGUI(Player player) {
        player.openInventory(buildQueueGUI(player, QueueManager.MatchmakingMode.CASUAL));
    }

    public void openCompetitiveQueueGUI(Player player) {
        player.openInventory(buildQueueGUI(player, QueueManager.MatchmakingMode.COMPETITIVE));
    }

    public void openRankedQueueGUI(Player player) {
        openCompetitiveQueueGUI(player);
    }

    private Inventory buildQueueGUI(Player player, QueueManager.MatchmakingMode mode) {
        String title = switch (mode) {
            case CASUAL -> CASUAL_QUEUE_TITLE;
            case COMPETITIVE -> COMPETITIVE_QUEUE_TITLE;
            case BOTH -> BOTH_QUEUE_TITLE;
        };
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(title));

        FileConfiguration config = getGamemodesConfig();
        int slot = 0;
        if (config != null) {
            for (String gamemode : config.getKeys(false)) {
                if (slot >= 18) break;
                String kit = config.getString(gamemode + ".kit", gamemode);
                String icon = config.getString(gamemode + ".icon", "PAPER");
                Material material = Material.matchMaterial(icon);
                if (material == null) material = Material.CHEST;

                int queueSize = plugin.getQueueManager().getGamemodeQueueSize(gamemode);
                int fightingCount = plugin.getQueueManager().getGamemodeFightingCount(gamemode);
                boolean isActive = queueSize > 0 || fightingCount > 0;

                ItemBuilder builder = new ItemBuilder(material)
                        .name("&f&l" + gamemode)
                        .lore(buildQueueLore(gamemode))
                        .hideVanillaLore();

                if (isActive) {
                    builder.enchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
                    builder.amount(Math.max(1, queueSize));
                }

                gui.setItem(slot++, builder.build());
            }
        }

        gui.setItem(22, closeButton());

        fillWithGlass(gui, slot);
        return gui;
    }

    private List<String> buildQueueLore(String gamemode) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Fighting: &f" + plugin.getQueueManager().getGamemodeFightingCount(gamemode));
        lore.add("&7Queuing: &f" + plugin.getQueueManager().getGamemodeQueueSize(gamemode));
        List<com.updraftduels.model.DuelPlayerStats> top = plugin.getQueueManager().getGamemodeTopQueued(gamemode, 5);
        if (!top.isEmpty()) {
            lore.add("");
            lore.add("&8&m                              ");
            lore.add("&e&lTop Players");
            for (int i = 0; i < top.size(); i++) {
                com.updraftduels.model.DuelPlayerStats s = top.get(i);
                String rank = plugin.getRankManager().getDivisionElo(s.getElo());
                lore.add("&7" + (i + 1) + ". " + rank + " &f" + s.getName());
            }
        }
        lore.add("");
        lore.add("&8&m                              ");
        return lore;
    }

    public void openKitsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(KITS_TITLE));

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, glassPane());
        }

        for (int i = 9; i < 18; i++) {
            int kitSlot = i - 8;
            Kit kit = plugin.getKitManager().getKitBySlot(player.getUniqueId(), kitSlot);
            if (kit != null) {
                gui.setItem(i, new ItemBuilder(Material.CHEST)
                        .name("&b&lKit " + kitSlot)
                        .lore("", "&7Left click to load", "&7Right click to edit", "", "&8&m                              ")
                        .build());
            } else {
                gui.setItem(i, new ItemBuilder(Material.CHEST)
                        .name("&7&lKit " + kitSlot)
                        .lore("", "&7Click to create", "", "&8&m                              ")
                        .build());
            }
        }

        for (int i = 18; i < 27; i++) {
            gui.setItem(i, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                    .name("&b&lKit " + (i - 17))
                    .lore("", "&7Click to edit", "", "&8&m                              ")
                    .build());
        }

        for (int i = 27; i < 36; i++) {
            gui.setItem(i, new ItemBuilder(Material.BOOK)
                    .name("&b&lKit " + (i - 26))
                    .lore("", "&7Click to edit", "", "&8&m                              ")
                    .build());
        }

        gui.setItem(37, glassPane());
        gui.setItem(38, glassPane());
        gui.setItem(39, glassPane());
        gui.setItem(40, glassPane());
        gui.setItem(41, glassPane());
        gui.setItem(42, glassPane());
        gui.setItem(43, glassPane());

        gui.setItem(37, new ItemBuilder(Material.NETHER_STAR)
                .name("&b&lKIT ROOM")
                .lore("", "&7Open the kit room", "&7to manage your kits", "", "&8&m                              ")
                .build());

        gui.setItem(38, new ItemBuilder(Material.BOOKSHELF)
                .name("&e&lPREMADE KITS")
                .lore("", "&7Browse premade kits", "&7for each gamemode", "", "&8&m                              ")
                .build());

        gui.setItem(39, new ItemBuilder(Material.OAK_SIGN)
                .name("&7&lINFO")
                .lore("", "&7Left click to load", "&7Right click to edit", "&7Shift click to import", "", "&8&m                              ")
                .build());

        gui.setItem(41, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&c&lCLEAR INVENTORY")
                .lore("", "&7Shift click to clear", "", "&8&m                              ")
                .build());

        gui.setItem(42, new ItemBuilder(Material.COMPASS)
                .name("&b&lSHARE KITS")
                .lore("", "&7/kits share <slot>", "", "&8&m                              ")
                .build());

        gui.setItem(43, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("&b&lREPAIR ITEMS")
                .lore("", "&7Click to repair all", "&7items in your inventory", "", "&8&m                              ")
                .build());

        player.openInventory(gui);
    }

    public void openPersonalKitsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(PERSONAL_KITS_TITLE));

        List<Kit> kits = getPersonalKits(player);
        int slot = 0;
        for (Kit kit : kits) {
            if (slot >= 45) break;
            Material material = Material.matchMaterial(kit.getIconMaterial());
            if (material == null) material = Material.CHEST;
            gui.setItem(slot++, new ItemBuilder(material)
                    .name("&f" + kit.getName())
                    .lore("&7Slot: &f" + kit.getSlot(),
                            "&7Prefix: &f" + (kit.getPrefix().isEmpty() ? "None" : kit.getPrefix()))
                    .hideVanillaLore()
                    .build());
        }

        gui.setItem(45, backButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openPublicKitsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(PUBLIC_KITS_TITLE));

        List<Kit> kits = getPublicKitsForPlayer(player);
        int slot = 0;
        for (Kit kit : kits) {
            if (slot >= 45) break;
            Material material = Material.matchMaterial(kit.getIconMaterial());
            if (material == null) material = Material.CHEST;
            gui.setItem(slot++, new ItemBuilder(material)
                    .name("&f" + kit.getName())
                    .lore("&7Prefix: &f" + (kit.getPrefix().isEmpty() ? "None" : kit.getPrefix()))
                    .hideVanillaLore()
                    .build());
        }

        gui.setItem(45, backButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public List<Kit> getPersonalKits(Player player) {
        return plugin.getKitManager().getPersonalKits(player.getUniqueId())
                .stream()
                .sorted(Comparator.comparingInt(Kit::getSlot))
                .toList();
    }

    public List<Kit> getPublicKitsForPlayer(Player player) {
        return plugin.getKitManager().getAllVisibleKits(player.getUniqueId()).stream()
                .filter(Kit::isPublic)
                .sorted(Comparator.comparing(Kit::getName))
                .toList();
    }

    public void openProfileGUI(Player viewer, Player target) {
        plugin.getDatabase().getOrCreateStats(target.getUniqueId(), target.getName()).thenAccept(stats -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (stats == null) {
                    viewer.sendMessage(com.updraftduels.util.ColorUtil.colorize(
                            plugin.getMessages().get("general.player-not-found", "%player%", target.getName())));
                    return;
                }

                Inventory gui = Bukkit.createInventory(null, 27,
                        ColorUtil.colorize(PROFILE_TITLE + " - " + target.getName()));

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                if (head.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                    skullMeta.setOwningPlayer(target);
                    skullMeta.setDisplayName(ColorUtil.colorize("&b&l" + target.getName()));
                    skullMeta.setLore(profileHeadLore(stats));
                    head.setItemMeta(skullMeta);
                }
                gui.setItem(4, head);

                ItemStack[] statsItems = {
                        statItem(Material.EMERALD_BLOCK, "&a&lWins", stats.getWins()),
                        statItem(Material.REDSTONE_BLOCK, "&c&lLosses", stats.getLosses()),
                        statItem(Material.GOLDEN_APPLE, "&6&lWin Rate", String.format("%.1f%%", stats.getWinRate())),
                        statItem(Material.NETHER_STAR, "&b&lELO", stats.getElo()),
                        statItem(Material.DIAMOND_BLOCK, "&f&lRank", stats.getRankTier()),
                        statItem(Material.IRON_SWORD, "&e&lKills", stats.getKills()),
                        statItem(Material.FIRE_CHARGE, "&d&lWin Streak", stats.getWinStreak()),
                        statItem(Material.BLAZE_POWDER, "&c&lBest Streak", stats.getBestWinStreak()),
                        statItem(Material.BOOK, "&7&lGames Played", stats.getGamesPlayed())
                };
                for (int i = 0; i < statsItems.length; i++) {
                    gui.setItem(9 + i, statsItems[i]);
                }

                fillWithGlass(gui, 0);
                viewer.openInventory(gui);
            });
        });
    }

    private ItemStack statItem(Material material, String name, Object value) {
        return new ItemBuilder(material)
                .name(name)
                .lore("", "&7" + value, "", "&8&m                              ")
                .build();
    }

    private List<String> profileHeadLore(DuelPlayerStats stats) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(stats.getRankTier());
        lore.add("&7ELO: &f" + stats.getElo());
        lore.add(rankProgress(stats.getElo()));
        lore.add("");
        lore.add("&8&m                              ");
        lore.add("&7Wins: &f" + stats.getWins());
        lore.add("&7Losses: &c" + stats.getLosses());
        lore.add("&7Win Rate: &f" + String.format("%.1f%%", stats.getWinRate()));
        lore.add("&7Kills: &a" + stats.getKills());
        lore.add("&7Deaths: &c" + stats.getDeaths());
        lore.add("&7Win Streak: &d" + stats.getWinStreak());
        lore.add("&7Best Streak: &6" + stats.getBestWinStreak());
        lore.add("&7Games Played: &f" + stats.getGamesPlayed());
        return lore.stream().map(ColorUtil::colorize).toList();
    }

    private String rankProgress(int elo) {
        List<RankManager.RankTier> ranks = plugin.getRankManager().getRanks();
        if (ranks.isEmpty()) return "&7Progress: &f-";

        RankManager.RankTier current = ranks.get(0);
        RankManager.RankTier next = null;
        for (int i = 0; i < ranks.size(); i++) {
            if (elo >= ranks.get(i).getMinElo()) {
                current = ranks.get(i);
                next = i + 1 < ranks.size() ? ranks.get(i + 1) : null;
            }
        }
        if (next == null) return "&7Progress: &aMax Rank";

        int span = Math.max(1, next.getMinElo() - current.getMinElo());
        int progress = Math.max(0, Math.min(span, elo - current.getMinElo()));
        int bars = 20;
        int filled = (int) ((double) progress / span * bars);
        StringBuilder bar = new StringBuilder("&7Progress: &a");
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "\u2588" : "&7\u2588");
        }
        return bar.toString();
    }

    public void openSettingsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(SETTINGS_TITLE));

        QueueManager.MatchmakingMode mode = plugin.getQueueManager().getMatchmakingMode(player.getUniqueId());
        String modeName = switch (mode) {
            case CASUAL -> "&7Casual";
            case COMPETITIVE -> "&6Competitive";
            case BOTH -> "&bBoth";
        };
        gui.setItem(10, new ItemBuilder(mode == QueueManager.MatchmakingMode.COMPETITIVE ? Material.NETHERITE_INGOT : Material.GOLD_INGOT)
                .name("&6&lMatchmaking")
                .lore("", "&7Current: " + modeName, "",
                        "&7Casual: No ELO changes", "&7Competitive: ELO on the line", "&7Both: Queue accepts all modes",
                        "", "&8&m                              ")
                .build());

        boolean autoGG = plugin.isAutoGG(player.getUniqueId());
        gui.setItem(11, new ItemBuilder(autoGG ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lAuto-GG")
                .lore("", "&7Status: " + (autoGG ? "&aON" : "&cOFF"), "",
                        "&7Automatically send \"GG\"", "&7at the end of each duel.",
                        "", "&8&m                              ")
                .build());

        boolean autoRequeue = plugin.isAutoRequeue(player.getUniqueId());
        gui.setItem(12, new ItemBuilder(autoRequeue ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lAuto Requeue")
                .lore("", "&7Status: " + (autoRequeue ? "&aON" : "&cOFF"), "",
                        "&7Automatically rejoin queue", "&7after a duel ends.",
                        "", "&8&m                              ")
                .build());

        boolean partyInvites = plugin.isPartyInvites(player.getUniqueId());
        gui.setItem(13, new ItemBuilder(partyInvites ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lParty Invites")
                .lore("", "&7Status: " + (partyInvites ? "&aON" : "&cOFF"), "",
                        "&7Allow other players to send", "&7you party invites.",
                        "", "&8&m                              ")
                .build());

        boolean spectatorEnabled = plugin.isSpectators(player.getUniqueId());
        gui.setItem(14, new ItemBuilder(spectatorEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lSpectators")
                .lore("", "&7Status: " + (spectatorEnabled ? "&aON" : "&cOFF"), "",
                        "&7Allow others to spectate", "&7your duels.",
                        "", "&8&m                              ")
                .build());

        boolean sbEnabled = plugin.isScoreboard(player.getUniqueId());
        gui.setItem(15, new ItemBuilder(sbEnabled ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lScoreboard")
                .lore("", "&7Status: " + (sbEnabled ? "&aON" : "&cOFF"), "",
                        "&7Toggle the in-game", "&7scoreboard display.",
                        "", "&8&m                              ")
                .build());

        boolean mentions = plugin.isChatMentions(player.getUniqueId());
        gui.setItem(16, new ItemBuilder(mentions ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lChat Mentions")
                .lore("", "&7Status: " + (mentions ? "&aON" : "&cOFF"), "",
                        "&7Receive a sound when someone", "&7mentions you in chat.",
                        "", "&8&m                              ")
                .build());

        boolean duelReqs = plugin.isDuelRequests(player.getUniqueId());
        gui.setItem(17, new ItemBuilder(duelReqs ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&f&lDuel Requests")
                .lore("", "&7Status: " + (duelReqs ? "&aON" : "&cOFF"), "",
                        "&7Allow other players to send", "&7you duel requests.",
                        "", "&8&m                              ")
                .build());

        boolean pingLimitEnabled = plugin.getConfig().getBoolean("ping-limit.enabled", false);
        int maxPing = plugin.getConfig().getInt("ping-limit.max-ping", 200);
        gui.setItem(28, new ItemBuilder(pingLimitEnabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name("&f&lPing Limit")
                .lore("", "&7Status: " + (pingLimitEnabled ? "&aON" : "&cOFF"), "",
                        "&7Max ping: &f" + maxPing + "ms",
                        "&7Players with higher ping", "&7won't be matched.",
                        "", "&8&m                              ")
                .build());

        gui.setItem(31, new ItemBuilder(Material.COMPASS)
                .name("&f&lMax Ping: &e" + maxPing + "ms")
                .lore("", "&7Click to cycle ping limit:", "",
                        "&7100ms &7| &f200ms &7| &7300ms &7| &7500ms &7| &7Off",
                        "", "&8&m                              ")
                .build());

        gui.setItem(49, closeButton());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openRulesetsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(RULESETS_TITLE));

        String selected = plugin.getRulesetManager().getSelectedRuleset(player.getUniqueId());
        int slot = 0;
        for (Ruleset ruleset : plugin.getRulesetManager().getAllRulesets()) {
            if (slot >= 45) break;
            Material material = Material.matchMaterial(ruleset.getId().toUpperCase() + "_BANNER");
            if (material == null) material = Material.MAP;
            boolean isSelected = ruleset.getId().equals(selected);
            gui.setItem(slot++, new ItemBuilder(material)
                    .name((isSelected ? "&a" : "&f") + "&l" + ruleset.getDisplayName())
                    .lore("", "&7" + ruleset.getDescription(),
                            "",
                            isSelected ? "&a&lSELECTED" : "&7Click to select",
                            "&8Shift-click for details",
                            "", "&8&m                              ")
                    .build());
        }

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openRulesetDetailsGUI(Player player, Ruleset ruleset) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(RULESET_DETAILS_TITLE));

        gui.setItem(4, new ItemBuilder(Material.BOOK)
                .name("&f&l" + ruleset.getDisplayName())
                .lore("",
                        "&7" + ruleset.getDescription(),
                        "",
                        "&7ID: &f" + ruleset.getId(),
                        "", "&8&m                              ")
                .build());

        ItemStack[] flags = {
                flagItem(Material.SHIELD, "No Damage", ruleset.isNoDamage()),
                flagItem(Material.ARROW, "Knockback Only", ruleset.isKnockbackOnly()),
                flagItem(Material.FEATHER, "Fists Only", ruleset.isFistsOnly()),
                flagItem(Material.APPLE, "Natural Regen Disabled", ruleset.isNaturalRegenDisabled()),
                flagItem(Material.COOKED_BEEF, "Hunger Enabled", ruleset.isHungerEnabled()),
                flagItem(Material.GLASS, "Breakable Floor", ruleset.isBreakableFloor()),
                flagItem(Material.ENDER_PEARL, "Ender Pearls Allowed", ruleset.isEnderPearlsAllowed()),
                flagItem(Material.CHORUS_FRUIT, "Chorus Fruit Allowed", ruleset.isChorusFruitAllowed())
        };
        for (int i = 0; i < flags.length; i++) {
            gui.setItem(10 + i, flags[i]);
        }

        gui.setItem(18, backButton());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    private ItemStack flagItem(Material material, String label, boolean enabled) {
        return new ItemBuilder(material)
                .name((enabled ? "&a" : "&c") + "&l" + label)
                .lore("", enabled ? "&a&lEnabled" : "&c&lDisabled", "", "&8&m                              ")
                .build();
    }

    public void openPartyGUI(Player player) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(plugin.getMessages().get("party.not-in-party"));
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(PARTY_TITLE));

        String partyName = party.getName() != null ? party.getName() : "Party";
        gui.setItem(4, new ItemBuilder(Material.WHITE_BANNER)
                .name("&9&l" + partyName)
                .lore("",
                        "&7Members: &f" + party.getMembers().size(),
                        "&7Ready: &f" + party.getReadyMembers().size() + "&7/" + party.getMembers().size(),
                        "", "&8&m                              ")
                .build());

        int slot = 10;
        for (UUID memberUUID : party.getMembers()) {
            if (slot > 16) break;
            Player member = Bukkit.getPlayer(memberUUID);
            String name = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberUUID).getName();
            if (name == null) name = memberUUID.toString().substring(0, 8);
            String skullOwner = name;
            boolean ready = party.getReadyMembers().contains(memberUUID);
            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&f&l" + name)
                    .lore("",
                            party.isLeader(memberUUID) ? "&b&lLeader" : "&7Member",
                            ready ? "&a&lReady" : "&7Not ready",
                            "", "&7Click to kick",
                            "", "&8&m                              ");
            builder.meta(meta -> {
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
                    if (member != null) skull.setOwningPlayer(member);
                    else skull.setOwner(skullOwner);
                }
            });
            gui.setItem(slot++, builder.build());
        }

        int inviteSlot = 19;
        for (UUID inviteeUUID : party.getInvitees()) {
            if (inviteSlot > 25) break;
            String name = Bukkit.getOfflinePlayer(inviteeUUID).getName();
            if (name == null) name = inviteeUUID.toString().substring(0, 8);
            gui.setItem(inviteSlot++, new ItemBuilder(Material.GRAY_DYE)
                    .name("&7&l" + name)
                    .lore("", "&7Pending invite", "", "&8&m                              ")
                    .build());
        }

        boolean isLeader = party.isLeader(player.getUniqueId());
        gui.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("&c&lLeave Party")
                .lore("", "&7Click to leave", "", "&8&m                              ")
                .build());
        if (isLeader) {
            gui.setItem(50, new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("&c&lDisband Party")
                    .lore("", "&7Click to disband", "", "&8&m                              ")
                    .build());
        }
        gui.setItem(51, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("&a&lReady")
                .lore("",
                        "&7Mark yourself ready",
                        "",
                        party.getReadyMembers().contains(player.getUniqueId()) ? "&a&lYou are ready" : "&7&lNot ready",
                        "", "&8&m                              ")
                .build());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openPartyDuelGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(PARTY_DUEL_TITLE));

        Party myParty = plugin.getPartyManager().getParty(player.getUniqueId());
        int slot = 0;
        for (Party party : plugin.getPartyManager().getAllParties().values()) {
            if (slot >= 45) break;
            if (myParty != null && party.getPartyId().equals(myParty.getPartyId())) continue;

            boolean inFight = plugin.getPartyManager().isPartyInFight(party);
            String leaderName = nameOf(party.getLeaderUUID());

            gui.setItem(slot++, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&f&l" + leaderName)
                    .lore("", "&7Status: " + (inFight ? "&c&lIn Fight" : "&a&lAvailable"),
                            "&7Players: &f" + party.getSize(),
                            "", "&8&m                              ")
                    .hideVanillaLore()
                    .build());
        }

        gui.setItem(49, closeButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openLeaderboardGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(LEADERBOARD_TITLE));

        gui.setItem(10, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&f&lKills")
                .lore("", "&7Top 10 by kills", "", "&8&m                              ")
                .build());
        gui.setItem(12, new ItemBuilder(Material.SKELETON_SKULL)
                .name("&f&lDeaths")
                .lore("", "&7Top 10 by deaths", "", "&8&m                              ")
                .build());
        gui.setItem(14, new ItemBuilder(Material.CLOCK)
                .name("&f&lPlaytime")
                .lore("", "&7Top 10 by playtime", "", "&8&m                              ")
                .build());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openLeaderboardCategoryGUI(Player player, String column) {
        String title = switch (column) {
            case "deaths" -> LEADERBOARD_DEATHS_TITLE;
            case "playtime" -> LEADERBOARD_PLAYTIME_TITLE;
            default -> LEADERBOARD_KILLS_TITLE;
        };
        String columnFinal = column;

        plugin.getDatabase().getTopPlayersByStat(column, 10).thenAccept(topPlayers -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));

                gui.setItem(45, backButton());

                int slot = 0;
                int place = 1;
                for (DuelPlayerStats stats : topPlayers) {
                    if (slot >= 45) break;
                    Material medal = switch (place) {
                        case 1 -> Material.GOLD_BLOCK;
                        case 2 -> Material.IRON_BLOCK;
                        case 3 -> Material.DIAMOND_BLOCK;
                        default -> Material.IRON_NUGGET;
                    };
                    String value = switch (columnFinal) {
                        case "deaths" -> "&c" + stats.getDeaths() + " deaths";
                        case "playtime" -> "&e" + formatPlaytime(stats.getPlaytime());
                        default -> "&a" + stats.getKills() + " kills";
                    };
                    gui.setItem(slot++, new ItemBuilder(medal)
                            .name("&f#" + place + " " + stats.getName())
                            .lore("", value, "", "&8&m                              ")
                            .build());
                    place++;
                }

                fillWithGlass(gui, Math.max(topPlayers.size(), 0));
                player.openInventory(gui);
            });
        });
    }

    public void openTournamentFormatGUI(Player player, Tournament tournament) {
        plugin.getTournamentManager().setPendingFormat(player.getUniqueId(), tournament.getId());
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(TOURNAMENT_FORMAT_TITLE));

        gui.setItem(10, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&f&l1v1")
                .lore("", "&7Solo tournament", "", "&8&m                              ")
                .hideVanillaLore()
                .build());
        gui.setItem(11, new ItemBuilder(Material.SKELETON_SKULL)
                .name("&f&l2v2")
                .lore("", "&7Duos tournament", "", "&8&m                              ")
                .hideVanillaLore()
                .build());
        gui.setItem(12, new ItemBuilder(Material.ZOMBIE_HEAD)
                .name("&f&l3v3")
                .lore("", "&7Trios tournament", "", "&8&m                              ")
                .hideVanillaLore()
                .build());
        gui.setItem(13, new ItemBuilder(Material.CREEPER_HEAD)
                .name("&f&l4v4")
                .lore("", "&7Squads tournament", "", "&8&m                              ")
                .hideVanillaLore()
                .build());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openTournamentBracketGUI(Player player) {
        Tournament tournament = plugin.getTournamentManager().getPlayerTournament(player.getUniqueId());
        if (tournament == null) {
            player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&cYou are not in a tournament."));
            return;
        }        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(BRACKET_TITLE));

        String state = switch (tournament.getState()) {
            case RECRUITING -> "&a&lRecruiting";
            case IN_PROGRESS -> "&e&lIn Progress";
            case FINISHED -> "&c&lFinished";
        };
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&5&l" + tournament.getName())
                .lore("",
                        "&7State: " + state,
                        "&7Round: &f" + tournament.getCurrentRound() + "&7/" + tournament.getTotalRounds(),
                        "&7Players: &f" + tournament.getParticipants().size(),
                        "", "&8&m                              ")
                .build());

        int slot = 9;
        for (Tournament.TournamentMatch match : tournament.getMatches()) {
            if (slot > 44) break;
            String p1 = nameOf(match.getPlayer1());
            String p2 = match.getPlayer2() != null ? nameOf(match.getPlayer2()) : "&7TBD";
            String winner = match.getWinner() != null ? nameOf(match.getWinner()) : "&7-";

            ItemBuilder builder = new ItemBuilder(match.isPlayed() ? Material.GOLDEN_APPLE : Material.MAP)
                    .name("&6&lRound " + match.getRound())
                    .lore("",
                            "&f" + p1 + " &7vs &f" + p2,
                            "&7Winner: " + (match.isPlayed() ? "&a" + winner : "&7-"),
                            "", "&8&m                              ");
            if (match.isPlayed()) builder.glow();
            gui.setItem(slot++, builder.build());
        }

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    private String formatPlaytime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public void openSpectatorSelectorGUI(Player player) {
        List<Player> targets = new ArrayList<>();
        for (Duel duel : plugin.getDuelManager().getActiveDuels()) {
            if (duel.getState() != DuelState.IN_PROGRESS) continue;
            for (UUID uuid : duel.getAllAlive()) {
                Player target = Bukkit.getPlayer(uuid);
                if (target != null) targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            player.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix("&cThere are no active duels right now."));
            return;
        }

        int size = Math.max(9, ((targets.size() + 8) / 9) * 9);
        Inventory gui = Bukkit.createInventory(null, size, ColorUtil.colorize(SPECTATOR_TITLE));

        int slot = 0;
        for (Player target : targets) {
            if (slot >= size) break;
            Duel duel = plugin.getDuelManager().getDuelOf(target.getUniqueId());
            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&b&l" + target.getName())
                    .lore("", duel != null ? "&7Duel in &f" + duel.getArenaName() + " &7(" + duel.getFormattedDuration() + ")" : "&7Not in a duel",
                            "", "&7Click to spectate",
                            "", "&8&m                              ");
            builder.meta(meta -> {
                if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
                    skull.setOwningPlayer(target);
                }
            });
            gui.setItem(slot++, builder.build());
        }

        fillWithGlass(gui, targets.size());
        player.openInventory(gui);
    }

    public void openVoteGUI(Player player, UUID duelId) {
        VotingManager.VoteSession session = plugin.getVotingManager().getSession(duelId);
        if (session == null || session.isResolved()) return;

        Inventory gui = Bukkit.createInventory(null, 9, ColorUtil.colorize(VOTE_TITLE));

        List<String> options = session.getOptions();
        String playerVote = session.getParticipantVotes().get(player.getUniqueId());
        for (int i = 0; i < options.size() && i < 9; i++) {
            String arena = options.get(i);
            int votes = session.getVoteCount(arena);
            boolean votedThis = arena.equals(playerVote);
            ItemBuilder builder = new ItemBuilder(VOTE_WOOL[i])
                    .name("&f&l" + arena)
                    .lore("",
                            "&7Votes: &f" + votes,
                            "",
                            votedThis ? "&a&lYour vote" : "&7Click to vote",
                            "", "&8&m                              ");
            if (votedThis) builder.glow();
            gui.setItem(i, builder.build());
        }

        fillWithGlass(gui, options.size());
        player.openInventory(gui);
    }

    private String nameOf(UUID uuid) {
        if (uuid == null) return "Unknown";
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    public void openCosmeticsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(COSMETICS_TITLE));

        gui.setItem(10, new ItemBuilder(Material.LIGHTNING_ROD)
                .name("&f&lKill Effect")
                .lore("", "&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getKillEffect(player.getUniqueId())),
                        "", "&8&m                              ")
                .build());

        gui.setItem(12, new ItemBuilder(Material.FIREWORK_ROCKET)
                .name("&f&lVictory Animation")
                .lore("", "&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getVictoryAnimation(player.getUniqueId())),
                        "", "&8&m                              ")
                .build());

        gui.setItem(14, new ItemBuilder(Material.ENDER_PEARL)
                .name("&f&lTrail")
                .lore("", "&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getTrail(player.getUniqueId())),
                        "", "&8&m                              ")
                .build());

        gui.setItem(16, new ItemBuilder(Material.WRITTEN_BOOK)
                .name("&f&lDeath Message")
                .lore("", "&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getDeathMessage(player.getUniqueId())),
                        "", "&8&m                              ")
                .build());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openCosmeticsCategoryGUI(Player player, String category) {
        String title;
        List<String> options;

        switch (category.toLowerCase()) {
            case "kill" -> {
                title = COSMETICS_KILL_TITLE;
                options = plugin.getCosmeticsManager().getAvailableKillEffects();
            }
            case "victory" -> {
                title = COSMETICS_VICTORY_TITLE;
                options = plugin.getCosmeticsManager().getAvailableVictoryAnimations();
            }
            case "trail" -> {
                title = COSMETICS_TRAIL_TITLE;
                options = plugin.getCosmeticsManager().getAvailableTrails();
            }
            case "deathmsg" -> {
                title = COSMETICS_DEATH_TITLE;
                options = plugin.getCosmeticsManager().getAvailableDeathMessages();
            }
            default -> {
                openCosmeticsGUI(player);
                return;
            }
        }

        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));

        gui.setItem(45, backButton());

        String current = getSelectedCosmetic(player, category);
        int slot = 0;
        for (String option : options) {
            if (slot >= 45) break;
            boolean selected = option.equalsIgnoreCase(current);
            ItemBuilder builder = new ItemBuilder(iconForCosmeticOption(option))
                    .name((selected ? "&a" : "&f") + "&l" + formatMaterialName(option))
                    .lore("", selected ? "&a&lSELECTED" : "&7Click to select",
                            "", "&8&m                              ");
            if (selected) builder.glow();
            gui.setItem(slot++, builder.build());
        }

        fillWithGlass(gui, options.size());
        player.openInventory(gui);
    }

    private String getSelectedCosmetic(Player player, String category) {
        return switch (category.toLowerCase()) {
            case "kill" -> plugin.getCosmeticsManager().getKillEffect(player.getUniqueId());
            case "victory" -> plugin.getCosmeticsManager().getVictoryAnimation(player.getUniqueId());
            case "trail" -> plugin.getCosmeticsManager().getTrail(player.getUniqueId());
            case "deathmsg" -> plugin.getCosmeticsManager().getDeathMessage(player.getUniqueId());
            default -> "";
        };
    }

    private Material iconForCosmeticOption(String option) {
        return switch (option.toLowerCase()) {
            case "none" -> Material.BARRIER;
            case "default" -> Material.PAPER;
            case "lightning", "lightning_rain" -> Material.LIGHTNING_ROD;
            case "firework", "firework_show" -> Material.FIREWORK_ROCKET;
            case "fireworks_ring" -> Material.FIREWORK_STAR;
            case "explosion" -> Material.TNT;
            case "soul" -> Material.SOUL_SAND;
            case "blood" -> Material.RED_DYE;
            case "electric" -> Material.GLOWSTONE_DUST;
            case "fire_pillar", "flame" -> Material.BLAZE_POWDER;
            case "ender_dragon" -> Material.DRAGON_EGG;
            case "portal" -> Material.OBSIDIAN;
            case "enchant" -> Material.ENCHANTING_TABLE;
            case "crit", "competitive" -> Material.DIAMOND_SWORD;
            case "smoke" -> Material.CAMPFIRE;
            case "heart" -> Material.RED_DYE;
            case "snow" -> Material.SNOWBALL;
            case "humorous" -> Material.EMERALD;
            case "dramatic" -> Material.REDSTONE_BLOCK;
            case "minimal" -> Material.BOOK;
            default -> Material.BARRIER;
        };
    }

    public FileConfiguration getGamemodesConfig() {
        return plugin.getExtraConfig("gamemodes.yml");
    }

    public List<String> getGamemodeArenas(String gamemode) {
        FileConfiguration config = getGamemodesConfig();
        if (config == null) return List.of();
        ConfigurationSection section = config.getConfigurationSection(gamemode);
        if (section == null) return List.of();
        return section.getStringList("arenas");
    }

    private String formatMaterialName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
