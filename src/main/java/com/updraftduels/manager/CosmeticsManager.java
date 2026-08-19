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
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CosmeticsManager {
    private final UpdraftDuels plugin;
    private final Map<UUID, String> selectedKillEffect;
    private final Map<UUID, String> selectedVictoryAnimation;
    private final Map<UUID, String> selectedTrail;
    private final Map<UUID, String> selectedDeathMessage;
    private final File cosmeticsFile;
    private FileConfiguration cosmeticsConfig;
    private volatile boolean dirty;

    public CosmeticsManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.selectedKillEffect = new ConcurrentHashMap<>();
        this.selectedVictoryAnimation = new ConcurrentHashMap<>();
        this.selectedTrail = new ConcurrentHashMap<>();
        this.selectedDeathMessage = new ConcurrentHashMap<>();
        this.cosmeticsFile = new File(plugin.getDataFolder(), "cosmetics.yml");
        loadAll();
    }

    public void setKillEffect(UUID uuid, String effect) {
        selectedKillEffect.put(uuid, effect);
        markDirty();
    }
    public String getKillEffect(UUID uuid) { return selectedKillEffect.getOrDefault(uuid, "none"); }

    public void setVictoryAnimation(UUID uuid, String anim) {
        selectedVictoryAnimation.put(uuid, anim);
        markDirty();
    }
    public String getVictoryAnimation(UUID uuid) { return selectedVictoryAnimation.getOrDefault(uuid, "none"); }

    public void setTrail(UUID uuid, String trail) {
        selectedTrail.put(uuid, trail);
        markDirty();
    }
    public String getTrail(UUID uuid) { return selectedTrail.getOrDefault(uuid, "none"); }

    public void setDeathMessage(UUID uuid, String msg) {
        selectedDeathMessage.put(uuid, msg);
        markDirty();
    }
    public String getDeathMessage(UUID uuid) { return selectedDeathMessage.getOrDefault(uuid, "default"); }

    public List<String> getAvailableKillEffects() {
        return List.of("none", "lightning", "firework", "explosion", "soul", "blood", "electric");
    }

    public List<String> getAvailableVictoryAnimations() {
        return List.of("none", "firework_show", "lightning_rain", "fire_pillar", "ender_dragon", "fireworks_ring");
    }

    public List<String> getAvailableTrails() {
        return List.of("none", "flame", "portal", "enchant", "crit", "smoke", "heart", "snow");
    }

    public List<String> getAvailableDeathMessages() {
        return List.of("default", "humorous", "dramatic", "minimal", "competitive");
    }

    public void playKillEffect(Location location, String effect) {
        if (effect == null || effect.equals("none")) return;
        World world = location.getWorld();
        if (world == null) return;

        switch (effect.toLowerCase()) {
            case "lightning" -> world.strikeLightningEffect(location);
            case "firework" -> spawnFirework(location);
            case "explosion" -> world.createExplosion(location.getX(), location.getY(), location.getZ(), 0F, false, false);
            case "soul" -> {
                for (int i = 0; i < 20; i++) {
                    world.spawnParticle(Particle.SOUL, location.clone().add(
                            (Math.random() - 0.5) * 2,
                            Math.random() * 2,
                            (Math.random() - 0.5) * 2), 1);
                }
            }
            case "blood" -> {
                for (int i = 0; i < 30; i++) {
                    world.spawnParticle(Particle.DUST, location.clone().add(
                            (Math.random() - 0.5) * 1.5,
                            Math.random() * 1.5,
                            (Math.random() - 0.5) * 1.5), 1,
                            new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                }
            }
            case "electric" -> {
                for (int i = 0; i < 15; i++) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(
                            (Math.random() - 0.5) * 3,
                            Math.random() * 3,
                            (Math.random() - 0.5) * 3), 3);
                }
            }
        }
    }

    public void playVictoryAnimation(Location location, String animation) {
        if (animation == null || animation.equals("none")) return;
        World world = location.getWorld();
        if (world == null) return;

        switch (animation.toLowerCase()) {
            case "firework_show" -> {
                for (int i = 0; i < 5; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> spawnFirework(location), i * 10L);
                }
            }
            case "lightning_rain" -> {
                for (int i = 0; i < 3; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double x = location.getX() + (Math.random() - 0.5) * 10;
                        double z = location.getZ() + (Math.random() - 0.5) * 10;
                        world.strikeLightningEffect(new Location(world, x, location.getY(), z));
                    }, i * 15L);
                }
            }
            case "fire_pillar" -> {
                for (int i = 0; i < 20; i++) {
                    double y = i * 0.5;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        world.spawnParticle(Particle.FLAME, location.clone().add(0, y, 0), 10, 0.3, 0, 0.3, 0.05);
                    }, i * 2L);
                }
            }
            case "ender_dragon" -> {
                world.spawnParticle(Particle.DRAGON_BREATH, location.clone().add(0, 1, 0), 200, 2, 2, 2, 0.1);
                world.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 1f);
            }
            case "fireworks_ring" -> {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    double x = location.getX() + Math.cos(angle) * 5;
                    double z = location.getZ() + Math.sin(angle) * 5;
                    Location fwLoc = new Location(location.getWorld(), x, location.getY() + 3, z);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> spawnFirework(fwLoc), i * 5L);
                }
            }
        }
    }

    public void playDefeatAnimation(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        Location origin = location.clone().add(0, 1, 0);
        world.spawnParticle(Particle.SMOKE, origin, 40, 0.5, 1.0, 0.5, 0.05);
        world.spawnParticle(Particle.ASH, origin, 30, 0.4, 0.8, 0.4, 0.02);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, origin, 15, 0.3, 0.6, 0.3, 0.03);
        world.playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        world.playSound(location, Sound.ENTITY_WITHER_HURT, 0.8f, 0.6f);
        world.playSound(location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
    }

    public void startLobbyTrailTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                com.updraftduels.model.Duel duel = plugin.getDuelManager().getDuelOf(player.getUniqueId());
                if (duel != null) continue;
                String trail = getTrail(player.getUniqueId());
                if (!trail.equals("none")) {
                    playTrail(player, trail);
                }
            }
        }, 2L, 2L);
    }

    public void playTrail(Player player, String trail) {
        if (trail == null || trail.equals("none")) return;
        Location loc = player.getLocation().add(0, 0.1, 0);

        switch (trail.toLowerCase()) {
            case "flame" -> player.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.01);
            case "portal" -> player.getWorld().spawnParticle(Particle.PORTAL, loc, 5, 0.2, 0, 0.2, 0.05);
            case "enchant" -> player.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.3, 0.1, 0.3, 0.5);
            case "crit" -> player.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.1, 0, 0.1, 0.1);
            case "smoke" -> player.getWorld().spawnParticle(Particle.SMOKE, loc, 2, 0.1, 0, 0.1, 0.01);
            case "heart" -> player.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 1.5, 0), 1, 0.3, 0.3, 0.3, 0);
            case "snow" -> player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.3, 0, 0.3, 0.02);
        }
    }

    public String formatDeathMessage(String template, String killer, String victim, String cause) {
        if (template == null) return null;
        return template.replace("%killer%", killer).replace("%victim%", victim).replace("%cause%", cause);
    }

    public String getDeathMessageTemplate(String type) {
        if (type == null) type = "default";
        return switch (type.toLowerCase()) {
            case "humorous" -> "&c%killer% &7sent &f%victim% &7to the shadow realm";
            case "dramatic" -> "&4%killer% &7obliterated &f%victim%";
            case "minimal" -> "&7[&c%killer% &7killed &f%victim%&7]";
            case "competitive" -> "&c%killer% &7defeated &f%victim% &7(%cause%)";
            default -> "&c%killer% &7killed &f%victim%";
        };
    }

    private void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.YELLOW, Color.ORANGE)
                .withFade(Color.RED)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private void markDirty() { dirty = true; }

    public void saveIfDirty() {
        if (!dirty) return;
        Set<UUID> all = new HashSet<>();
        all.addAll(selectedKillEffect.keySet());
        all.addAll(selectedVictoryAnimation.keySet());
        all.addAll(selectedTrail.keySet());
        all.addAll(selectedDeathMessage.keySet());
        for (UUID uuid : all) {
            String path = uuid.toString();
            cosmeticsConfig.set(path + ".kill-effect", selectedKillEffect.getOrDefault(uuid, "none"));
            cosmeticsConfig.set(path + ".victory-animation", selectedVictoryAnimation.getOrDefault(uuid, "none"));
            cosmeticsConfig.set(path + ".trail", selectedTrail.getOrDefault(uuid, "none"));
            cosmeticsConfig.set(path + ".death-message", selectedDeathMessage.getOrDefault(uuid, "default"));
        }
        try {
            cosmeticsConfig.save(cosmeticsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cosmetics: " + e.getMessage());
        }
        dirty = false;
    }

    private void loadAll() {
        if (!cosmeticsFile.exists()) {
            cosmeticsConfig = new YamlConfiguration();
            return;
        }
        cosmeticsConfig = YamlConfiguration.loadConfiguration(cosmeticsFile);
        for (String key : cosmeticsConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                selectedKillEffect.put(uuid, cosmeticsConfig.getString(key + ".kill-effect", "none"));
                selectedVictoryAnimation.put(uuid, cosmeticsConfig.getString(key + ".victory-animation", "none"));
                selectedTrail.put(uuid, cosmeticsConfig.getString(key + ".trail", "none"));
                selectedDeathMessage.put(uuid, cosmeticsConfig.getString(key + ".death-message", "default"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
