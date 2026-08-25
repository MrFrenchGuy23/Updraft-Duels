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
import com.updraftduels.gui.GUIManager;
import com.updraftduels.manager.QueueManager;
import com.updraftduels.manager.VotingManager;
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelState;
import com.updraftduels.model.Kit;
import com.updraftduels.model.Party;
import com.updraftduels.model.PendingDuelSelection;
import com.updraftduels.model.Ruleset;
import com.updraftduels.model.Tournament;
import com.updraftduels.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GUIListener implements Listener {
    private final UpdraftDuels plugin;
    private final java.util.Set<java.util.UUID> kitSaveOnClose = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public GUIListener(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())
                && event.getClickedInventory() == player.getInventory()) {
            event.setCancelled(true);
            return;
        }

        if (event.getClickedInventory() == player.getInventory()
                && plugin.getPartyManager().isPartyItem(event.getCurrentItem())) {
            event.setCancelled(true);
            plugin.getPartyManager().executePartyItem(player, event.getCurrentItem());
            return;
        }

        if (event.getClickedInventory() != null
                && event.getClickedInventory() != player.getInventory()
                && event.getClickedInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) {
            playClickSound(player);
        }

        String title = event.getView().getTitle();

        if (title.contains("Kit Editor")) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                event.setCancelled(true);
                return;
            }
            int slot = event.getSlot();

            if (slot == 53) {
                event.setCancelled(true);
                kitSaveOnClose.add(player.getUniqueId());
                player.closeInventory();
                return;
            }

            if (slot == 51) {
                event.setCancelled(true);
                ItemStack[] inv = player.getInventory().getContents();
                Inventory topInv = event.getView().getTopInventory();
                for (int i = 0; i < 36; i++) {
                    topInv.setItem(i, inv[i] != null ? inv[i].clone() : null);
                }
                player.sendMessage(ColorUtil.colorizePrefix("&aImported inventory into kit editor!"));
                return;
            }

            if (slot == 52) {
                event.setCancelled(true);
                if (event.isShiftClick()) {
                    Inventory topInv = event.getView().getTopInventory();
                    for (int i = 0; i < 41; i++) {
                        topInv.setItem(i, null);
                    }
                    player.sendMessage(ColorUtil.colorizePrefix("&cKit cleared!"));
                }
                return;
            }

            if (slot >= 45 && slot <= 49) {
                event.setCancelled(true);
                return;
            }

            if (slot >= 41 && slot <= 53) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        if (title.contains("Kit Room")) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            event.setCancelled(true);
            handleKitRoomClick(player, event.getSlot(), event.getCurrentItem(), title, event.getClick());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.KITS_TITLE))) {
            event.setCancelled(true);
            handleKitsClick(player, event.getSlot(), event.getCurrentItem(), event.getClick());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.PERSONAL_KITS_TITLE))) {
            event.setCancelled(true);
            handlePersonalKitsClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.PUBLIC_KITS_TITLE))) {
            event.setCancelled(true);
            handlePublicKitsClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.COSMETICS_TITLE))) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 10 -> plugin.getGuiManager().openCosmeticsCategoryGUI(player, "kill");
                case 12 -> plugin.getGuiManager().openCosmeticsCategoryGUI(player, "victory");
                case 14 -> plugin.getGuiManager().openCosmeticsCategoryGUI(player, "trail");
                case 16 -> plugin.getGuiManager().openCosmeticsCategoryGUI(player, "deathmsg");
            }
            return;
        }

        if (isCosmeticsCategoryTitle(title)) {
            event.setCancelled(true);
            handleCosmeticsCategoryClick(player, event.getSlot(), event.getCurrentItem(), title);
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.SETTINGS_TITLE))) {
            event.setCancelled(true);
            handleSettingsClick(player, event.getSlot());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.RULESETS_TITLE))) {
            event.setCancelled(true);
            handleRulesetsClick(player, event.getSlot(), event.getCurrentItem(), event.isShiftClick());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.RULESET_DETAILS_TITLE))) {
            event.setCancelled(true);
            if (event.getSlot() == 18) plugin.getGuiManager().openRulesetsGUI(player);
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.VOTE_TITLE))) {
            event.setCancelled(true);
            handleVoteClick(player, event.getSlot());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.PARTY_TITLE))) {
            event.setCancelled(true);
            handlePartyClick(player, event.getSlot());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.PARTY_DUEL_TITLE))) {
            event.setCancelled(true);
            handlePartyDuelClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.LEADERBOARD_TITLE))) {
            event.setCancelled(true);
            switch (event.getSlot()) {
                case 10 -> plugin.getGuiManager().openLeaderboardCategoryGUI(player, "kills");
                case 12 -> plugin.getGuiManager().openLeaderboardCategoryGUI(player, "deaths");
                case 14 -> plugin.getGuiManager().openLeaderboardCategoryGUI(player, "playtime");
            }
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.LEADERBOARD_KILLS_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.LEADERBOARD_DEATHS_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.LEADERBOARD_PLAYTIME_TITLE))) {
            event.setCancelled(true);
            if (event.getSlot() == 45) plugin.getGuiManager().openLeaderboardGUI(player);
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.BRACKET_TITLE))) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.TOURNAMENT_FORMAT_TITLE))) {
            event.setCancelled(true);
            handleTournamentFormatClick(player, event.getSlot());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.SPECTATOR_TITLE))) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            event.setCancelled(true);
            handleSpectatorSelectorClick(player, event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.DUEL_KIT_TYPE_TITLE))) {
            event.setCancelled(true);
            handleDuelKitTypeClick(player, event.getSlot());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.DUEL_KIT_TITLE))) {
            event.setCancelled(true);
            handleDuelKitClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.DUEL_ROUNDS_TITLE))) {
            event.setCancelled(true);
            handleDuelRoundsClick(player, event.getSlot(), event.getCurrentItem());
            return;
        }

        if (title.equals(ColorUtil.colorize(GUIManager.QUEUE_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.CASUAL_QUEUE_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.COMPETITIVE_QUEUE_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.BOTH_QUEUE_TITLE))) {
            event.setCancelled(true);
            QueueManager.MatchmakingMode mode = QueueManager.MatchmakingMode.BOTH;
            if (title.equals(ColorUtil.colorize(GUIManager.CASUAL_QUEUE_TITLE))) {
                mode = QueueManager.MatchmakingMode.CASUAL;
            } else if (title.equals(ColorUtil.colorize(GUIManager.COMPETITIVE_QUEUE_TITLE))) {
                mode = QueueManager.MatchmakingMode.COMPETITIVE;
            }
            handleQueueClick(player, event.getSlot(), event.getCurrentItem(), mode);
            return;
        }

        if (title.startsWith(ColorUtil.colorize(GUIManager.PROFILE_TITLE))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (plugin.getPartyManager().isPartyItem(item)) {
                event.setCancelled(true);
                plugin.getPartyManager().executePartyItem(player, item);
            }
        }
    }

    private void handleTournamentFormatClick(Player player, int slot) {
        UUID tournamentId = plugin.getTournamentManager().getPendingFormat(player.getUniqueId());
        if (tournamentId == null) {
            player.closeInventory();
            return;
        }
        Tournament tournament = plugin.getTournamentManager().getTournament(tournamentId);
        if (tournament == null) {
            player.closeInventory();
            return;
        }
        int teamSize = switch (slot) {
            case 10 -> 1;
            case 11 -> 2;
            case 12 -> 3;
            case 13 -> 4;
            default -> -1;
        };
        if (teamSize < 1) return;
        tournament.setTeamSize(teamSize);
        player.sendMessage(ColorUtil.colorizePrefix("&aTournament format set to &f" + teamSize + "v" + teamSize));
        player.closeInventory();
    }

    private void handleSpectatorSelectorClick(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(name);
        if (target == null) {
            player.closeInventory();
            return;
        }
        Duel duel = plugin.getDuelManager().getDuelOf(target.getUniqueId());
        if (duel == null || duel.getState() != com.updraftduels.model.DuelState.IN_PROGRESS) {
            player.closeInventory();
            return;
        }
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            plugin.getSpectatorManager().followPlayer(player, target);
        } else {
            plugin.getSpectatorManager().startSpectating(player, target, duel);
        }
        player.closeInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingDuelSelection pending = plugin.getDuelManager().getPendingDuelSelection(player.getUniqueId());
        if (pending == null || !pending.isAwaitingChat()) {
            handlePartyChat(event, player);
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ColorUtil.colorizePrefix("&cDuel request cancelled.")));
            return;
        }

        final int rounds;
        try {
            rounds = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ColorUtil.colorizePrefix("&cThat's not a valid number. Type a number like &f3&c, or type &fcancel&c.")));
            return;
        }
        if (rounds < 1 || rounds > 100) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ColorUtil.colorizePrefix("&cRounds must be between &f1&c and &f100&c.")));
            return;
        }

        final int finalRounds = rounds;
        Bukkit.getScheduler().runTask(plugin, () -> {
            PendingDuelSelection current = plugin.getDuelManager().getPendingDuelSelection(player.getUniqueId());
            if (current == null || !current.isAwaitingChat()) return;
            if (!player.isOnline()) {
                plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
                return;
            }
            Player target = Bukkit.getPlayer(current.getTarget());
            if (target == null) {
                plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
                player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline",
                        "%player%", current.getTarget().toString())));
                return;
            }
            plugin.getDuelManager().sendDuelRequest(player, target, current.getKitName(), finalRounds);
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
        });
    }

    private void handlePartyChat(AsyncPlayerChatEvent event, Player player) {
        if (!plugin.getPartyManager().isPartyChatEnabled(player.getUniqueId())) {
            handleChatMentions(event, player);
            return;
        }
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            plugin.getPartyManager().setPartyChat(player.getUniqueId(), false);
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage(ColorUtil.colorize(
                        plugin.getMessages().get("party.chat-prefix") + "&f" + player.getName() + ": &7" + message));
            }
        }
    }

    private void handleChatMentions(AsyncPlayerChatEvent event, Player player) {
        String message = event.getMessage();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            if (!plugin.isChatMentions(online.getUniqueId())) continue;
            if (message.toLowerCase().contains("@" + online.getName().toLowerCase())) {
                online.playSound(online.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                online.playSound(online.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
                online.sendMessage(ColorUtil.colorize(
                        "&e&lMention &7by &f" + player.getName() + "&7: &f" + message));
            }
        }
    }

    private void handleQueueClick(Player player, int slot, ItemStack item, QueueManager.MatchmakingMode mode) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        if (slot == 22) {
            player.closeInventory();
            return;
        }

        FileConfiguration config = plugin.getGuiManager().getGamemodesConfig();
        if (config == null) return;
        List<String> gamemodes = new ArrayList<>(config.getKeys(false));
        if (slot < 0 || slot >= gamemodes.size()) return;

        String gamemode = gamemodes.get(slot);
        player.closeInventory();
        plugin.getQueueCommand().joinGamemode(player, gamemode, mode);
    }

    private void handleDuelKitTypeClick(Player player, int slot) {
        PendingDuelSelection pending = plugin.getDuelManager().getPendingDuelSelection(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }
        if (slot == 22) {
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (slot == 11) {
            pending.setPersonalKit(false);
            plugin.getGuiManager().openDuelPublicKitGUI(player);
        } else if (slot == 15) {
            pending.setPersonalKit(true);
            plugin.getGuiManager().openDuelPersonalKitGUI(player);
        }
    }

    private void handleDuelKitClick(Player player, int slot, ItemStack item) {
        PendingDuelSelection pending = plugin.getDuelManager().getPendingDuelSelection(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }
        if (slot == 49) {
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        List<Kit> kits = pending.isPersonalKit()
                ? plugin.getKitManager().getPersonalKits(player.getUniqueId())
                : plugin.getGuiManager().getVisiblePublicKits(player);
        if (slot < 0 || slot >= kits.size()) return;
        pending.setKitName(kits.get(slot).getName());
        plugin.getGuiManager().openDuelRoundsGUI(player);
    }

    private void handleDuelRoundsClick(Player player, int slot, ItemStack item) {
        PendingDuelSelection pending = plugin.getDuelManager().getPendingDuelSelection(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }
        if (slot == 18) {
            plugin.getGuiManager().openDuelKitGUI(player);
            return;
        }
        if (slot == 22) {
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        int rounds;
        switch (slot) {
            case 10 -> rounds = 1;
            case 11 -> rounds = 2;
            case 12 -> rounds = 4;
            case 13 -> rounds = 6;
            case 14 -> rounds = 10;
            case 15 -> {
                pending.setAwaitingChat(true);
                player.closeInventory();
                player.sendMessage(ColorUtil.colorizePrefix("&eType the amount of rounds in chat (e.g. &f3&e) or &fcancel&e."));
                return;
            }
            default -> { return; }
        }
        finishDuelRounds(player, pending, rounds);
    }

    private void finishDuelRounds(Player player, PendingDuelSelection pending, int rounds) {
        Player target = Bukkit.getPlayer(pending.getTarget());
        if (target == null) {
            plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
            String name = Bukkit.getOfflinePlayer(pending.getTarget()).getName();
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("general.player-offline",
                    "%player%", name != null ? name : pending.getTarget().toString())));
            player.closeInventory();
            return;
        }
        pending.setRounds(rounds);
        plugin.getDuelManager().sendDuelRequest(player, target, pending.getKitName(), rounds);
        plugin.getDuelManager().removePendingDuelSelection(player.getUniqueId());
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(ColorUtil.colorize(GUIManager.VOTE_TITLE))) {
            Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
            if (duel != null && duel.getState() == DuelState.WAITING) {
                VotingManager.VoteSession session = plugin.getVotingManager().getSession(duel.getId());
                if (session != null && !session.isResolved()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline() && duel.getState() == DuelState.WAITING) {
                            plugin.getGuiManager().openVoteGUI(player, duel.getId());
                        }
                    }, 1L);
                }
            }
            return;
        }

        if (!title.contains("Kit Editor")) return;

        player.setItemOnCursor(null);

        String kitName = title.substring(title.lastIndexOf(" - ") + 3);
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;

        if (!kitSaveOnClose.remove(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorizePrefix("&cKit changes discarded."));
            return;
        }

        Inventory topInv = event.getView().getTopInventory();
        ItemStack[] contents = new ItemStack[41];
        for (int i = 0; i < 41; i++) {
            ItemStack s = topInv.getItem(i);
            contents[i] = s != null && s.getType() != Material.AIR ? s.clone() : null;
        }
        plugin.getKitManager().updateKit(kitName, player.getUniqueId(), contents, kit.getArmorContents(), kit.getOffHand());
        player.sendMessage(ColorUtil.colorizePrefix("&aKit &f" + kitName + " &ahas been saved!"));
    }

    private void handleVoteClick(Player player, int slot) {
        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null || duel.getState() != DuelState.WAITING) {
            player.closeInventory();
            return;
        }
        VotingManager.VoteSession session = plugin.getVotingManager().getSession(duel.getId());
        if (session == null || session.isResolved()) {
            player.closeInventory();
            return;
        }
        List<String> options = session.getOptions();
        if (slot < 0 || slot >= options.size()) return;

        if (plugin.getVotingManager().castVote(duel.getId(), player.getUniqueId(), options.get(slot))) {
            plugin.getGuiManager().openVoteGUI(player, duel.getId());
        } else {
            player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("voting.already-voted")));
            plugin.getGuiManager().openVoteGUI(player, duel.getId());
        }
    }

    private void handleKitRoomClick(Player player, int slot, ItemStack item, String title, org.bukkit.event.inventory.ClickType clickType) {
        if (slot == 53) {
            if (player.hasPermission("updraftduels.kit.editkitroom") && clickType.isShiftClick() && clickType.isRightClick()) {
                Inventory topInv = player.getOpenInventory().getTopInventory();
                int currentPage = 0;
                ItemStack editButton = topInv.getItem(53);
                if (editButton != null && editButton.getAmount() > 0) {
                    currentPage = editButton.getAmount() - 1;
                }
                ItemStack[] kitroom = new ItemStack[45];
                for (int i = 0; i < 45; i++) {
                    ItemStack s = topInv.getItem(i);
                    kitroom[i] = s != null ? s.clone() : null;
                }
                plugin.getKitRoomManager().setPage(currentPage, kitroom);
                plugin.getKitRoomManager().savePageToDatabase(currentPage);
                player.sendMessage(ColorUtil.colorizePrefix("&aSaved kit room page " + (currentPage + 1) + "!"));
            }
            return;
        }

        if (slot >= 47 && slot <= 51) {
            int pageIndex = slot - 47;
            plugin.getGuiManager().openKitRoomGUI(player, pageIndex);
            return;
        }

        if (slot == 45) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.sendMessage(ColorUtil.colorizePrefix("&aRefilled health and hunger!"));
            return;
        }

        if (slot >= 0 && slot < 45 && item != null && item.getType() != Material.AIR && item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            if (!player.hasPermission("updraftduels.kit.editkitroom")) {
                player.getInventory().addItem(item.clone());
                player.sendMessage(ColorUtil.colorizePrefix("&aAdded &f" + item.getType().name().replace("_", " ").toLowerCase() + " &ato your inventory."));
            }
        }
    }

    private void handleSettingsClick(Player player, int slot) {
        switch (slot) {
            case 10 -> {
                QueueManager.MatchmakingMode current = plugin.getQueueManager().getMatchmakingMode(player.getUniqueId());
                QueueManager.MatchmakingMode next = switch (current) {
                    case CASUAL -> QueueManager.MatchmakingMode.COMPETITIVE;
                    case COMPETITIVE -> QueueManager.MatchmakingMode.BOTH;
                    case BOTH -> QueueManager.MatchmakingMode.CASUAL;
                };
                plugin.getQueueManager().setMatchmakingMode(player.getUniqueId(), next);
                String msg = switch (next) {
                    case CASUAL -> "&7Matchmaking set to &7Casual";
                    case COMPETITIVE -> "&6Matchmaking set to &6Competitive";
                    case BOTH -> "&bMatchmaking set to &bBoth";
                };
                player.sendMessage(ColorUtil.colorizePrefix(msg));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 11 -> {
                boolean enabled = !plugin.isAutoGG(player.getUniqueId());
                plugin.setAutoGG(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aAuto-GG enabled." : "&7Auto-GG disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 12 -> {
                boolean enabled = !plugin.isAutoRequeue(player.getUniqueId());
                plugin.setAutoRequeue(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aAuto Requeue enabled." : "&7Auto Requeue disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 13 -> {
                boolean enabled = !plugin.isPartyInvites(player.getUniqueId());
                plugin.setPartyInvites(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aParty invites enabled." : "&7Party invites disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 14 -> {
                boolean enabled = !plugin.isSpectators(player.getUniqueId());
                plugin.setSpectators(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aSpectators enabled." : "&7Spectators disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 15 -> {
                boolean enabled = !plugin.isScoreboard(player.getUniqueId());
                plugin.setScoreboard(player.getUniqueId(), enabled);
                Bukkit.dispatchCommand(player, "sb");
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aScoreboard enabled." : "&7Scoreboard disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 16 -> {
                boolean enabled = !plugin.isChatMentions(player.getUniqueId());
                plugin.setChatMentions(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aChat mentions enabled." : "&7Chat mentions disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 17 -> {
                boolean enabled = !plugin.isDuelRequests(player.getUniqueId());
                plugin.setDuelRequests(player.getUniqueId(), enabled);
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aDuel requests enabled." : "&7Duel requests disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 28 -> {
                boolean enabled = !plugin.getConfig().getBoolean("ping-limit.enabled", false);
                plugin.getConfig().set("ping-limit.enabled", enabled);
                plugin.saveConfig();
                player.sendMessage(ColorUtil.colorizePrefix(enabled ? "&aPing limit enabled." : "&7Ping limit disabled."));
                plugin.getGuiManager().openSettingsGUI(player);
            }
            case 31 -> {
                int current = plugin.getConfig().getInt("ping-limit.max-ping", 200);
                int next = switch (current) {
                    case 100 -> 200;
                    case 200 -> 300;
                    case 300 -> 500;
                    default -> 100;
                };
                plugin.getConfig().set("ping-limit.max-ping", next);
                plugin.getConfig().set("ping-limit.enabled", true);
                plugin.saveConfig();
                player.sendMessage(ColorUtil.colorizePrefix("&eMax ping set to &f" + next + "ms"));
                plugin.getGuiManager().openSettingsGUI(player);
            }
        }
    }

    private void handleRulesetsClick(Player player, int slot, ItemStack item, boolean shiftClick) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        List<Ruleset> rulesets = new ArrayList<>(plugin.getRulesetManager().getAllRulesets());
        if (slot < 0 || slot >= rulesets.size()) return;
        Ruleset ruleset = rulesets.get(slot);
        if (shiftClick) {
            plugin.getGuiManager().openRulesetDetailsGUI(player, ruleset);
            return;
        }
        plugin.getRulesetManager().setSelectedRuleset(player.getUniqueId(), ruleset.getId());
        player.sendMessage(ColorUtil.colorize(plugin.getMessages().get("rules.selected", "%ruleset%", ruleset.getId())));
        plugin.getGuiManager().openRulesetsGUI(player);
    }

    private void handlePartyClick(Player player, int slot) {
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            plugin.getPartyManager().leaveParty(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (slot == 50 && party.isLeader(player.getUniqueId())) {
            plugin.getPartyManager().disbandParty(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (slot == 51) {
            party.getReadyMembers().add(player.getUniqueId());
            player.sendMessage(ColorUtil.colorizePrefix("&aYou are ready!"));
            if (party.isReadyCheckComplete()) {
                for (UUID uuid : party.getMembers()) {
                    Player member = Bukkit.getPlayer(uuid);
                    if (member != null) {
                        member.sendMessage(ColorUtil.colorize(plugin.getMessages().get("party.ready-check-complete")));
                    }
                }
            }
            plugin.getGuiManager().openPartyGUI(player);
            return;
        }
        if (slot >= 10 && slot <= 16 && party.isLeader(player.getUniqueId())) {
            List<UUID> members = party.getMembers();
            int index = slot - 10;
            if (index >= 0 && index < members.size() && !party.isLeader(members.get(index))) {
                plugin.getPartyManager().kickMember(player.getUniqueId(), members.get(index));
                plugin.getGuiManager().openPartyGUI(player);
            }
        }
    }

    private void handlePartyDuelClick(Player player, int slot, ItemStack item) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        Party myParty = plugin.getPartyManager().getParty(player.getUniqueId());
        if (myParty == null || !myParty.isLeader(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorizePrefix("&cOnly the party leader can duel other parties."));
            player.closeInventory();
            return;
        }

        List<Party> parties = new ArrayList<>();
        for (Party party : plugin.getPartyManager().getAllParties().values()) {
            if (party.getPartyId().equals(myParty.getPartyId())) continue;
            parties.add(party);
        }
        if (slot < 0 || slot >= parties.size()) return;

        player.closeInventory();
        plugin.getPartyCommand().challengeParty(player, parties.get(slot));
    }

    private void handleKitsClick(Player player, int slot, ItemStack item, org.bukkit.event.inventory.ClickType clickType) {
        if (slot == 37) {
            plugin.getGuiManager().openKitRoomGUI(player);
            return;
        }
        if (slot == 38) {
            plugin.getGuiManager().openPublicKitsGUI(player);
            return;
        }
        if (slot == 41) {
            if (clickType.isShiftClick()) {
                player.getInventory().clear();
                player.sendMessage(ColorUtil.colorizePrefix("&cInventory cleared!"));
            }
            return;
        }
        if (slot == 43) {
            for (ItemStack s : player.getInventory().getContents()) {
                if (s != null && s.getType() != Material.AIR && s.getItemMeta() instanceof Damageable damageable) {
                    damageable.setDamage(0);
                    s.setItemMeta(damageable);
                }
            }
            for (ItemStack s : player.getInventory().getArmorContents()) {
                if (s != null && s.getType() != Material.AIR && s.getItemMeta() instanceof Damageable damageable) {
                    damageable.setDamage(0);
                    s.setItemMeta(damageable);
                }
            }
            player.sendMessage(ColorUtil.colorizePrefix("&aAll items repaired!"));
            return;
        }

        if (slot >= 9 && slot < 18) {
            int kitSlot = slot - 8;
            Kit kit = plugin.getKitManager().getKitBySlot(player.getUniqueId(), kitSlot);
            if (kit == null) {
                player.closeInventory();
                player.sendMessage(ColorUtil.colorizePrefix("&eHold the items you want in your kit, then type &f/kit create " + kitSlot));
                return;
            }
            if (clickType.isLeftClick()) {
                equipKit(player, kit);
            } else if (clickType.isRightClick()) {
                plugin.getGuiManager().openKitEditorGUI(player, kit);
            }
            return;
        }

        if (slot >= 18 && slot < 36) {
            int kitSlot = slot - 17;
            Kit kit = plugin.getKitManager().getKitBySlot(player.getUniqueId(), kitSlot);
            if (kit != null) {
                plugin.getGuiManager().openKitEditorGUI(player, kit);
            } else {
                player.closeInventory();
                player.sendMessage(ColorUtil.colorizePrefix("&eHold the items you want in your kit, then type &f/kit create " + kitSlot));
            }
            return;
        }
    }

    private void handlePersonalKitsClick(Player player, int slot, ItemStack item) {
        if (slot == 45) {
            plugin.getGuiManager().openKitsGUI(player);
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        List<Kit> kits = plugin.getGuiManager().getPersonalKits(player);
        if (slot < 0 || slot >= kits.size()) return;
        equipKit(player, kits.get(slot));
    }

    private void handlePublicKitsClick(Player player, int slot, ItemStack item) {
        if (slot == 45) {
            plugin.getGuiManager().openKitsGUI(player);
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        List<Kit> kits = plugin.getGuiManager().getPublicKitsForPlayer(player);
        if (slot < 0 || slot >= kits.size()) return;
        equipKit(player, kits.get(slot));
    }

    private void equipKit(Player player, Kit kit) {
        player.getInventory().clear();
        player.getInventory().setContents(kit.getContentsArray());
        player.getInventory().setArmorContents(kit.getArmorContents());
        player.getInventory().setItemInOffHand(kit.getOffHand() != null ? kit.getOffHand() : new ItemStack(Material.AIR));
        player.sendMessage(ColorUtil.colorizePrefix("&aEquipped kit &f" + kit.getName()));
        player.closeInventory();
    }

    private boolean isCosmeticsCategoryTitle(String title) {
        return title.equals(ColorUtil.colorize(GUIManager.COSMETICS_KILL_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.COSMETICS_VICTORY_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.COSMETICS_TRAIL_TITLE))
                || title.equals(ColorUtil.colorize(GUIManager.COSMETICS_DEATH_TITLE));
    }

    private void handleCosmeticsCategoryClick(Player player, int slot, ItemStack item, String title) {
        if (slot == 45) {
            plugin.getGuiManager().openCosmeticsGUI(player);
            return;
        }
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        String category = cosmeticsCategoryForTitle(title);
        if (category == null) return;
        List<String> options = cosmeticsOptions(category);
        if (slot < 0 || slot >= options.size()) return;

        applyCosmetic(player, category, options.get(slot));
        plugin.getGuiManager().openCosmeticsCategoryGUI(player, category);
    }

    private String cosmeticsCategoryForTitle(String title) {
        if (title.equals(ColorUtil.colorize(GUIManager.COSMETICS_KILL_TITLE))) return "kill";
        if (title.equals(ColorUtil.colorize(GUIManager.COSMETICS_VICTORY_TITLE))) return "victory";
        if (title.equals(ColorUtil.colorize(GUIManager.COSMETICS_TRAIL_TITLE))) return "trail";
        if (title.equals(ColorUtil.colorize(GUIManager.COSMETICS_DEATH_TITLE))) return "deathmsg";
        return null;
    }

    private List<String> cosmeticsOptions(String category) {
        return switch (category) {
            case "kill" -> plugin.getCosmeticsManager().getAvailableKillEffects();
            case "victory" -> plugin.getCosmeticsManager().getAvailableVictoryAnimations();
            case "trail" -> plugin.getCosmeticsManager().getAvailableTrails();
            case "deathmsg" -> plugin.getCosmeticsManager().getAvailableDeathMessages();
            default -> List.of();
        };
    }

    private void applyCosmetic(Player player, String category, String option) {
        switch (category) {
            case "kill" -> {
                plugin.getCosmeticsManager().setKillEffect(player.getUniqueId(), option);
                player.sendMessage(ColorUtil.colorizePrefix("&aKill effect set to &f" + option));
            }
            case "victory" -> {
                plugin.getCosmeticsManager().setVictoryAnimation(player.getUniqueId(), option);
                player.sendMessage(ColorUtil.colorizePrefix("&aVictory animation set to &f" + option));
            }
            case "trail" -> {
                plugin.getCosmeticsManager().setTrail(player.getUniqueId(), option);
                player.sendMessage(ColorUtil.colorizePrefix("&aTrail set to &f" + option));
            }
            case "deathmsg" -> {
                plugin.getCosmeticsManager().setDeathMessage(player.getUniqueId(), option);
                player.sendMessage(ColorUtil.colorizePrefix("&aDeath message set to &f" + option));
            }
        }
    }
}
