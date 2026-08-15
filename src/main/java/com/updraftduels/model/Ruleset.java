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
package com.updraftduels.model;

import java.util.Map;

public class Ruleset {
    private final String id;
    private final String displayName;
    private final String description;
    private final boolean knockbackOnly;
    private final boolean noDamage;
    private final boolean fistsOnly;
    private final boolean naturalRegenDisabled;
    private final boolean hungerEnabled;
    private final boolean breakableFloor;
    private final boolean enderPearlsAllowed;
    private final boolean chorusFruitAllowed;
    private final Map<String, Object> customFlags;

    public Ruleset(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.knockbackOnly = builder.knockbackOnly;
        this.noDamage = builder.noDamage;
        this.fistsOnly = builder.fistsOnly;
        this.naturalRegenDisabled = builder.naturalRegenDisabled;
        this.hungerEnabled = builder.hungerEnabled;
        this.breakableFloor = builder.breakableFloor;
        this.enderPearlsAllowed = builder.enderPearlsAllowed;
        this.chorusFruitAllowed = builder.chorusFruitAllowed;
        this.customFlags = builder.customFlags;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isKnockbackOnly() { return knockbackOnly; }
    public boolean isNoDamage() { return noDamage; }
    public boolean isFistsOnly() { return fistsOnly; }
    public boolean isNaturalRegenDisabled() { return naturalRegenDisabled; }
    public boolean isHungerEnabled() { return hungerEnabled; }
    public boolean isBreakableFloor() { return breakableFloor; }
    public boolean isEnderPearlsAllowed() { return enderPearlsAllowed; }
    public boolean isChorusFruitAllowed() { return chorusFruitAllowed; }
    public Map<String, Object> getCustomFlags() { return customFlags; }

    public Object getCustomFlag(String key) {
        return customFlags != null ? customFlags.get(key) : null;
    }

    public boolean isKitRestricted() {
        return fistsOnly || knockbackOnly;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String description;
        private boolean knockbackOnly = false;
        private boolean noDamage = false;
        private boolean fistsOnly = false;
        private boolean naturalRegenDisabled = false;
        private boolean hungerEnabled = false;
        private boolean breakableFloor = false;
        private boolean enderPearlsAllowed = true;
        private boolean chorusFruitAllowed = true;
        private Map<String, Object> customFlags;

        private Builder(String id) {
            this.id = id;
        }

        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder knockbackOnly(boolean val) { this.knockbackOnly = val; return this; }
        public Builder noDamage(boolean val) { this.noDamage = val; return this; }
        public Builder fistsOnly(boolean val) { this.fistsOnly = val; return this; }
        public Builder naturalRegenDisabled(boolean val) { this.naturalRegenDisabled = val; return this; }
        public Builder hungerEnabled(boolean val) { this.hungerEnabled = val; return this; }
        public Builder breakableFloor(boolean val) { this.breakableFloor = val; return this; }
        public Builder enderPearlsAllowed(boolean val) { this.enderPearlsAllowed = val; return this; }
        public Builder chorusFruitAllowed(boolean val) { this.chorusFruitAllowed = val; return this; }
        public Builder customFlags(Map<String, Object> flags) { this.customFlags = flags; return this; }

        public Ruleset build() {
            if (displayName == null) displayName = id;
            if (description == null) description = "";
            return new Ruleset(this);
        }
    }
}
