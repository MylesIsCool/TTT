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

package net.caseif.ttt.util.shop.items;

import net.caseif.ttt.util.constant.Color;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.security.SecureRandom;
import java.util.Arrays;

public class DeflectorItem extends Item implements Listener {

    public static final String NAME = Color.INFO + "Reflector Chestplate";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Bounces arrows 50% of the time"));

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
        player.getInventory().setChestplate(getIcon());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void arrowHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile) {
            if (e.getEntity() instanceof Player) {
                if (isValid((Player) e.getEntity())) {
                    ItemStack chest = ((Player) e.getEntity()).getInventory().getChestplate();
                    if (chest != null) {
                        if (chest.getItemMeta() != null) {
                            if (chest.getItemMeta().getDisplayName() != null) {
                                if (chest.getItemMeta().getDisplayName().equals(NAME)) {
                                    if (new SecureRandom().nextInt(3) != 1) {
                                        e.setCancelled(true);
                                        e.setDamage(0D);
                                        Projectile p = (Projectile) e.getDamager();
                                        Vector v = p.getVelocity().multiply(-1f);
                                        SecureRandom random = new SecureRandom();
                                        v = v.add(new Vector((-random.nextDouble() + 0.5D) / 10D, (-random.nextDouble() + 0.5D) / 10D, (-random.nextDouble() + 0.5D) / 10D));
                                        p.remove();
                                        // Shoot back
                                        Projectile p2 = ((Player) e.getEntity()).launchProjectile(p.getClass(), v);
                                        p2.setShooter(p.getShooter());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
