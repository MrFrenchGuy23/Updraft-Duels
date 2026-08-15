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
import com.updraftduels.model.Arena;
import com.updraftduels.model.FFAGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FFAManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, FFAGame> games;
    private final Map<UUID, UUID> playerGames;
    private final Map<UUID, FFAStateSnapshot> savedStates;

    public FFAManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.games = new ConcurrentHashMap<>();
        this.playerGames = new ConcurrentHashMap<>();
        this.savedStates = new ConcurrentHashMap<>();
    }

    public FFAGame createGame(String name, String arenaName, int maxPlayers, UUID creatorUUID) {
        for (FFAGame g : games.values()) {
            if (g.getName().equalsIgnoreCase(name)) return null;
        }
        FFAGame game = new FFAGame(name, arenaName, maxPlayers, creatorUUID);
        games.put(game.getId(), game);
        return game;
    }

    public boolean joinGame(UUID gameId, UUID playerUUID) {
        FFAGame game = games.get(gameId);
        if (game == null || game.getState() != FFAGame.State.WAITING) return false;
        if (game.getAlive().size() >= game.getMaxPlayers()) return false;
        if (playerGames.containsKey(playerUUID)) return false;

        if (!game.addParticipant(playerUUID)) return false;
        playerGames.put(playerUUID, gameId);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            saveState(player);
            Arena arena = plugin.getArenaManager().getArena(game.getArenaName());
            if (arena != null && arena.getCenter() != null) {
                player.teleport(arena.getCenter());
            }
            player.getInventory().clear();
        }

        if (game.getAlive().size() >= 2 && game.getState() == FFAGame.State.WAITING) {
            game.setState(FFAGame.State.IN_PROGRESS);
            broadcastToGame(game, "&aFFA &f" + game.getName() + " &astarted.");
        }

        return true;
    }

    public boolean leaveGame(UUID playerUUID) {
        UUID gameId = playerGames.remove(playerUUID);
        if (gameId == null) return false;

        FFAGame game = games.get(gameId);
        if (game == null) return false;

        game.removeParticipant(playerUUID);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            restoreState(player);
        }

        checkWinCondition(game);
        return true;
    }

    public void onPlayerDeath(UUID playerUUID) {
        UUID gameId = playerGames.get(playerUUID);
        if (gameId == null) return;

        FFAGame game = games.get(gameId);
        if (game == null || game.getState() != FFAGame.State.IN_PROGRESS) return;

        game.eliminate(playerUUID);
        playerGames.remove(playerUUID);

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            FFAStateSnapshot state = savedStates.get(playerUUID);
            if (state != null && state.location != null) {
                player.setRespawnLocation(state.location);
            }
            restoreState(player);
        }

        checkWinCondition(game);
    }

    public void onPlayerDisconnect(UUID playerUUID) {
        UUID gameId = playerGames.remove(playerUUID);
        if (gameId == null) return;

        FFAGame game = games.get(gameId);
        if (game != null) {
            game.removeParticipant(playerUUID);
            checkWinCondition(game);
        }
    }

    public void restoreSavedState(Player player) {
        restoreState(player);
    }

    private void checkWinCondition(FFAGame game) {
        if (game.getState() != FFAGame.State.IN_PROGRESS) return;
        if (game.getAlive().size() <= 1) {
            UUID winner = game.getAlive().isEmpty() ? null : game.getAlive().get(0);
            game.setState(FFAGame.State.FINISHED);
            game.setWinner(winner);

            String winnerName = winner != null ? Bukkit.getOfflinePlayer(winner).getName() : "Nobody";
            broadcastToGame(game, "&6FFA &f" + game.getName() + " &6winner: &f" + winnerName);

            if (winner != null) {
                Player winnerPlayer = Bukkit.getPlayer(winner);
                String winnerHP = winnerPlayer != null
                        ? String.valueOf((int) Math.ceil(Math.max(0, Math.min(winnerPlayer.getHealth(), 20))))
                        : "?";
                broadcastToGame(game, "&7Winner: &f" + winnerName + " &7(&c♥ " + winnerHP + "&7)");
            } else {
                broadcastToGame(game, "&7The game ended in a draw.");
            }

            if (winner != null) {
                Player winnerPlayer = Bukkit.getPlayer(winner);
                if (winnerPlayer != null) {
                    plugin.getCosmeticsManager().playVictoryAnimation(winnerPlayer.getLocation(),
                            plugin.getCosmeticsManager().getVictoryAnimation(winner));
                    winnerPlayer.playSound(winnerPlayer.getLocation(),
                            org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                }
            }

            for (UUID uuid : game.getAllParticipants()) {
                playerGames.remove(uuid);
                if (winner == null || !uuid.equals(winner)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        plugin.getCosmeticsManager().playDefeatAnimation(p.getLocation());
                    }
                }
            }
        }
    }

    public void broadcastToGame(FFAGame game, String message) {
        for (UUID uuid : game.getAllParticipants()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(com.updraftduels.util.ColorUtil.colorizePrefix(message));
            }
        }
    }

    public FFAGame getGame(UUID id) { return games.get(id); }
    public FFAGame getGameOf(UUID playerUUID) {
        UUID gameId = playerGames.get(playerUUID);
        return gameId != null ? games.get(gameId) : null;
    }
    public Collection<FFAGame> getAllGames() { return games.values(); }
    public List<FFAGame> getOpenGames() {
        return games.values().stream().filter(g -> g.getState() == FFAGame.State.WAITING).toList();
    }
    public boolean isInFFA(UUID uuid) { return playerGames.containsKey(uuid); }
    public boolean deleteGame(String name) {
        for (FFAGame game : games.values()) {
            if (game.getName().equalsIgnoreCase(name)) {
                for (UUID uuid : game.getAllParticipants()) {
                    playerGames.remove(uuid);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        restoreState(p);
                    }
                }
                games.remove(game.getId());
                return true;
            }
        }
        return false;
    }

    private void saveState(Player player) {
        if (savedStates.containsKey(player.getUniqueId())) return;
        FFAStateSnapshot state = new FFAStateSnapshot();
        state.location = player.getLocation().clone();
        state.contents = player.getInventory().getContents();
        state.armor = player.getInventory().getArmorContents();
        state.offHand = player.getInventory().getItemInOffHand().clone();
        state.enderChest = player.getEnderChest().getContents();
        state.health = player.getHealth();
        state.food = player.getFoodLevel();
        savedStates.put(player.getUniqueId(), state);
    }

    private void restoreState(Player player) {
        FFAStateSnapshot state = savedStates.remove(player.getUniqueId());
        if (state == null) return;

        if (state.location != null && !player.isDead()) {
            player.teleport(state.location);
        }
        if (state.contents != null) {
            player.getInventory().setContents(state.contents.clone());
        }
        if (state.armor != null) {
            player.getInventory().setArmorContents(state.armor.clone());
        }
        if (state.offHand != null) {
            player.getInventory().setItemInOffHand(state.offHand.clone());
        }
        if (state.enderChest != null) {
            player.getEnderChest().setContents(state.enderChest.clone());
        }
        player.setHealth(Math.min(state.health, 20.0));
        player.setFoodLevel(state.food);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
    }

    private static class FFAStateSnapshot {
        org.bukkit.Location location;
        org.bukkit.inventory.ItemStack[] contents;
        org.bukkit.inventory.ItemStack[] armor;
        org.bukkit.inventory.ItemStack offHand;
        org.bukkit.inventory.ItemStack[] enderChest;
        double health;
        int food;
    }
}
