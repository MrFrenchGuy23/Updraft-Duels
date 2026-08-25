package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrainCommand implements CommandExecutor {
    private final UpdraftDuels plugin;
    private final Set<UUID> trainingPlayers = new HashSet<>();

    public TrainCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (plugin.getDuelManager().isInDuel(uuid)) {
            player.sendMessage(plugin.getMessages().get("train.in-duel"));
            return true;
        }

        if (trainingPlayers.contains(uuid)) {
            trainingPlayers.remove(uuid);
            player.sendMessage(plugin.getMessages().get("train.disabled"));
            return true;
        }

        String worldName = plugin.getConfig().getString("train-arena.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(plugin.getMessages().get("train.no-arena"));
            return true;
        }

        double x = plugin.getConfig().getDouble("train-arena.spawn-x", 0);
        double y = plugin.getConfig().getDouble("train-arena.spawn-y", 64);
        double z = plugin.getConfig().getDouble("train-arena.spawn-z", 0);

        player.teleport(new Location(world, x, y, z));
        trainingPlayers.add(uuid);
        player.sendMessage(plugin.getMessages().get("train.enabled"));
        return true;
    }

    public boolean isTraining(UUID uuid) {
        return trainingPlayers.contains(uuid);
    }

    public void removePlayer(UUID uuid) {
        trainingPlayers.remove(uuid);
    }
}
