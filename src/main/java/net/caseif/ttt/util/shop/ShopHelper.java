/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2016, Max Roncace <me@caseif.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.ttt.util.shop;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.listeners.world.ListenerManager;
import net.caseif.ttt.util.constant.MetadataKey;
import net.caseif.ttt.util.constant.Role;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;
import net.caseif.ttt.util.shop.items.DeflectorItem;
import net.caseif.ttt.util.shop.items.Item;
import net.caseif.ttt.util.shop.items.LauncherGun;
import net.caseif.ttt.util.shop.items.detective.BodyArmourItem;
import net.caseif.ttt.util.shop.items.detective.EyeOfInnocence;
import net.caseif.ttt.util.shop.items.detective.PowerGun;
import net.caseif.ttt.util.shop.items.traitor.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * Static utility class for role-related functionality.
 */
public final class ShopHelper {
    public static Item[] DETECTIVE_ITEMS = {new BodyArmourItem(), new PowerGun(), new EyeOfInnocence()};
    public static Item[] TRAITOR_ITEMS = {new OneHitKillKnifeItem(), new DisguisedGun(), new Jihad(), new ChestTrap(), new Deadringer(), new SmokeGrenade(), new EyeOfTraitor()};
    public static Item[] BOTH_ITEMS = {new LauncherGun(), new DeflectorItem()};
    public static final String TOKEN_KEY = "tokens";
    public static final String ITEMS_KEY = "bought_items";

    // Traitor Helmet - glow
    // Aimbot gun
    // C4
    // Health Stations (possibly remove current regen?)
    // Maybe displaying health under a name?
    // Radio controlled arrow? fire from other people
    // End of round stats
    private ShopHelper() {
    }

    public static boolean isAlive(Player player) {
        if (player == null) return false;
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
        if (challengerOptional.isPresent()) {
            return isAlive(challengerOptional.get());
        }
        return false;
    }

    public static boolean isAlive(Challenger player) {
        return !player.isSpectating() && !player.getMetadata().containsKey(MetadataKey.Player.PURE_SPECTATOR);
    }

    public static boolean canUseShop(Challenger player) {
        if (RoleHelper.isTraitor(player) || player.getMetadata().containsKey(Role.DETECTIVE)) {
            if (isAlive(player)) {
                return true;
            }
        }
        return false;
    }

    public static void openShop(Player player) {
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
        if (!challengerOptional.isPresent()) {
            player.sendMessage(ChatColor.RED + "You must be in a game to open the shop.");
            return;
        }
        if (!isAlive(challengerOptional.get())) {
            player.sendMessage(ChatColor.RED + "You must be alive to open the shop");
            return;
        }
        if (!canUseShop(challengerOptional.get())) {
            player.sendMessage(ChatColor.RED + "You must be a detective / traitor to use the shop, *gasp*");
            return;
        }
        boolean traitorShop = RoleHelper.isTraitor(challengerOptional.get());
        Inventory shop = Bukkit.createInventory(new TTTInventoryHolder("shop"), 18, traitorShop ? (ChatColor.RED + "Traitor Shop") : (ChatColor.BLUE + "Detective Shop"));
        // Get tokens
        int tokens = getTokens(challengerOptional.get());
        // Get bought items
        Map<Integer, Integer> bought = (HashMap<Integer, Integer>) (challengerOptional.get().getMetadata().get(ITEMS_KEY).isPresent() ? challengerOptional.get().getMetadata().get(ITEMS_KEY).get() : new HashMap<>());
        // Add items
        List<Item> items = new ArrayList<>();
        items.addAll(Arrays.asList(BOTH_ITEMS));
        items.addAll(Arrays.asList(traitorShop ? TRAITOR_ITEMS : DETECTIVE_ITEMS));

        int i = 0;
        for (Item item : items) {
            int count = bought.containsKey(item.getId()) ? bought.get(item.getId()) : 0;
            if (item.getMax() <= count && item.getMax() != 0) continue;
            ItemStack baseStack = item.getIcon();
            ItemMeta meta = baseStack.getItemMeta();

            List<String> lore = new ArrayList<>(meta.getLore());
            // Check tokens :D
            lore.add(item.getCost() > tokens ? (ChatColor.RED + "Need more tokens") : (ChatColor.GREEN + "Click to buy"));
            lore.add(ChatColor.RED + "Cost: " + item.getCost());
            lore.add(ChatColor.GOLD + "" + (item.getMax() - count) + " Left");
            lore.add("id:" + item.getId());

            meta.setLore(lore);
            baseStack.setItemMeta(meta);
            shop.setItem(i++, baseStack);
        }

        // Display Tokens
        ItemStack tokenStack = new ItemStack(Material.EMERALD, tokens);
        ItemMeta tsMeta = tokenStack.getItemMeta();
        tsMeta.setDisplayName(ChatColor.WHITE + "" + tokens + " Tokens");

        tokenStack.setItemMeta(tsMeta);
        shop.setItem(i++, tokenStack);
        player.openInventory(shop);
    }

