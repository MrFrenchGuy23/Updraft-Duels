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
import com.updraftduels.model.Party;
import com.updraftduels.model.PartyDuelChallenge;
import com.updraftduels.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, Party> parties;
    private final Map<UUID, UUID> playerParties;
    private final Map<UUID, PartyDuelChallenge> pendingChallenges;
    private final Map<UUID, Boolean> partyChatToggles;

    public PartyManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.parties = new ConcurrentHashMap<>();
        this.playerParties = new ConcurrentHashMap<>();
        this.pendingChallenges = new ConcurrentHashMap<>();
        this.partyChatToggles = new ConcurrentHashMap<>();
    }

    private static final class PartyItemConfig {
        final int slot;
        final ItemStack item;
        final String command;

        PartyItemConfig(int slot, ItemStack item, String command) {
            this.slot = slot;
            this.item = item;
            this.command = command;
        }
    }

    private List<PartyItemConfig> loadPartyItemConfigs() {
        List<PartyItemConfig> configs = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("party.items");
        if (section == null) return configs;
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;
            Material material = Material.matchMaterial(itemSection.getString("material", "STONE"));
            if (material == null) material = Material.STONE;
            String name = itemSection.getString("name", "&f" + key);
            List<String> lore = itemSection.getStringList("lore");
            String command = itemSection.getString("command", "");
            int slot = itemSection.getInt("slot", -1);
            ItemStack stack = new ItemBuilder(material).name(name).lore(lore).build();
            configs.add(new PartyItemConfig(slot, stack, command));
        }
        return configs;
    }

    private boolean isMatching(PartyItemConfig config, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (stack.getType() != config.item.getType()) return false;
        ItemMeta meta = stack.getItemMeta();
        ItemMeta configMeta = config.item.getItemMeta();
        if (configMeta != null && configMeta.hasDisplayName()) {
            return meta != null && meta.hasDisplayName()
                    && meta.getDisplayName().equals(configMeta.getDisplayName());
        }
        return true;
    }

    public void givePartyItems(Player player) {
        if (player == null) return;
        for (PartyItemConfig config : loadPartyItemConfigs()) {
            if (config.slot >= 0 && config.slot < 36) {
                player.getInventory().setItem(config.slot, config.item.clone());
            } else {
                player.getInventory().addItem(config.item.clone());
            }
        }
    }

    public void removePartyItems(Player player) {
        if (player == null) return;
        for (PartyItemConfig config : loadPartyItemConfigs()) {
            if (config.slot >= 0 && config.slot < 36) {
                ItemStack current = player.getInventory().getItem(config.slot);
                if (isMatching(config, current)) {
                    player.getInventory().setItem(config.slot, null);
                }
            } else {
                for (ItemStack stack : player.getInventory().getContents()) {
                    if (isMatching(config, stack)) {
                        player.getInventory().removeItem(stack);
                        break;
                    }
                }
            }
        }
    }

    public boolean isPartyItem(ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return false;
        for (PartyItemConfig config : loadPartyItemConfigs()) {
            if (isMatching(config, clicked)) return true;
        }
        return false;
    }

    public void executePartyItem(Player player, ItemStack clicked) {
        for (PartyItemConfig config : loadPartyItemConfigs()) {
            if (isMatching(config, clicked)) {
                if (!config.command.isEmpty()) {
                    player.performCommand(config.command);
                }
                return;
            }
        }
    }

    public Party createParty(UUID leaderUUID) {
        if (getParty(leaderUUID) != null) return null;
        Party party = new Party(leaderUUID);
        parties.put(party.getPartyId(), party);
        playerParties.put(leaderUUID, party.getPartyId());
        givePartyItems(Bukkit.getPlayer(leaderUUID));
        return party;
    }

    public boolean inviteToParty(UUID inviterUUID, UUID inviteeUUID) {
        Party party = getParty(inviterUUID);
        if (party == null) return false;
        if (!party.isLeader(inviterUUID)) return false;
        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        if (party.isFull(maxSize)) return false;
        if (getParty(inviteeUUID) != null) return false;
        party.addInvitee(inviteeUUID);
        return true;
    }

    public boolean acceptInvite(UUID inviteeUUID) {
        if (getParty(inviteeUUID) != null) return false;
        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        for (Party party : parties.values()) {
            if (party.isInvited(inviteeUUID)) {
                if (party.isFull(maxSize)) return false;
                party.removeInvitee(inviteeUUID);
                party.addMember(inviteeUUID);
                playerParties.put(inviteeUUID, party.getPartyId());
                return true;
            }
        }
        return false;
    }

    public boolean leaveParty(UUID uuid) {
        UUID partyId = playerParties.get(uuid);
        if (partyId == null) return false;
        Party party = parties.get(partyId);
        if (party == null) return false;

        party.removeMember(uuid);
        playerParties.remove(uuid);
        disablePartyChat(uuid);

        if (party.getMembers().isEmpty()) {
            parties.remove(partyId);
            return true;
        }

        if (party.isLeader(uuid)) {
            party.setLeaderUUID(party.getMembers().get(0));
            broadcastToParty(party, plugin.getMessages().get("party.leader-changed",
                    "%player%", Bukkit.getOfflinePlayer(party.getLeaderUUID()).getName()));
            givePartyItems(Bukkit.getPlayer(party.getLeaderUUID()));
        }

        removePartyItems(Bukkit.getPlayer(uuid));

        String leaveName = Bukkit.getOfflinePlayer(uuid).getName();
        broadcastToParty(party, plugin.getMessages().get("party.member-left", "%player%", leaveName != null ? leaveName : "Unknown"));

        return true;
    }

    public boolean disbandParty(UUID leaderUUID) {
        Party party = getParty(leaderUUID);
        if (party == null || !party.isLeader(leaderUUID)) return false;

        broadcastToParty(party, plugin.getMessages().get("party.disbanded"));

        for (UUID member : new ArrayList<>(party.getMembers())) {
            playerParties.remove(member);
            disablePartyChat(member);
            removePartyItems(Bukkit.getPlayer(member));
        }
        parties.remove(party.getPartyId());
        return true;
    }

    public boolean kickMember(UUID leaderUUID, UUID targetUUID) {
        Party party = getParty(leaderUUID);
        if (party == null || !party.isLeader(leaderUUID)) return false;
        if (targetUUID.equals(leaderUUID)) return false;
        if (!party.isMember(targetUUID)) return false;

        party.removeMember(targetUUID);
        playerParties.remove(targetUUID);
        disablePartyChat(targetUUID);

        String kickedName = Bukkit.getOfflinePlayer(targetUUID).getName();
        broadcastToParty(party, plugin.getMessages().get("party.kicked", "%player%", kickedName != null ? kickedName : "Unknown"));

        Player kickedPlayer = Bukkit.getPlayer(targetUUID);
        if (kickedPlayer != null) {
            kickedPlayer.sendMessage(com.updraftduels.util.ColorUtil.colorize(plugin.getMessages().get("party.kicked")));
            removePartyItems(kickedPlayer);
        }

        return true;
    }

    public Party getParty(UUID uuid) {
        UUID partyId = playerParties.get(uuid);
        return partyId != null ? parties.get(partyId) : null;
    }

    public Party getPartyById(UUID partyId) {
        return parties.get(partyId);
    }

    public boolean isInParty(UUID uuid) {
        return playerParties.containsKey(uuid);
    }

    public boolean isPartyInFight(Party party) {
        for (UUID member : party.getMembers()) {
            if (plugin.getDuelManager().getDuelOf(member) != null) return true;
        }
        return false;
    }

    public boolean isPartyChatEnabled(UUID uuid) {
        return partyChatToggles.getOrDefault(uuid, false);
    }

    public void setPartyChat(UUID uuid, boolean enabled) {
        partyChatToggles.put(uuid, enabled);
    }

    public void disablePartyChat(UUID uuid) {
        partyChatToggles.remove(uuid);
    }

    public boolean createChallenge(UUID challengerLeaderUUID, UUID defenderLeaderUUID, String arenaName, String rulesetId) {
        Party challenger = getParty(challengerLeaderUUID);
        Party defender = getParty(defenderLeaderUUID);
        if (challenger == null || defender == null) return false;
        if (!challenger.isLeader(challengerLeaderUUID)) return false;
        if (challenger.getSize() != defender.getSize()) return false;
        if (isPartyInFight(challenger) || isPartyInFight(defender)) return false;

        for (PartyDuelChallenge existing : pendingChallenges.values()) {
            if (existing.isProcessed() || existing.isExpired()) continue;
            boolean samePair = (existing.getChallengerPartyId().equals(challenger.getPartyId())
                    && existing.getDefenderPartyId().equals(defender.getPartyId()))
                    || (existing.getChallengerPartyId().equals(defender.getPartyId())
                    && existing.getDefenderPartyId().equals(challenger.getPartyId()));
            if (samePair) return false;
        }

        PartyDuelChallenge challenge = new PartyDuelChallenge(
                challenger.getPartyId(), defender.getPartyId(), arenaName, rulesetId);
        pendingChallenges.put(challenge.getChallengeId(), challenge);
        return true;
    }

    public PartyDuelChallenge getPendingChallengeFor(UUID playerUUID) {
        Party party = getParty(playerUUID);
        if (party == null) return null;
        for (PartyDuelChallenge challenge : pendingChallenges.values()) {
            if (challenge.isProcessed() || challenge.isExpired()) continue;
            if (challenge.getDefenderPartyId().equals(party.getPartyId())) {
                return challenge;
            }
        }
        return null;
    }

    public boolean acceptChallenge(UUID challengeId, UUID acceptorUUID) {
        PartyDuelChallenge challenge = pendingChallenges.get(challengeId);
        if (challenge == null || challenge.isProcessed() || challenge.isExpired()) return false;

        Party defender = getPartyById(challenge.getDefenderPartyId());
        if (defender == null || !defender.isLeader(acceptorUUID)) return false;

        Party challenger = getPartyById(challenge.getChallengerPartyId());
        if (challenger == null) return false;

        com.updraftduels.model.Arena arena = plugin.getArenaManager().getArena(challenge.getArenaName());
        if (arena == null || arena.isInUse()) return false;

        if (isPartyInFight(challenger) || isPartyInFight(defender)) return false;

        challenge.setProcessed(true);
        pendingChallenges.remove(challengeId);

        boolean started = plugin.getDuelManager().startPartyDuel(
                challenger, defender, arena, challenge.getRulesetId(), 1);
        if (!started) {
            broadcastToParty(challenger, plugin.getMessages().get("party.duel-failed"));
            broadcastToParty(defender, plugin.getMessages().get("party.duel-failed"));
        }
        return started;
    }

    public boolean declineChallenge(UUID challengeId, UUID declinerUUID) {
        PartyDuelChallenge challenge = pendingChallenges.get(challengeId);
        if (challenge == null || challenge.isProcessed()) return false;
        challenge.setProcessed(true);
        pendingChallenges.remove(challengeId);
        return true;
    }

    public boolean isReadyCheckComplete(UUID partyUUID) {
        Party party = parties.get(partyUUID);
        return party != null && party.isReadyCheckComplete();
    }

    public Map<UUID, Party> getAllParties() {
        return parties;
    }

    public void broadcastToParty(Party party, String message) {
        String colored = com.updraftduels.util.ColorUtil.colorize(message);
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(colored);
            }
        }
        for (UUID uuid : party.getInvitees()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(colored);
            }
        }
    }

    public void onPlayerDisconnect(UUID uuid) {
        partyChatToggles.remove(uuid);
    }
}
