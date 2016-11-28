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

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Item {
    private static int total = 0;
    private final int id;

    public Item() {
        this.id = total++;
    }

    public abstract ItemStack getIcon();

    public abstract int getCost();

    public abstract void use(Player player);

    public void useItem(Player player) {
        if (player.getItemInHand().getAmount() > 1) {
            player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }
    }

    public int getId() {
        return id;
    }

    public abstract int getMax();

    public static boolean isValid(Player player) {
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
        if (!challengerOptional.isPresent()) {
            return false;
        }
        if (challengerOptional.get().getRound().isEnding()) {
            return false;
        }
        if (!ShopHelper.isAlive(challengerOptional.get())) {
            return false;
        }
        return true;
    }

    public static boolean isHolding(Player player, String itemName) {
        if (player.getItemInHand() == null) return false;
        if (!player.getItemInHand().hasItemMeta()) return false;
        return player.getItemInHand().getItemMeta().getDisplayName().equals(itemName);
    }
}
