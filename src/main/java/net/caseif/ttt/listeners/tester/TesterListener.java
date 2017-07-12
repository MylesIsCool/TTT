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

package net.caseif.ttt.listeners.tester;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import net.caseif.ttt.util.shop.TTTInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TesterListener implements Listener {
    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;
        if (!ShopHelper.isAlive(e.getPlayer())) return;
        final Challenger ch = TTTCore.getInstance().mg.getChallenger(e.getPlayer().getUniqueId()).get();
        if (ch.getRound().getLifecycleStage() != Stage.PLAYING) return;
        if (e.getAction() == Action.PHYSICAL || (e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (e.getClickedBlock() != null) {
                if (e.getClickedBlock().getType() == Material.IRON_PLATE) {
                    if (e.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.IRON_BLOCK) {
                        // Traitor tester
                        if (e.getClickedBlock().hasMetadata("tester")) {
                            Tester tester = (Tester) e.getClickedBlock().getMetadata("tester").get(0).value();
                            if (tester.isDone() || (ch.getRound() != tester.getRound())) {
                                e.getClickedBlock().removeMetadata("tester", TTTCore.getPlugin());
                            }
                        }
                        if (!e.getClickedBlock().hasMetadata("tester")) {
                            if (e.getAction() == Action.PHYSICAL) {
                                // Initiate tester
                                if (!e.getClickedBlock().hasMetadata("checker")) {
                                    e.getPlayer().sendMessage("Warming up tester...");
                                    if (RoleHelper.isTraitor(ch)) {
                                        if (!e.getClickedBlock().hasMetadata("alreadyrigged")) {
                                            e.getPlayer().sendMessage(ChatColor.GRAY + "Hint: As a traitor you can rig the tester by right clicking the pressure plate.");
                                        }
                                    }
                                    RoundHelper.addToCleaner(ch.getRound(), e.getClickedBlock(), "checker");
                                    e.getClickedBlock().setMetadata("checker", new FixedMetadataValue(TTTCore.getPlugin(), true));
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
                                        public void run() {
                                            e.getClickedBlock().removeMetadata("checker", TTTCore.getPlugin());
                                            for (BlockFace face : Tester.faces) {
                                                if (e.getClickedBlock().getRelative(face).hasMetadata("body")) {
                                                    e.getPlayer().sendMessage("You cannot use the tester while dead bodies are in the way! :o");
                                                    return;
                                                }
                                                if (e.getClickedBlock().getRelative(BlockFace.UP).getRelative(face).hasMetadata("body")) {
                                                    e.getPlayer().sendMessage("You cannot use the tester while dead bodies are in the way! :o");
                                                    return;
                                                }
                                            }
                                            // Calculate players to be tested
                                            List<UUID> players = new ArrayList<UUID>();
                                            for (Player p : Bukkit.getOnlinePlayers()) {
                                                if (ShopHelper.isAlive(p)) {
                                                    if (p.getLocation().getBlockX() == e.getClickedBlock().getX() && p.getLocation().getBlockZ() == e.getClickedBlock().getZ()) {
                                                        if (Math.abs(p.getLocation().getBlockY() - e.getClickedBlock().getY()) < 3) { // 3 blocks
                                                            players.add(p.getUniqueId());
                                                        }
                                                    }
                                                }
                                            }
                                            if (players.size() > 0) {
                                                for (UUID uuid : players) {
                                                    Bukkit.getPlayer(uuid).teleport(e.getClickedBlock().getLocation().add(0.5, 0, 0.5).setDirection(Bukkit.getPlayer(uuid).getLocation().getDirection()));
                                                }
                                                Tester tester = new Tester(e.getClickedBlock(), players, ch.getRound());
                                                tester.start();
                                            }
                                        }
                                    }, 20L * 4);

                                }
                            } else {
                                // Open traitor menu
                                if (RoleHelper.isTraitor(ch)) {
                                    if (!e.getClickedBlock().hasMetadata("alreadyrigged")) {
                                        TTTInventoryHolder holder = new TTTInventoryHolder("tester");
                                        holder.setData(Optional.of(e.getClickedBlock()));
                                        List<String> lore = Arrays.asList(ChatColor.RED + "Occurs to next tester user", ChatColor.GREEN + "Can only be rigged once per round.");

                                        Inventory shop = Bukkit.createInventory(holder, 9, "Tester rigger");

                                        ItemStack item1 = new ItemStack(Material.STAINED_GLASS, 1, (byte) 5);
                                        ItemMeta meta1 = item1.getItemMeta();
                                        meta1.setDisplayName(ChatColor.WHITE + "Next person is innocent");
                                        meta1.setLore(lore);
                                        item1.setItemMeta(meta1);
                                        shop.addItem(item1);

                                        ItemStack item2 = new ItemStack(Material.STAINED_GLASS, 1, (byte) 14);
                                        ItemMeta meta2 = item1.getItemMeta();
                                        meta2.setDisplayName(ChatColor.WHITE + "Next person is traitor");
                                        meta2.setLore(lore);
                                        item2.setItemMeta(meta2);
                                        shop.addItem(item2);

                                        ItemStack item3 = new ItemStack(Material.GOLD_SWORD, 1);
                                        ItemMeta meta3 = item1.getItemMeta();
                                        meta3.setDisplayName(ChatColor.WHITE + "Next person dies.");
                                        meta3.setLore(lore);
                                        item3.setItemMeta(meta3);
                                        shop.addItem(item3);

                                        e.getPlayer().openInventory(shop);
                                    } else {
                                        if (e.getClickedBlock().hasMetadata("rig")) {
                                            e.getPlayer().sendMessage("The tester is currently rigged...");
                                        } else {
                                            e.getPlayer().sendMessage("The tester may only be rigged once per round.");
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
}
