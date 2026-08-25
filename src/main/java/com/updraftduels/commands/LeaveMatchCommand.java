package com.updraftduels.commands;

import com.updraftduels.UpdraftDuels;
import com.updraftduels.model.Duel;
import com.updraftduels.model.DuelState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveMatchCommand implements CommandExecutor {
    private final UpdraftDuels plugin;

    public LeaveMatchCommand(UpdraftDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
        if (duel == null) {
            player.sendMessage(plugin.getMessages().get("leavematch.not-in-duel"));
            return true;
        }

        if (duel.getState() == DuelState.FINISHED || duel.getState() == DuelState.CANCELLED) {
            player.sendMessage(plugin.getMessages().get("leavematch.not-in-duel"));
            return true;
        }

        for (java.util.UUID uuid : duel.getAllParticipants()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player opponent = Bukkit.getPlayer(uuid);
            if (opponent != null) {
                opponent.sendMessage(plugin.getMessages().get("leavematch.opponent-left", "%player%", player.getName()));
            }
        }

        plugin.getDuelManager().cancelDuel(duel);
        player.sendMessage(plugin.getMessages().get("leavematch.left"));
        return true;
    }
}
