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

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.shop.ShopHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.UUID;

public class Jihad extends Item implements Listener {
    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.REDSTONE_TORCH_ON);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Jihad Bomb");
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Right click to blow up", ChatColor.WHITE + "Everyone will have a blast!"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 2;
    }

    @Override
    public void use(Player player) {
        player.getInventory().addItem(getIcon());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            if (isValid(event.getPlayer())) {
                if (isHolding(event.getPlayer(), ChatColor.WHITE + "Jihad Bomb")) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        event.getPlayer().setItemInHand(null);
                        activateJihad(event.getPlayer());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlow(EntityExplodeEvent event) {
        if (event.getEntity().hasMetadata("ttt")) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof TNTPrimed) {
            if (event.getDamager().hasMetadata("ttt")) {
                event.setCancelled(true);
                if (event.getEntity() instanceof Player) {
                    final Player target = (Player) event.getEntity();
                    UUID killer = (UUID) event.getDamager().getMetadata("ttt").get(0).value();
                    if (killer.equals(target.getUniqueId())) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(),
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        target.damage(100d, target);
                                        target.sendMessage("You went boom!");
                                    }
                                }, 25L);
                    } else {
                        Player damager = Bukkit.getPlayer(killer);
                        if (damager == null) {
                            target.damage(100d);
                        } else {
                            target.damage(100d, damager);
                        }
                    }
                }
            }
        }
    }

    public void activateJihad(final Player player) {
        player.setWalkSpeed(0.001f);
        final int dinga = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TTTCore.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 1D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.COBBLESTONE));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 5f, 1f);
                    }

                }, 0L, 10L);
        final int dingb = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                TTTCore.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        player.getWorld().spawnParticle(Particle.BLOCK_DUST, player.getLocation().clone().add(0D, 1D, 0D), 100, 0.1f, 0.1f, 0.1f, 0, new MaterialData(Material.ANVIL));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_PLING, 5f, 5f);
                    }

                }, 5L, 10L);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(),
                new Runnable() {
                    @Override
                    public void run() {
                        Bukkit.getScheduler().cancelTask(dinga);
                        Bukkit.getScheduler().cancelTask(dingb);
                        player.setWalkSpeed(0.2f);
                        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
                        if (challengerOptional.isPresent()) {
                            if (ShopHelper.isAlive(challengerOptional.get())) {
                                TNTPrimed tnt = player.getWorld().spawn(
                                        player.getLocation(), TNTPrimed.class);
                                tnt.setFuseTicks(5);
                                tnt.setYield(4.5f);
                                tnt.setIsIncendiary(false);
                                tnt.setMetadata("ttt", new FixedMetadataValue(TTTCore.getPlugin(), player.getUniqueId()));
                            }
                        }
                    }

                }, 40L);
    }
}
