/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PublicKit;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;

public class API {
    private static API instance;

    private API() {
        instance = this;
    }

    public static API getInstance() {
        if (instance == null) {
            instance = new API();
        }
        return instance;
    }

    public List<PublicKit> getPublicKits() {
        List<PublicKit> originalList = KitManager.get().getPublicKitList();
        return new ArrayList<PublicKit>(originalList);
    }

    public void loadPublicKit(Player player, PublicKit kit) {
        KitManager.get().loadPublicKitSilent(player, kit.id);
    }
}
