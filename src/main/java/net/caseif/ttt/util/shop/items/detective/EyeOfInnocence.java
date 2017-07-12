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

package net.caseif.ttt.util.shop.items.detective;

import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.shop.ShopHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EyeOfInnocence extends Item implements Listener {

    public static final String NAME = Color.INFO + "Eye of Innocence";
    private static final String ROUND_KEY = "eye";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.EYE_OF_ENDER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Right Click to use", ChatColor.WHITE + "For 10s, everyone can see through walls"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public int getMax() {
        return 5;
    }

    @Override
    public void use(Player player) {
        player.getInventory().addItem(getIcon());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            if (isValid(event.getPlayer())) {
                if (isHolding(event.getPlayer(), NAME)) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        event.setCancelled(true);
                        // Activate Eye of Traitor for traitors
                        final Challenger ch = TTTCore.getInstance().mg.getChallenger(event.getPlayer().getUniqueId()).get();
                        if (!ch.getRound().getMetadata().get(ROUND_KEY).isPresent()) {
                            useItem(event.getPlayer());
                            final List<UUID> glowing = new ArrayList<>();
                            ch.getRound().getMetadata().set(ROUND_KEY, true);
                            for (Challenger cha : ch.getRound().getChallengers()) {
                                if (ShopHelper.isAlive(cha)) {
                                    Bukkit.getPlayer(cha.getUniqueId()).sendMessage(ChatColor.GRAY + "[Glow] For the next 10s everyone can see through walls!");
                                    Bukkit.getPlayer(cha.getUniqueId()).setGlowing(true);
                                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.2f, 1f);
                                    glowing.add(cha.getUniqueId());
                                }
                            }
                            Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
                                @Override
                                public void run() {
                                    ch.getRound().getMetadata().remove(ROUND_KEY);
                                    for (UUID uuid : glowing) {
                                        if (Bukkit.getPlayer(uuid) == null) continue;
                                        if (ShopHelper.isAlive(Bukkit.getPlayer(uuid))) {
                                            Bukkit.getPlayer(uuid).sendMessage(ChatColor.GRAY + "[Glow] The glow has stopped!");
                                        }
                                        Bukkit.getPlayer(uuid).setGlowing(false);
                                    }
                                }
                            }, 20L * 10);

                        } else {
                            event.getPlayer().sendMessage(ChatColor.GRAY + "Wait until the current one is over first!");
                        }
                    }
                }
            }
        }
    }
}