    public static int getTokens(Challenger challenger) {
        return challenger.getMetadata().get(TOKEN_KEY).isPresent() ? (int) challenger.getMetadata().get(TOKEN_KEY).get() : 0;
    }

    public static void handleClick(Player player, Inventory inv, InventoryClickEvent event) {
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
        if (!challengerOptional.isPresent()) {
            player.closeInventory();
            return;
        }
        if (!isAlive(challengerOptional.get())) {
            player.closeInventory();
            return;
        }
        if (!canUseShop(challengerOptional.get())) {
            player.closeInventory();
            return;
        }
        // Actual click event yay :D
        TTTInventoryHolder holder = (TTTInventoryHolder) inv.getHolder();
        if (holder.getType().equals("shop")) {
            if (event.getCurrentItem() != null) {
                Optional<Item> item = getItemFromStack(event.getCurrentItem());
                if (item.isPresent()) {
                    // Handle buy
                    int tokens = getTokens(challengerOptional.get());
                    if (tokens < item.get().getCost()) {
                        player.sendMessage(ChatColor.RED + "You don't have enough tokens :(!");
                        return;
                    }
                    challengerOptional.get().getMetadata().set(TOKEN_KEY, tokens - item.get().getCost());
                    player.sendMessage(ChatColor.GREEN + "You have bought the " + event.getCurrentItem().getItemMeta().getDisplayName());
                    item.get().use(player);

                    Map<Integer, Integer> list = new HashMap<>();
                    if (challengerOptional.get().getMetadata().get(ITEMS_KEY).isPresent()) {
                        if (challengerOptional.get().getMetadata().get(ITEMS_KEY).get() instanceof Map) {
                            list = (Map<Integer, Integer>) challengerOptional.get().getMetadata().get(ITEMS_KEY).get();
                        }
                    }
                    list.put(item.get().getId(), (list.containsKey(item.get().getId()) ? list.get(item.get().getId()) : 0) + 1);
                    challengerOptional.get().getMetadata().set(ITEMS_KEY, list);
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (holder.getType().equals("tester")) {
            if (event.getCurrentItem() != null) {
                int rig = 0;
                if (event.getCurrentItem().getType() == Material.STAINED_GLASS) {
                    if (event.getCurrentItem().getDurability() == 5) {
                        rig = 1;
                    }
                    if (event.getCurrentItem().getDurability() == 14) {
                        rig = 2;
                    }
                }
                if (event.getCurrentItem().getType() == Material.GOLD_SWORD) {
                    rig = 3;
                }
                if (rig != 0 && holder.getData().isPresent()) {
                    Block block = (Block) holder.getData().get();
                    if (!block.hasMetadata("alreadyrigged")) {
                        player.sendMessage(ChatColor.RED + "The tester has been rigged for the next player to enter...");
                        RoundHelper.addToCleaner(challengerOptional.get().getRound(), block, "alreadyrigged");
                        RoundHelper.addToCleaner(challengerOptional.get().getRound(), block, "rig");
                        block.setMetadata("alreadyrigged", new FixedMetadataValue(TTTCore.getPlugin(), challengerOptional.get().getUniqueId()));
                        block.setMetadata("rig", new FixedMetadataValue(TTTCore.getPlugin(), rig));
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        player.closeInventory(); // Close :(
    }

    public static Optional<Item> getItemFromStack(ItemStack cursor) {
        if (cursor != null) {
            if (cursor.hasItemMeta()) {
                ItemMeta meta = cursor.getItemMeta();
                if (meta.getLore() != null) {
                    if (meta.getLore().size() > 0) {
                        String idString = meta.getLore().get(meta.getLore().size() - 1);
                        if (idString.startsWith("id:")) {
                            return getItemFromId(Integer.parseInt(idString.split("id:")[1]));
                        }
                    }
                }
            }
        }
        return Optional.absent();
    }

    public static Optional<Item> getItemFromId(int id) {
        for (Item item : DETECTIVE_ITEMS) {
            if (item.getId() == id)
                return Optional.of(item);
        }
        for (Item item : TRAITOR_ITEMS) {
            if (item.getId() == id)
                return Optional.of(item);
        }
        for (Item item : BOTH_ITEMS) {
            if (item.getId() == id)
                return Optional.of(item);
        }
        return null;
    }

    public static void handleClose(Player player, Inventory inventory, InventoryCloseEvent event) {

    }

    public static void registerItemListeners() {
        for (Item item : DETECTIVE_ITEMS) {
            if (item instanceof Listener) {
                ListenerManager.registerListener((Listener) item);
            }
        }
        for (Item item : TRAITOR_ITEMS) {
            if (item instanceof Listener) {
                ListenerManager.registerListener((Listener) item);
            }
        }
        for (Item item : BOTH_ITEMS) {
            if (item instanceof Listener) {
                ListenerManager.registerListener((Listener) item);
            }
        }
    }
}
