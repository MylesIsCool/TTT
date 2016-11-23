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
import net.caseif.ttt.util.helper.platform.LocationHelper;
import net.caseif.ttt.util.shop.items.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;

public class ChestTrap extends Item implements Listener {

    public static final String NAME = Color.INFO + "Trapped Casket";

    @Override
    public ItemStack getIcon() {
        ItemStack stack = new ItemStack(Material.PISTON_BASE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(NAME);
        meta.setLore(Arrays.asList(ChatColor.WHITE + "Whoever opens it, dies."));

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

    public static void placeFakeChest(final Player player, Block block) {
        // backup
        final Location loc = block.getLocation();
        // Do this
        Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
            @Override
            public void run() {
                TTTCore.getInstance().mg.getChallenger(player.getUniqueId()).get().getRound().getArena().markForRollback(LocationHelper.convert(loc));
                loc.getBlock().setType(Material.PISTON_BASE);
                loc.getBlock().setData((byte) 1);
                player.sendMessage(ChatColor.RED + "Your trap has been placed... don't open it!");
                loc.getBlock().setMetadata("traitor", new FixedMetadataValue(TTTCore.getPlugin(), player));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(final BlockPlaceEvent event) {
        if (isValid(event.getPlayer())) {
            if (isHolding(event.getPlayer(), NAME)) {
                BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.SOUTH};
                for (BlockFace face : faces) {
                    if (event.getBlock().getRelative(face) != null) {
                        if (event.getBlock().getRelative(face).getType() != Material.AIR) {
                            event.getPlayer().sendMessage(ChatColor.RED + "There must be air around the edges and top when placing!");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                // backup
                event.setCancelled(true);
                event.getPlayer().setItemInHand(null);
                // Do this
                placeFakeChest(event.getPlayer(), event.getBlock());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getHand() == EquipmentSlot.OFF_HAND)
            return;
        if (isValid(event.getPlayer())) {
            if (event.getClickedBlock() != null) {
                if (event.getClickedBlock().hasMetadata("traitor")) {
                    event.setCancelled(true);
                    // Damage
                    event.getPlayer().damage(100d, (Player) event.getClickedBlock().getMetadata("traitor").get(0).value());
                    event.getPlayer().sendMessage(ChatColor.RED + "Ouch, looks like that was a trap!");
                    // Restore
                    event.getClickedBlock().setType(Material.AIR);
                    event.getClickedBlock().removeMetadata("traitor", TTTCore.getPlugin());
                }
            }
        }
    }
}