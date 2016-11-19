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

import net.caseif.ttt.TTTCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.Arrays;

public class LauncherGun extends Item implements Listener {
    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.END_ROD);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Launching Gun");
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Blast people away!"));

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
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isValid(event.getPlayer())) {
                if (isHolding(event.getPlayer(), ChatColor.WHITE + "Launching Gun")) {
                    Snowball snowball = event.getPlayer().launchProjectile(Snowball.class);
                    snowball.setMetadata("blast", new FixedMetadataValue(TTTCore.getPlugin(), true));
                }
            }
        }
    }

    @EventHandler
    public void onDmg(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Snowball) {
            if (event.getDamager().hasMetadata("blast")) {
                // Launch player
                double knockback = 5;
                Vector vector = event.getDamager().getVelocity();
                double horizontalSpeed = Math.sqrt(vector.getX() * vector.getX() + vector.getZ() * vector.getZ());
                Vector velocity = event.getEntity().getVelocity();
                velocity.setX(velocity.getX() + vector.getX() * knockback * 0.6 / horizontalSpeed);
                velocity.setY(velocity.getY() + 0.1);
                velocity.setZ(velocity.getZ() + vector.getZ() * knockback * 0.6 / horizontalSpeed);
                event.getEntity().setVelocity(velocity);
            }
        }
    }
}
