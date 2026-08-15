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
import com.updraftduels.model.Ruleset;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RulesetManager {
    private final UpdraftDuels plugin;
    private final Map<String, Ruleset> rulesets;

    public RulesetManager(UpdraftDuels plugin) {
        this.plugin = plugin;
        this.rulesets = new ConcurrentHashMap<>();
        registerDefaultRulesets();
        loadCustomRulesets();
    }

    private void registerDefaultRulesets() {
        rulesets.put("default", Ruleset.builder("default")
                .displayName("Default")
                .description("Standard duel rules")
                .build());

        rulesets.put("sumo", Ruleset.builder("sumo")
                .displayName("Sumo")
                .description("Knockback only, push your opponent off!")
                .knockbackOnly(true)
                .noDamage(true)
                .enderPearlsAllowed(false)
                .chorusFruitAllowed(false)
                .build());

        rulesets.put("boxing", Ruleset.builder("boxing")
                .displayName("Boxing")
                .description("Fists only, no weapons!")
                .fistsOnly(true)
                .build());

        rulesets.put("uhc", Ruleset.builder("uhc")
                .displayName("UHC")
                .description("No natural regeneration, hunger enabled!")
                .naturalRegenDisabled(true)
                .hungerEnabled(true)
                .build());

        rulesets.put("spleef", Ruleset.builder("spleef")
                .displayName("Spleef")
                .description("Break the floor to make opponents fall!")
                .breakableFloor(true)
                .build());

        rulesets.put("buildpvp", Ruleset.builder("buildpvp")
                .displayName("Build PvP")
                .description("Place and break blocks during combat!")
                .build());

        rulesets.put("uhcmeetup", Ruleset.builder("uhcmeetup")
                .displayName("UHC Meetup")
                .description("No regen, hunger enabled, borders shrink!")
                .naturalRegenDisabled(true)
                .hungerEnabled(true)
                .enderPearlsAllowed(false)
                .chorusFruitAllowed(false)
                .build());

        rulesets.put("archery", Ruleset.builder("archery")
                .displayName("Archery")
                .description("Bow-only combat, no melee!")
                .fistsOnly(true)
                .enderPearlsAllowed(false)
                .chorusFruitAllowed(false)
                .build());

        rulesets.put("skywars", Ruleset.builder("skywars")
                .displayName("SkyWars")
                .description("Break blocks, no natural regen!")
                .breakableFloor(true)
                .naturalRegenDisabled(true)
                .build());

        rulesets.put("combo", Ruleset.builder("combo")
                .displayName("Combo")
                .description("No knockback, combo your opponent!")
                .noDamage(false)
                .enderPearlsAllowed(false)
                .chorusFruitAllowed(false)
                .build());
    }

    private void loadCustomRulesets() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rulesets");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rs = section.getConfigurationSection(key);
            if (rs == null) continue;

            if (rulesets.containsKey(key)) continue;

            Ruleset.Builder builder = Ruleset.builder(key)
                    .displayName(rs.getString("display-name", key))
                    .description(rs.getString("description", ""))
                    .knockbackOnly(rs.getBoolean("knockback-only", false))
                    .noDamage(rs.getBoolean("no-damage", false))
                    .fistsOnly(rs.getBoolean("fists-only", false))
                    .naturalRegenDisabled(rs.getBoolean("natural-regen-disabled", false))
                    .hungerEnabled(rs.getBoolean("hunger-enabled", false))
                    .breakableFloor(rs.getBoolean("breakable-floor", false))
                    .enderPearlsAllowed(rs.getBoolean("ender-pearls-allowed", true))
                    .chorusFruitAllowed(rs.getBoolean("chorus-fruit-allowed", true));

            rulesets.put(key, builder.build());
        }
    }

    public Ruleset getRuleset(String id) {
        return rulesets.getOrDefault(id, rulesets.get("default"));
    }

    public Collection<Ruleset> getAllRulesets() {
        return rulesets.values();
    }

    public void registerRuleset(Ruleset ruleset) {
        rulesets.put(ruleset.getId(), ruleset);
    }

    public void reload() {
        for (Iterator<Map.Entry<String, Ruleset>> it = rulesets.entrySet().iterator(); it.hasNext(); ) {
            if (!isDefaultId(it.next().getKey())) it.remove();
        }
        loadCustomRulesets();
    }

    private boolean isDefaultId(String id) {
        return id.equals("default") || id.equals("sumo") || id.equals("boxing") || id.equals("uhc")
                || id.equals("spleef") || id.equals("buildpvp") || id.equals("uhcmeetup")
                || id.equals("archery") || id.equals("skywars") || id.equals("combo");
    }

    public boolean hasRuleset(String id) {
        return rulesets.containsKey(id);
    }
}
