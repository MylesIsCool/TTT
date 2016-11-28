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

import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class OneHitKillKnifeItem extends Item implements Listener {

    public static final String NAME = Color.INFO + "One Hit Kill Knife";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.GOLD_SWORD);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "One hit kills", ChatColor.WHITE + "If you're 1 block away"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getMax() {
        return 1;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public void use(Player player) {
        player.getInventory().addItem(getIcon());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (isValid(player)) {
                if (isHolding(player, NAME)) {
                    if (event.getDamager().getLocation().distance(event.getEntity().getLocation()) <= 2.5D) {
                        event.setDamage(50D);
                    } else {
                        event.setDamage(5D);
                    }

                    // use knife
                    player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, 1);
                    useItem(player);
                }
            }
        }
    }
}
