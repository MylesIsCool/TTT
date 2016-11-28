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

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.shop.ShopHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Arrays;

public class SmokeGrenade extends Item implements Listener {

    public static final String NAME = Color.INFO + "Smoke Grenade";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(438);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Right click to throw", ChatColor.WHITE + "It will go smokey!"));
        meta.setBasePotionData(new PotionData(PotionType.THICK, false, false));
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public int getMax() {
        return 3;
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
                        // Throw "bomb"
                        ThrownPotion pot = event.getPlayer().launchProjectile(ThrownPotion.class);
                        pot.setItem(event.getPlayer().getItemInHand());
                        pot.setMetadata("smoke", new FixedMetadataValue(TTTCore.getPlugin(), true));
                        useItem(event.getPlayer());
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSplash(final PotionSplashEvent event) {
        if (event.getEntity().hasMetadata("smoke")) {
            event.setCancelled(true);
            // Deploy Smokey!
            final org.bukkit.entity.Item item = event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), getIcon());
            item.setCustomNameVisible(false);
            item.setCustomName("");
            item.setMetadata("smokeg", new FixedMetadataValue(TTTCore.getPlugin(), true));
            // Remove potion
            event.getEntity().remove();
            // Make sure potion is magic
            item.setPickupDelay(Integer.MAX_VALUE);
            // Play smoke effect
            // Smoke iterator
            final int id = Bukkit.getScheduler().scheduleSyncRepeatingTask(TTTCore.getPlugin(), new Runnable() {
                private double radius = 0;
                private int current = 0;

                // go up to 5
                @Override
                public void run() {
                    if (!item.isValid()) return;
                    if (radius % 1 == 0) {
                        event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_BOBBER_THROW, 0.05f, 0.2f);
                    }
                    radius += 0.05D;
                    if (radius > 6) {
                        radius = 0;
                    }
                    // circle :D
                    int amount = 10;
                    int total = 200;
                    // Increment is 2PI / amount
                    double increment = (2 * Math.PI) / total;
                    for (int t = 0; t < amount && (current + t) < total; t++) {
                        int i = current + t;
                        double angle = i * increment;
                        // Simple equation using cos(angle) to work out x between 0-1
                        double x = item.getLocation().getX() + (radius * Math.cos(angle));
                        double z = item.getLocation().getZ() + (radius * Math.sin(angle));
                        Location smoke = new Location(item.getLocation().getWorld(), x, event.getEntity().getLocation().getY() + 0.1D, z);
                        smoke.getWorld().spawnParticle(Particle.SMOKE_NORMAL, smoke, 1, 0, 0, 0, 0f);
                    }
                    // Check if the new current is more than we want
                    current = current + amount;
                    if (current >= total) {
                        current = 0;
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(event.getEntity().getWorld())) {
                            if (ShopHelper.isAlive(player) && !player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                                double dist = player.getLocation().distance(event.getEntity().getLocation());
                                if (dist < 6) {
                                    PotionEffect pot = new PotionEffect(PotionEffectType.BLINDNESS, (int) (16D * (2D * (8 - dist))), 1, true);
                                    pot.apply(player);
                                }
                            }
                        }
                    }
                }
            }, 0L, 1L);

            Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    // Remove
                    Bukkit.getScheduler().cancelTask(id);
                    item.remove();
                }
            }, 20L * 6);

        }
    }
}
