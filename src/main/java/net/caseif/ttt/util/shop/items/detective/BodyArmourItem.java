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

import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;

public class BodyArmourItem extends Item {

    public static final String NAME = net.caseif.ttt.util.constant.Color.INFO + "Body Armour";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = makeBlue(new ItemStack(Material.LEATHER_CHESTPLATE));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Reinforced Armour"));

        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public int getMax() {
        return 1;
    }

    @Override
    public void use(Player player) {
        player.getInventory().setHelmet(makeBlue(new ItemStack(Material.LEATHER_HELMET)));
        player.getInventory().setChestplate(makeBlue(new ItemStack(Material.LEATHER_CHESTPLATE)));
        player.getInventory().setLeggings(makeBlue(new ItemStack(Material.LEATHER_LEGGINGS)));
        player.getInventory().setBoots(makeBlue(new ItemStack(Material.LEATHER_BOOTS)));
    }

    private ItemStack makeBlue(ItemStack itemStack) {
        LeatherArmorMeta lam = (LeatherArmorMeta) itemStack.getItemMeta();
        lam.setColor(Color.BLUE);
        itemStack.setItemMeta(lam);
        return itemStack;
    }
}
