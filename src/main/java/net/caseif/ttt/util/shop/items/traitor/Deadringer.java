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

package net.caseif.ttt.util.shop.items.traitor;

import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.util.physical.Location3D;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.helper.event.DeathHelper;
import net.caseif.ttt.util.helper.platform.LocationHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Deadringer extends Item implements Listener {

    public static final String NAME = Color.INFO + "Deadringer";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.WATCH);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "When you drop below 2 hearts", ChatColor.WHITE + "Teleport to a random spawn"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 2;
    }

    @Override
    public int getMax() {
        return 1;
    }

    @Override
    public void use(Player player) {
        player.getInventory().addItem(getIcon());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            double result = player.getHealth() - event.getFinalDamage();
            if (result <= 2D) {
                if (ShopHelper.isAlive(player)) {
                    ItemStack stack = null;
                    ItemStack trap = null;
                    for (ItemStack i : player.getInventory()) {
                        if (i != null) {
                            if (i.hasItemMeta()) {
                                if (i.getItemMeta() != null) {
                                    if (i.getItemMeta().getDisplayName() != null) {
                                        if (i.getItemMeta().getDisplayName().equals(NAME)) {
                                            stack = i;
                                        }
                                        if (i.getItemMeta().getDisplayName().equals(ChestTrap.NAME)) {
                                            trap = i;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (stack != null) {
                        // Particles
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 1D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.OBSIDIAN));
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 0.8D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.OBSIDIAN));
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 0.6D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.OBSIDIAN));
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 0.4D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.OBSIDIAN));
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 0.2D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.OBSIDIAN));
                        // Teleport
                        Challenger ch = TTTCore.getInstance().mg.getChallenger(player.getUniqueId()).get();
                        Collection<Location3D> locations = ch.getRound().getArena().getSpawnPoints().values();
                        // Activate trap
                        if (trap != null) {
                            ChestTrap.placeFakeChest(player, DeathHelper.relocate(ch.getRound(), player.getLocation(), false).getBlock());
                            player.getInventory().remove(trap);
                        }

                        player.teleport(LocationHelper.convert(pickLocation(player, locations)));
                        // Reset health
                        player.setHealth(4D);
                        event.setCancelled(true);
                        if (stack.getAmount() > 1) {
                            stack.setAmount(stack.getAmount() - 1);
                        } else {
                            player.getInventory().remove(stack);
                        }
                        return;
                    }
                }
            }
        }
    }

    private Location3D pickLocation(Player player, Collection<Location3D> locations) {
        List<Location3D> filtered = new ArrayList<>();
        for (Location3D loc : locations) {
            if (LocationHelper.convert(loc).distance(player.getLocation()) > 15) {
                filtered.add(loc);
            }
        }
        if (filtered.size() == 0)
            filtered.addAll(locations);
        return filtered.get(new SecureRandom().nextInt(filtered.size()));
    }
}
