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
    public static final String DUEL_ROUNDS_TITLE = "Select Rounds";
    public static final String QUEUE_TITLE = "Queue";
    public static final String RANKED_QUEUE_TITLE = "Ranked Queue";
    public static final String VOTE_TITLE = "Vote for Arena";

    private static final Material[] VOTE_WOOL = {
            Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.LIGHT_BLUE_WOOL, Material.MAGENTA_WOOL,
            Material.PINK_WOOL, Material.PURPLE_WOOL, Material.GREEN_WOOL
    };

    private static final List<Material> COMBAT_ITEMS = List.of(
            Material.DIAMOND_SWORD, Material.DIAMOND_AXE, Material.DIAMOND_PICKAXE,
            Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE,
            Material.STONE_SWORD, Material.STONE_AXE, Material.STONE_PICKAXE,
            Material.WOODEN_SWORD, Material.WOODEN_AXE, Material.WOODEN_PICKAXE,
            Material.NETHERITE_SWORD, Material.NETHERITE_AXE, Material.NETHERITE_PICKAXE,
            Material.GOLDEN_SWORD, Material.GOLDEN_AXE, Material.GOLDEN_PICKAXE,
            Material.TRIDENT, Material.MACE, Material.BOW, Material.CROSSBOW,
            Material.SHIELD, Material.ARROW, Material.SPECTRAL_ARROW,
            Material.TIPPED_ARROW, Material.SPYGLASS
    );

    private static final List<Material> FOOD_ITEMS = List.of(
            Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_MUTTON,
            Material.COOKED_CHICKEN, Material.COOKED_RABBIT, Material.COOKED_COD,
            Material.COOKED_SALMON, Material.BREAD, Material.PUMPKIN_PIE,
            Material.GOLDEN_CARROT, Material.BAKED_POTATO, Material.MUSHROOM_STEW,
            Material.BEETROOT_SOUP, Material.RABBIT_STEW, Material.SUSPICIOUS_STEW,
            Material.CHORUS_FRUIT, Material.SWEET_BERRIES, Material.GLOW_BERRIES,
            Material.APPLE, Material.MELON_SLICE, Material.DRIED_KELP,
            Material.CARROT, Material.POTATO, Material.BEETROOT,
            Material.HONEY_BOTTLE
    );

    private static final List<Material> UTILITY_ITEMS = List.of(
            Material.ENDER_PEARL, Material.ENDER_EYE, Material.ELYTRA,
            Material.FIREWORK_ROCKET, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
            Material.ENDER_CHEST, Material.CHEST, Material.BARREL,
            Material.BOW, Material.FLINT_AND_STEEL,
            Material.COBWEB, Material.SLIME_BLOCK, Material.HONEY_BLOCK,
            Material.TRIDENT, Material.SHIELD, Material.TOTEM_OF_UNDYING,
            Material.TORCH, Material.SOUL_TORCH, Material.LANTERN,
            Material.SOUL_LANTERN, Material.CRAFTING_TABLE, Material.ANVIL,
            Material.BREWING_STAND, Material.CHEST_MINECART,
            Material.HOPPER, Material.BUCKET, Material.WATER_BUCKET,
            Material.LAVA_BUCKET, Material.MILK_BUCKET, Material.NAME_TAG,
            Material.SADDLE, Material.LEAD, Material.COMPASS,
            Material.CLOCK, Material.BOOK, Material.ENCHANTING_TABLE,
            Material.BONE_MEAL, Material.SHEARS, Material.FISHING_ROD
    );

    private static final List<Material> INGREDIENT_ITEMS = List.of(
            Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.NETHERITE_SCRAP, Material.ANCIENT_DEBRIS,
            Material.REDSTONE, Material.LAPIS_LAZULI, Material.QUARTZ,
            Material.GLOWSTONE_DUST, Material.BLAZE_POWDER, Material.BLAZE_ROD,
            Material.NETHER_WART, Material.FERMENTED_SPIDER_EYE,
            Material.SPIDER_EYE, Material.MAGMA_CREAM, Material.ENDER_PEARL,
            Material.DRAGON_BREATH, Material.PHANTOM_MEMBRANE,
            Material.EXPERIENCE_BOTTLE, Material.BOOK, Material.PAPER,
            Material.LEATHER, Material.STRING, Material.FLINT,
            Material.GUNPOWDER, Material.SLIME_BALL, Material.HONEY_BOTTLE,
            Material.SHULKER_SHELL, Material.ECHO_SHARD, Material.HEART_OF_THE_SEA,
            Material.NAUTILUS_SHELL, Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
            Material.END_CRYSTAL
    );

    public GUIManager(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    public void openKitEditorGUI(Player player, Kit kit) {
        Inventory gui = Bukkit.createInventory(null, 54,
                ColorUtil.colorize(KIT_EDITOR_TITLE + " - " + kit.getName()));

        ItemStack[] contents = kit.getContentsArray();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            gui.setItem(i, contents[i] != null ? contents[i].clone() : null);
        }

        ItemStack[] armor = kit.getArmorContents();
        if (armor != null) {
            gui.setItem(36, lockArmor(armor[0]));
            gui.setItem(37, lockArmor(armor[1]));
            gui.setItem(38, lockArmor(armor[2]));
            gui.setItem(39, lockArmor(armor[3]));
        }

        gui.setItem(46, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("&fRepair All")
                .lore("&7Restore all items to full durability")
                .build());

        gui.setItem(47, new ItemBuilder(Material.BARRIER)
                .name("&cClear All")
                .lore("&7Remove all items from this kit")
                .build());

        gui.setItem(48, new ItemBuilder(Material.LIME_DYE)
                .name("&aSave Kit")
                .lore("&7Save the kit and close the editor")
                .build());

        gui.setItem(50, new ItemBuilder(Material.RED_DYE)
                .name("&cDon't Save")
                .lore("&7Discard changes and close the editor")
                .build());

        player.openInventory(gui);
    }

    private ItemStack lockArmor(ItemStack item) {
        if (item == null) {
            item = new ItemStack(Material.BARRIER);
        }
        ItemBuilder builder = new ItemBuilder(item)
                .name("&cLocked")
                .lore("&7Armor cannot be edited", "&7It is saved with the kit automatically");
        builder.hideVanillaLore();
        return builder.build();
    }

    public void openKitRoomGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ColorUtil.colorize(KIT_ROOM_TITLE));

        gui.setItem(10, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&fCombat")
                .lore("&7Weapons, armor & tools")
                .build());

        gui.setItem(12, new ItemBuilder(Material.COOKED_BEEF)
                .name("&fFood")
                .lore("&7Food & consumables")
                .build());

        gui.setItem(14, new ItemBuilder(Material.ENDER_PEARL)
                .name("&fUtility")
                .lore("&7Tools & utility items")
                .build());

        gui.setItem(16, new ItemBuilder(Material.DIAMOND)
                .name("&fMaterials")
                .lore("&7Crafting ingredients & materials")
                .build());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openKitRoomCategoryGUI(Player player, String category) {
        Inventory gui;
        List<Material> items;

        switch (category.toLowerCase()) {
            case "combat" -> {
                gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(KIT_ROOM_TITLE + " - Combat"));
                items = COMBAT_ITEMS;
            }
            case "food" -> {
                gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(KIT_ROOM_TITLE + " - Food"));
                items = FOOD_ITEMS;
            }
            case "utility" -> {
                gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(KIT_ROOM_TITLE + " - Utility"));
                items = UTILITY_ITEMS;
            }
            case "ingredients" -> {
                gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(KIT_ROOM_TITLE + " - Materials"));
                items = INGREDIENT_ITEMS;
            }
            default -> {
                openKitRoomGUI(player);
                return;
            }
        }

        gui.setItem(45, backButton());

        int slot = 0;
        for (Material material : items) {
            if (slot >= 45) break;
            gui.setItem(slot++, new ItemBuilder(material)
                    .name("&f" + formatMaterialName(material.name()))
                    .build());
        }

        fillWithGlass(gui, items.size());
        player.openInventory(gui);
    }

    private void fillWithGlass(Inventory gui, int startSlot) {
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = startSlot; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                gui.setItem(i, glass);
            }
        }
    }

    private ItemStack backButton() {
        return new ItemBuilder(Material.ARROW)
                .name("&7Back")
                .lore("&7Return to previous menu")
                .build();
    }

    private ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("&cClose")
                .build();
    }

    public List<Kit> getVisiblePublicKits(Player player) {
        return plugin.getKitManager().getPublicKits().stream()
                .filter(kit -> kit.getPermissionNode() == null
                        || player.hasPermission("updraftduels.kit.public." + kit.getName().toLowerCase()))
                .sorted(Comparator.comparing(Kit::getName))
                .toList();
    }

    public void openDuelKitGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ColorUtil.colorize(DUEL_KIT_TITLE));

        List<Kit> kits = getVisiblePublicKits(player);
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

        gui.setItem(49, closeButton());

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openDuelRoundsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(DUEL_ROUNDS_TITLE));

        gui.setItem(10, new ItemBuilder(Material.IRON_SWORD)
                .name("&f1 Round")
                .lore("&7A quick single round")
                .build());
        gui.setItem(11, new ItemBuilder(Material.STONE_SWORD)
                .name("&f2 Rounds")
                .lore("&7Two rounds")
                .build());
        gui.setItem(12, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&f4 Rounds")
                .lore("&7Four rounds")
                .build());
        gui.setItem(13, new ItemBuilder(Material.NETHERITE_SWORD)
                .name("&f6 Rounds")
                .lore("&7Six rounds")
                .build());
        gui.setItem(14, new ItemBuilder(Material.MACE)
                .name("&f10 Rounds")
                .lore("&7Ten rounds")
                .build());
        gui.setItem(15, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&dCustom")
                .lore("&7Type a number in chat")
                .build());

        gui.setItem(18, backButton());
        gui.setItem(22, closeButton());

        fillWithGlass(gui, 0);
        player.openInventory(gui);
    }

    public void openQueueGUI(Player player) {
        player.openInventory(buildQueueGUI(player, false));
    }

    public void openRankedQueueGUI(Player player) {
        player.openInventory(buildQueueGUI(player, true));
    }

    private Inventory buildQueueGUI(Player player, boolean ranked) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ColorUtil.colorize(ranked ? RANKED_QUEUE_TITLE : QUEUE_TITLE));

        FileConfiguration config = getGamemodesConfig();
        int slot = 0;
        if (config != null) {
            for (String gamemode : config.getKeys(false)) {
                if (slot >= 18) break;
                String kit = config.getString(gamemode + ".kit", gamemode);
                String icon = config.getString(gamemode + ".icon", "PAPER");
                Material material = Material.matchMaterial(icon);
                if (material == null) material = Material.PAPER;

                gui.setItem(slot++, new ItemBuilder(material)
                        .name("&f" + gamemode)
                        .lore("&7Fighting: &f" + plugin.getQueueManager().getGamemodeFightingCount(gamemode),
                                "&7Queuing: &f" + plugin.getQueueManager().getGamemodeQueueSize(gamemode))
                        .hideVanillaLore()
                        .build());
            }
        }

        gui.setItem(22, closeButton());

        fillWithGlass(gui, slot);
        return gui;
    }

    public void openKitsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(KITS_TITLE));

        gui.setItem(11, new ItemBuilder(Material.ENDER_CHEST)
                .name("&aPersonal Kits")
                .lore("&7Browse your own kits")
                .build());

        gui.setItem(15, new ItemBuilder(Material.CHEST)
                .name("&bPublic Kits")
                .lore("&7Browse pre made kits")
                .build());

        fillWithGlass(gui, 0);
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
                    skullMeta.setDisplayName(ColorUtil.colorize("&b" + target.getName()));
                    skullMeta.setLore(profileHeadLore(stats));
                    head.setItemMeta(skullMeta);
                }
                gui.setItem(4, head);

                ItemStack[] statsItems = {
                        statItem(Material.EMERALD, "&aWins", stats.getWins()),
                        statItem(Material.REDSTONE, "&cLosses", stats.getLosses()),
                        statItem(Material.GOLDEN_APPLE, "&6Win Rate", String.format("%.1f%%", stats.getWinRate())),
                        statItem(Material.NETHER_STAR, "&bELO", stats.getElo()),
                        statItem(Material.DIAMOND, "&fRank", stats.getRankTier()),
                        statItem(Material.IRON_SWORD, "&eKills", stats.getKills()),
                        statItem(Material.FIRE_CHARGE, "&dWin Streak", stats.getWinStreak()),
                        statItem(Material.BLAZE_POWDER, "&cBest Streak", stats.getBestWinStreak()),
                        statItem(Material.PAPER, "&7Games Played", stats.getGamesPlayed())
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
                .lore("&7" + value)
                .build();
    }

    private List<String> profileHeadLore(DuelPlayerStats stats) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(stats.getRankTier());
        lore.add("&7ELO: &f" + stats.getElo());
        lore.add(rankProgress(stats.getElo()));
        lore.add("");
        lore.add("&7Wins: &f" + stats.getWins());
        lore.add("&7Losses: &f" + stats.getLosses());
        lore.add("&7Win Rate: &f" + String.format("%.1f%%", stats.getWinRate()));
        lore.add("&7Kills: &f" + stats.getKills());
        lore.add("&7Deaths: &f" + stats.getDeaths());
        lore.add("&7Win Streak: &f" + stats.getWinStreak());
        lore.add("&7Best Streak: &f" + stats.getBestWinStreak());
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
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(SETTINGS_TITLE));

        boolean ranked = plugin.getQueueManager().isRankedMode(player.getUniqueId());
        gui.setItem(10, new ItemBuilder(ranked ? Material.NETHERITE_INGOT : Material.IRON_INGOT)
                .name("&6Queue Mode: " + (ranked ? "Ranked" : "Unranked"))
                .lore("",
                        "&7" + (ranked ? "You fight for ELO & rewards" : "Casual matches, no ELO"))
                .build());

        gui.setItem(12, new ItemBuilder(Material.PLAYER_HEAD)
                .name("&fProfile")
                .lore("&7View your stats")
                .build());

        gui.setItem(13, new ItemBuilder(Material.CHEST)
                .name("&fKits")
                .lore("&7Browse and equip kits")
                .build());

        gui.setItem(14, new ItemBuilder(Material.FIREWORK_ROCKET)
                .name("&fCosmetics")
                .lore("&7Manage your cosmetics")
                .build());

        gui.setItem(16, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("&fKit Room")
                .lore("&7Grab items for your kit")
                .build());

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
                    .name("&f" + ruleset.getDisplayName())
                    .lore("&7" + ruleset.getDescription(),
                            "",
                            isSelected ? "&aSelected" : "&7Click to select",
                            "&8Shift-click for details")
                    .build());
        }

        fillWithGlass(gui, slot);
        player.openInventory(gui);
    }

    public void openRulesetDetailsGUI(Player player, Ruleset ruleset) {
        Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.colorize(RULESET_DETAILS_TITLE));

        gui.setItem(4, new ItemBuilder(Material.BOOK)
                .name("&f" + ruleset.getDisplayName())
                .lore("",
                        "&7" + ruleset.getDescription(),
                        "",
                        "&7ID: &f" + ruleset.getId())
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
                .name((enabled ? "&a" : "&c") + label)
                .lore(enabled ? "&aEnabled" : "&cDisabled")
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
                .name("&9" + partyName)
                .lore("",
                        "&7Members: &f" + party.getMembers().size(),
                        "&7Ready: &f" + party.getReadyMembers().size() + "&7/" + party.getMembers().size())
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
                    .name("&f" + name)
                    .lore("",
                            party.isLeader(memberUUID) ? "&bLeader" : "&7Member",
                            ready ? "&aReady" : "&7Not ready")
                    .lore("", "&7Click to kick");
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
                    .name("&7" + name)
                    .lore("&7Pending invite")
                    .build());
        }

        boolean isLeader = party.isLeader(player.getUniqueId());
        gui.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("&cLeave Party")
                .build());
        if (isLeader) {
            gui.setItem(50, new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("&cDisband Party")
                    .build());
        }
        gui.setItem(51, new ItemBuilder(Material.EMERALD)
                .name("&aReady")
                .lore("",
                        "&7Mark yourself ready",
                        "",
                        party.getReadyMembers().contains(player.getUniqueId()) ? "&aYou are ready" : "&7Not ready")
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
                    .name("&f" + leaderName)
                    .lore("&7Status: " + (inFight ? "&cIn Fight" : "&aAvailable"),
                            "&7Players: &f" + party.getSize())
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
                .name("&fKills")
                .lore("&7Top 10 by kills")
                .build());
        gui.setItem(12, new ItemBuilder(Material.SKELETON_SKULL)
                .name("&fDeaths")
                .lore("&7Top 10 by deaths")
                .build());
        gui.setItem(14, new ItemBuilder(Material.CLOCK)
                .name("&fPlaytime")
                .lore("&7Top 10 by playtime")
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
                        default -> Material.PAPER;
                    };
                    String value = switch (columnFinal) {
                        case "deaths" -> "&c" + stats.getDeaths() + " deaths";
                        case "playtime" -> "&e" + formatPlaytime(stats.getPlaytime());
                        default -> "&a" + stats.getKills() + " kills";
                    };
                    gui.setItem(slot++, new ItemBuilder(medal)
                            .name("&f#" + place + " " + stats.getName())
                            .lore(value)
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
                .name("&f1v1")
                .lore("&7Solo tournament")
                .hideVanillaLore()
                .build());
        gui.setItem(11, new ItemBuilder(Material.SKELETON_SKULL)
                .name("&f2v2")
                .lore("&7Duos tournament")
                .hideVanillaLore()
                .build());
        gui.setItem(12, new ItemBuilder(Material.ZOMBIE_HEAD)
                .name("&f3v3")
                .lore("&7Trios tournament")
                .hideVanillaLore()
                .build());
        gui.setItem(13, new ItemBuilder(Material.CREEPER_HEAD)
                .name("&f4v4")
                .lore("&7Squads tournament")
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
            case RECRUITING -> "&aRecruiting";
            case IN_PROGRESS -> "&eIn Progress";
            case FINISHED -> "&cFinished";
        };
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&5" + tournament.getName())
                .lore("",
                        "&7State: " + state,
                        "&7Round: &f" + tournament.getCurrentRound() + "&7/" + tournament.getTotalRounds(),
                        "&7Players: &f" + tournament.getParticipants().size())
                .build());

        int slot = 9;
        for (Tournament.TournamentMatch match : tournament.getMatches()) {
            if (slot > 44) break;
            String p1 = nameOf(match.getPlayer1());
            String p2 = match.getPlayer2() != null ? nameOf(match.getPlayer2()) : "&7TBD";
            String winner = match.getWinner() != null ? nameOf(match.getWinner()) : "&7-";

            ItemBuilder builder = new ItemBuilder(match.isPlayed() ? Material.GOLDEN_APPLE : Material.MAP)
                    .name("&6Round " + match.getRound())
                    .lore("",
                            "&f" + p1 + " &7vs &f" + p2,
                            "&7Winner: " + (match.isPlayed() ? "&a" + winner : "&7-"));
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
                    .name("&b" + target.getName())
                    .lore(duel != null ? "&7Duel in &f" + duel.getArenaName() + " &7(" + duel.getFormattedDuration() + ")" : "&7Not in a duel");
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
                    .name("&f" + arena)
                    .lore("",
                            "&7Votes: &f" + votes,
                            "",
                            votedThis ? "&aYour vote" : "&7Click to vote");
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
                .name("&fKill Effect")
                .lore("&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getKillEffect(player.getUniqueId())))
                .build());

        gui.setItem(12, new ItemBuilder(Material.FIREWORK_ROCKET)
                .name("&fVictory Animation")
                .lore("&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getVictoryAnimation(player.getUniqueId())))
                .build());

        gui.setItem(14, new ItemBuilder(Material.ENDER_PEARL)
                .name("&fTrail")
                .lore("&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getTrail(player.getUniqueId())))
                .build());

        gui.setItem(16, new ItemBuilder(Material.PAPER)
                .name("&fDeath Message")
                .lore("&7Selected: &f" + formatMaterialName(plugin.getCosmeticsManager().getDeathMessage(player.getUniqueId())))
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
                    .name((selected ? "&a" : "&f") + formatMaterialName(option))
                    .lore(selected ? "&aSelected" : "&7Click to select");
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
            default -> Material.PAPER;
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
