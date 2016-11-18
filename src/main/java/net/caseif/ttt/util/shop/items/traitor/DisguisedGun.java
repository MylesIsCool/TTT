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
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.helper.platform.InventoryHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class DisguisedGun extends Item implements Listener {
    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Disguised Gun");
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Looks like a crowbar", ChatColor.WHITE + "Shoots stuff"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public void use(Player player) {
        player.getInventory().addItem(getIcon());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHit(PlayerInteractEvent event) {
        if (isValid(event.getPlayer())) {
            if (isHolding(event.getPlayer(), ChatColor.WHITE + "Disguised Gun")) {
                if (event.getPlayer().getInventory().contains(Material.ARROW)
                        || !TTTCore.config.get(ConfigKey.REQUIRE_AMMO_FOR_GUNS)) {
                    if (TTTCore.config.get(ConfigKey.REQUIRE_AMMO_FOR_GUNS)) {
                        InventoryHelper.removeArrow(event.getPlayer().getInventory());
                        event.getPlayer().updateInventory();
                    }
                    event.getPlayer().launchProjectile(Arrow.class);
                } else {
                    TTTCore.locale.getLocalizable("info.personal.status.no-ammo")
                            .withPrefix(Color.ALERT).sendTo(event.getPlayer());
                }
            }
        }
    }
}
