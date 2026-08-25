/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.noah.perplayerkit.gui;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.PlayerUtil;
import dev.noah.perplayerkit.util.SoundManager;
import dev.noah.perplayerkit.util.StyleManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.ClickOptions;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.type.ChestMenu;

public class GUI {
    private final Plugin plugin;
    private final boolean filterItemsOnImport;
    private static final Set<UUID> kitDeletionFlag = new HashSet<UUID>();

    public GUI(Plugin plugin) {
        this.plugin = plugin;
        this.filterItemsOnImport = plugin.getConfig().getBoolean("anti-exploit.import-filter", false);
    }

    public static void addLoadPublicKit(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            KitManager.get().loadPublicKit(player, id);
            info.getClickedMenu().close();
        });
    }

    public static Menu createPublicKitMenu() {
        return ((ChestMenu.Builder)((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Public Kit Room")).redraw(true)).build();
    }

    public static boolean removeKitDeletionFlag(Player player) {
        return kitDeletionFlag.remove(player.getUniqueId());
    }

    public void OpenKitMenu(Player p, int slot) {
        int i;
        Menu menu = this.createKitMenu(slot);
        if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot);
            for (int i2 = 0; i2 < 41; ++i2) {
                menu.getSlot(i2).setItem(kit[i2]);
            }
        }
        for (i = 0; i < 41; ++i) {
            this.allowModification(menu.getSlot(i));
        }
        for (i = 41; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        menu.getSlot(45).setItem(ItemUtil.createItem(Material.CHAINMAIL_BOOTS, 1, "<gray><b>BOOTS</b></gray>"));
        menu.getSlot(46).setItem(ItemUtil.createItem(Material.CHAINMAIL_LEGGINGS, 1, "<gray><b>LEGGINGS</b></gray>"));
        menu.getSlot(47).setItem(ItemUtil.createItem(Material.CHAINMAIL_CHESTPLATE, 1, "<gray><b>CHESTPLATE</b></gray>"));
        menu.getSlot(48).setItem(ItemUtil.createItem(Material.CHAINMAIL_HELMET, 1, "<gray><b>HELMET</b></gray>"));
        menu.getSlot(49).setItem(ItemUtil.createItem(Material.SHIELD, 1, "<gray><b>OFFHAND</b></gray>"));
        menu.getSlot(51).setItem(ItemUtil.createItem(Material.CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>\u25cf Import from inventory</gray>"));
        menu.getSlot(52).setItem(ItemUtil.createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>\u25cf Shift click to clear</gray>"));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        this.addMainButton(menu.getSlot(53));
        this.addClear(menu.getSlot(52));
        this.addImport(menu.getSlot(51));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenPublicKitEditor(Player p, String kitId) {
        int i;
        Menu menu = this.createPublicKitMenu(kitId);
        if (KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId)) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId));
            for (int i2 = 0; i2 < 41; ++i2) {
                menu.getSlot(i2).setItem(kit[i2]);
            }
        }
        for (i = 0; i < 41; ++i) {
            this.allowModification(menu.getSlot(i));
        }
        for (i = 41; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        menu.getSlot(45).setItem(ItemUtil.createItem(Material.CHAINMAIL_BOOTS, 1, "<gray><b>BOOTS</b></gray>"));
        menu.getSlot(46).setItem(ItemUtil.createItem(Material.CHAINMAIL_LEGGINGS, 1, "<gray><b>LEGGINGS</b></gray>"));
        menu.getSlot(47).setItem(ItemUtil.createItem(Material.CHAINMAIL_CHESTPLATE, 1, "<gray><b>CHESTPLATE</b></gray>"));
        menu.getSlot(48).setItem(ItemUtil.createItem(Material.CHAINMAIL_HELMET, 1, "<gray><b>HELMET</b></gray>"));
        menu.getSlot(49).setItem(ItemUtil.createItem(Material.SHIELD, 1, "<gray><b>OFFHAND</b></gray>"));
        menu.getSlot(51).setItem(ItemUtil.createItem(Material.CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>\u25cf Import from inventory</gray>"));
        menu.getSlot(52).setItem(ItemUtil.createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>\u25cf Shift click to clear</gray>"));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        this.addMainButton(menu.getSlot(53));
        this.addClear(menu.getSlot(52));
        this.addImport(menu.getSlot(51));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenECKitKenu(Player p, int slot) {
        int i;
        Menu menu = this.createECMenu(slot);
        for (i = 0; i < 9; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        for (i = 36; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        if (KitManager.get().getItemStackArrayById(String.valueOf(p.getUniqueId()) + "ec" + slot) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(String.valueOf(p.getUniqueId()) + "ec" + slot);
            for (int i2 = 9; i2 < 36; ++i2) {
                menu.getSlot(i2).setItem(kit[i2 - 9]);
            }
        }
        for (int i3 = 9; i3 < 36; ++i3) {
            this.allowModification(menu.getSlot(i3));
        }
        menu.getSlot(51).setItem(ItemUtil.createItem(Material.ENDER_CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>\u25cf Import from enderchest</gray>"));
        menu.getSlot(52).setItem(ItemUtil.createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>\u25cf Shift click to clear</gray>"));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        this.addMainButton(menu.getSlot(53));
        this.addClear(menu.getSlot(52), 9, 36);
        this.addImportEC(menu.getSlot(51));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void InspectKit(Player p, UUID target, int slot) {
        int i;
        String playerName = this.getPlayerName(target);
        Menu menu = this.createInspectMenu(slot, playerName);
        if (KitManager.get().hasKit(target, slot)) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(target.toString() + slot);
            for (int i2 = 0; i2 < 41; ++i2) {
                menu.getSlot(i2).setItem(kit[i2]);
            }
        }
        for (i = 41; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        menu.getSlot(45).setItem(ItemUtil.createItem(Material.CHAINMAIL_BOOTS, 1, "<gray><b>BOOTS</b></gray>"));
        menu.getSlot(46).setItem(ItemUtil.createItem(Material.CHAINMAIL_LEGGINGS, 1, "<gray><b>LEGGINGS</b></gray>"));
        menu.getSlot(47).setItem(ItemUtil.createItem(Material.CHAINMAIL_CHESTPLATE, 1, "<gray><b>CHESTPLATE</b></gray>"));
        menu.getSlot(48).setItem(ItemUtil.createItem(Material.CHAINMAIL_HELMET, 1, "<gray><b>HELMET</b></gray>"));
        menu.getSlot(49).setItem(ItemUtil.createItem(Material.SHIELD, 1, "<gray><b>OFFHAND</b></gray>"));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>CLOSE</b></red>"));
        menu.getSlot(53).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });
        if (p.hasPermission("perplayerkit.admin")) {
            for (i = 0; i < 41; ++i) {
                this.allowModification(menu.getSlot(i));
            }
            menu.getSlot(52).setItem(ItemUtil.createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>\u25cf Shift click to delete kit</gray>"));
            this.addClearKit(menu.getSlot(52), target, slot);
        }
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        SoundManager.playOpenGui(p);
    }

    public void InspectEc(Player p, UUID target, int slot) {
        int i;
        String playerName = this.getPlayerName(target);
        Menu menu = this.createInspectEcMenu(slot, playerName);
        for (i = 0; i < 9; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        for (i = 36; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        if (KitManager.get().getItemStackArrayById(String.valueOf(p.getUniqueId()) + "ec" + slot) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(String.valueOf(p.getUniqueId()) + "ec" + slot);
            for (int i2 = 9; i2 < 36; ++i2) {
                menu.getSlot(i2).setItem(kit[i2 - 9]);
            }
        }
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>CLOSE</b></red>"));
        menu.getSlot(53).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });
        if (p.hasPermission("perplayerkit.admin")) {
            for (int i3 = 9; i3 < 36; ++i3) {
                this.allowModification(menu.getSlot(i3));
            }
            menu.getSlot(52).setItem(ItemUtil.createItem(Material.BARRIER, 1, "<red><b>CLEAR ENDERCHEST</b></red>", "<gray>\u25cf Shift click to delete enderchest</gray>"));
            this.addClearEnderchest(menu.getSlot(52), target, slot);
        }
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        SoundManager.playOpenGui(p);
    }

    public void OpenMainMenu(Player p) {
        int i;
        Menu menu = this.createMainMenu(p);
        for (i = 0; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        for (i = 9; i < 18; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.CHEST, 1, "<dark_aqua><b>Kit " + (i - 8) + "</b></dark_aqua>", "<gray>\u25cf Left click to load kit</gray>", "<gray>\u25cf Right click to edit kit</gray>"));
            this.addEditLoad(menu.getSlot(i), i - 8);
        }
        for (i = 18; i < 27; ++i) {
            if (KitManager.get().getItemStackArrayById(String.valueOf(p.getUniqueId()) + "ec" + (i - 17)) != null) {
                menu.getSlot(i).setItem(ItemUtil.createItem(Material.ENDER_CHEST, 1, "<dark_aqua><b>Enderchest " + (i - 17) + "</b></dark_aqua>", "<gray>\u25cf Left click to load kit</gray>", "<gray>\u25cf Right click to edit kit</gray>"));
                this.addEditLoadEC(menu.getSlot(i), i - 17);
                continue;
            }
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.ENDER_EYE, 1, "<dark_aqua><b>Enderchest " + (i - 17) + "</b></dark_aqua>", "<gray>\u25cf Click to create</gray>"));
            this.addEditEC(menu.getSlot(i), i - 17);
        }
        for (i = 27; i < 36; ++i) {
            if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + (i - 26)) != null) {
                menu.getSlot(i).setItem(ItemUtil.createItem(Material.KNOWLEDGE_BOOK, 1, "<green><b>KIT EXISTS</b></green>", "<gray>\u25cf Click to edit</gray>"));
            } else {
                menu.getSlot(i).setItem(ItemUtil.createItem(Material.BOOK, 1, "<red><b>KIT NOT FOUND</b></red>", "<gray>\u25cf Click to create</gray>"));
            }
            this.addEdit(menu.getSlot(i), i - 26);
        }
        for (i = 37; i < 44; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        menu.getSlot(37).setItem(ItemUtil.createItem(Material.NETHER_STAR, 1, "<green><b>KIT ROOM</b></green>"));
        menu.getSlot(38).setItem(ItemUtil.createItem(Material.BOOKSHELF, 1, "<yellow><b>PREMADE KITS</b></yellow>"));
        menu.getSlot(39).setItem(ItemUtil.createItem(Material.OAK_SIGN, 1, "<green><b>INFO</b></green>", "<gray>\u25cf Click a kit slot to load your kit</gray>", "<gray>\u25cf Right click or click the book to edit</gray>", "<gray>\u25cf Share kits with /sharekit <slot></gray>"));
        menu.getSlot(41).setItem(ItemUtil.createItem(Material.REDSTONE_BLOCK, 1, "<red><b>CLEAR INVENTORY</b></red>", "<gray>\u25cf Shift click</gray>"));
        menu.getSlot(42).setItem(ItemUtil.createItem(Material.COMPASS, 1, "<green><b>SHARE KITS</b></green>", "<gray>\u25cf /sharekit <slot></gray>"));
        menu.getSlot(43).setItem(ItemUtil.createItem(Material.EXPERIENCE_BOTTLE, 1, "<green><b>REPAIR ITEMS</b></green>"));
        this.addRepairButton(menu.getSlot(43));
        this.addKitRoom(menu.getSlot(37));
        this.addPublicKitMenu(menu.getSlot(38));
        this.addClearButton(menu.getSlot(41));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenKitRoom(Player p) {
        this.OpenKitRoom(p, 0);
    }

    public void OpenKitRoom(Player p, int page) {
        int i;
        Menu menu = this.createKitRoom();
        for (i = 0; i < 45; ++i) {
            this.allowModification(menu.getSlot(i));
        }
        for (i = 45; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        if (KitRoomDataManager.get().getKitRoomPage(page) != null) {
            for (i = 0; i < 45; ++i) {
                menu.getSlot(i).setItem(KitRoomDataManager.get().getKitRoomPage(page)[i]);
            }
        }
        menu.getSlot(45).setItem(ItemUtil.createItem(Material.BEACON, 1, "<dark_aqua><b>REFILL</b></dark_aqua>"));
        this.addKitRoom(menu.getSlot(45), page);
        if (!p.hasPermission("perplayerkit.editkitroom")) {
            menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
            this.addMainButton(menu.getSlot(53));
        } else {
            menu.getSlot(53).setItem(ItemUtil.createItem(Material.BARRIER, page + 1, "<red><b>EDIT MENU</b></red>", "<red>SHIFT RIGHT CLICK TO SAVE</red>"));
        }
        this.addKitRoom(menu.getSlot(47), 0);
        this.addKitRoom(menu.getSlot(48), 1);
        this.addKitRoom(menu.getSlot(49), 2);
        this.addKitRoom(menu.getSlot(50), 3);
        this.addKitRoom(menu.getSlot(51), 4);
        for (i = 1; i < 6; ++i) {
            menu.getSlot(46 + i).setItem(ItemUtil.addHideFlags(ItemUtil.createItem(Material.valueOf((String)this.plugin.getConfig().getString("kitroom.items." + i + ".material")), "<reset>" + this.plugin.getConfig().getString("kitroom.items." + i + ".name"))));
        }
        menu.getSlot(page + 47).setItem(ItemUtil.addEnchantLook(menu.getSlot(page + 47).getItem(p)));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public Menu ViewPublicKitMenu(Player p, String id) {
        int i;
        ItemStack[] kit = KitManager.get().getPublicKit(id);
        if (kit == null) {
            p.sendMessage(String.valueOf(ChatColor.RED) + "Kit not found");
            if (p.hasPermission("perplayerkit.admin")) {
                p.sendMessage(String.valueOf(ChatColor.RED) + "To assign a kit to this publickit use /savepublickit <id>");
            }
            return null;
        }
        ChestMenu menu = ((ChestMenu.Builder)((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Viewing Public Kit: " + id)).redraw(true)).build();
        for (i = 0; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        for (i = 9; i < 36; ++i) {
            menu.getSlot(i).setItem(kit[i]);
        }
        for (i = 0; i < 9; ++i) {
            menu.getSlot(i + 36).setItem(kit[i]);
        }
        for (i = 36; i < 41; ++i) {
            menu.getSlot(i + 9).setItem(kit[i]);
        }
        menu.getSlot(45).setItem(ItemUtil.createItem(Material.CHAINMAIL_BOOTS, 1, "<gray><b>BOOTS</b></gray>"));
        menu.getSlot(46).setItem(ItemUtil.createItem(Material.CHAINMAIL_LEGGINGS, 1, "<gray><b>LEGGINGS</b></gray>"));
        menu.getSlot(47).setItem(ItemUtil.createItem(Material.CHAINMAIL_CHESTPLATE, 1, "<gray><b>CHESTPLATE</b></gray>"));
        menu.getSlot(48).setItem(ItemUtil.createItem(Material.CHAINMAIL_HELMET, 1, "<gray><b>HELMET</b></gray>"));
        menu.getSlot(49).setItem(ItemUtil.createItem(Material.SHIELD, 1, "<gray><b>OFFHAND</b></gray>"));
        menu.getSlot(52).setItem(ItemUtil.createItem(Material.APPLE, 1, "<green><b>LOAD KIT</b></green>"));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        this.addPublicKitMenu(menu.getSlot(53));
        GUI.addLoadPublicKit(menu.getSlot(52), id);
        menu.open(p);
        return menu;
    }

    public void OpenPublicKitMenu(Player player) {
        int i;
        Menu menu = GUI.createPublicKitMenu();
        for (i = 0; i < 54; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }
        for (i = 18; i < 36; ++i) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BOOK, 1, "<gray><b>MORE KITS COMING SOON</b></gray>"));
        }
        List<PublicKit> publicKitList = KitManager.get().getPublicKitList();
        for (int i2 = 0; i2 < publicKitList.size(); ++i2) {
            if (KitManager.get().hasPublicKit(publicKitList.get((int)i2).id)) {
                if (player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i2 + 18).setItem(ItemUtil.createItem(publicKitList.get((int)i2).icon, 1, String.valueOf(ChatColor.RESET) + publicKitList.get((int)i2).name, "<gray>\u25cf [ADMIN] Shift click to edit</gray>"));
                } else {
                    menu.getSlot(i2 + 18).setItem(ItemUtil.createItem(publicKitList.get((int)i2).icon, 1, String.valueOf(ChatColor.RESET) + publicKitList.get((int)i2).name));
                }
                this.addPublicKitButton(menu.getSlot(i2 + 18), publicKitList.get((int)i2).id);
            } else if (player.hasPermission("perplayerkit.admin")) {
                menu.getSlot(i2 + 18).setItem(ItemUtil.createItem(publicKitList.get((int)i2).icon, 1, String.valueOf(ChatColor.RESET) + publicKitList.get((int)i2).name + " <red><b>[UNASSIGNED]</b></red>", "<gray>\u25cf Admins have not yet setup this kit yet</gray>", "<gray>\u25cf [ADMIN] Shift click to edit</gray>"));
            } else {
                menu.getSlot(i2 + 18).setItem(ItemUtil.createItem(publicKitList.get((int)i2).icon, 1, String.valueOf(ChatColor.RESET) + publicKitList.get((int)i2).name + " <red><b>[UNASSIGNED]</b></red>", "<gray>\u25cf Admins have not yet setup this kit yet</gray>"));
            }
            if (!player.hasPermission("perplayerkit.admin")) continue;
            this.addAdminPublicKitButton(menu.getSlot(i2 + 18), publicKitList.get((int)i2).id);
        }
        this.addMainButton(menu.getSlot(53));
        menu.getSlot(53).setItem(ItemUtil.createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        menu.open(player);
    }

    public void addClear(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = 0; i < 41; ++i) {
                    m.getSlot(i).setItem((ItemStack)null);
                }
            }
        });
    }

    public void addClear(Slot slot, int start, int end) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = start; i < end; ++i) {
                    m.getSlot(i).setItem((ItemStack)null);
                }
            }
        });
    }

    public void addClearKit(Slot slot, UUID target, int slotNum) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                KitManager.get().deleteKit(target, slotNum);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + slotNum + " deleted for player!");
                SoundManager.playSuccess(player);
                kitDeletionFlag.add(player.getUniqueId());
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addClearEnderchest(Slot slot, UUID target, int slotNum) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                KitManager.get().deleteEnderchest(target, slotNum);
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Enderchest " + slotNum + " deleted for player!");
                SoundManager.playSuccess(player);
                kitDeletionFlag.add(player.getUniqueId());
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            Menu m;
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT && (m = this.ViewPublicKitMenu(player, id)) != null) {
                m.open(player);
            }
        });
    }

    public void addAdminPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            Menu m;
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                this.OpenPublicKitEditor(player, id);
                return;
            }
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
            } else if (info.getClickType() == ClickType.RIGHT && (m = this.ViewPublicKitMenu(player, id)) != null) {
                m.open(player);
            }
        });
    }

    public void addMainButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            this.OpenMainMenu(player);
        });
    }

    public void addKitRoom(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            this.OpenKitRoom(player);
            BroadcastManager.get().broadcastPlayerOpenedKitRoom(player);
        });
    }

    public void addKitRoom(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            this.OpenKitRoom(player, page);
        });
    }

    public void addPublicKitMenu(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            this.OpenPublicKitMenu(player);
        });
    }

    public void addKitRoomSaveButton(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isRightClick() && info.getClickType().isShiftClick()) {
                ItemStack[] data = new ItemStack[45];
                for (int i = 0; i < 41; ++i) {
                    data[i] = player.getInventory().getContents()[i];
                }
                KitRoomDataManager.get().setKitRoom(page, data);
                player.sendMessage("saved menu");
                SoundManager.playSuccess(player);
            }
        });
    }

    public void addRepairButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            BroadcastManager.get().broadcastPlayerRepaired(player);
            PlayerUtil.repairAll(player);
            player.updateInventory();
            SoundManager.playSuccess(player);
        });
    }

    public void addClearButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                player.getInventory().clear();
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Inventory cleared");
                SoundManager.playSuccess(player);
            }
        });
    }

    public void addImport(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            Menu m = info.getClickedMenu();
            ItemStack[] inv = this.filterItemsOnImport ? ItemFilter.get().filterItemStack(player.getInventory().getContents()) : player.getInventory().getContents();
            for (int i = 0; i < 41; ++i) {
                m.getSlot(i).setItem(inv[i]);
            }
        });
    }

    public void addImportEC(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            Menu m = info.getClickedMenu();
            ItemStack[] inv = this.filterItemsOnImport ? ItemFilter.get().filterItemStack(player.getEnderChest().getContents()) : player.getEnderChest().getContents();
            for (int i = 0; i < 27; ++i) {
                m.getSlot(i + 9).setItem(inv[i]);
            }
        });
    }

    public void addEdit(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                this.OpenKitMenu(player, i);
            }
        });
    }

    public void addEditEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                this.OpenECKitKenu(player, i);
            }
        });
    }

    public void addLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addEditLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                this.OpenKitMenu(player, i);
            }
        });
    }

    public void addEditLoadEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadEnderchest(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                this.OpenECKitKenu(player, i);
            }
        });
    }

    public Menu createKitMenu(int slot) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Kit: " + slot)).build();
    }

    public Menu createPublicKitMenu(String id) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Public Kit: " + id)).build();
    }

    public Menu createECMenu(int slot) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Enderchest: " + slot)).build();
    }

    public Menu createInspectMenu(int slot, String playerName) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Inspecting " + playerName + "'s kit " + slot)).build();
    }

    public Menu createInspectEcMenu(int slot, String playerName) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Inspecting " + playerName + "'s enderchest " + slot)).build();
    }

    public Menu createMainMenu(Player p) {
        return ((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + p.getName() + "'s Kits")).build();
    }

    public Menu createKitRoom() {
        return ((ChestMenu.Builder)((ChestMenu.Builder)ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Kit Room")).redraw(true)).build();
    }

    public void allowModification(Slot slot) {
        ClickOptions options = ClickOptions.ALLOW_ALL;
        slot.setClickOptions(options);
    }

    private String getPlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer((UUID)uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }
}
