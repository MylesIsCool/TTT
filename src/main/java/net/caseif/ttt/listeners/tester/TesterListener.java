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

import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TesterListener implements Listener {
    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (!ShopHelper.isAlive(e.getPlayer())) return;
        if (e.getAction() == Action.PHYSICAL) {
            if (e.getClickedBlock() != null) {
                if (e.getClickedBlock().getType() == Material.IRON_PLATE) {
                    if (e.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.IRON_BLOCK) {
                        // Traitor tester
                        if (!e.getClickedBlock().hasMetadata("tester")) {
                            if (!e.getClickedBlock().hasMetadata("checker")) {
                                e.getClickedBlock().setMetadata("checker", new FixedMetadataValue(TTTCore.getPlugin(), true));
                                Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
                                    public void run() {
                                        e.getClickedBlock().removeMetadata("checker", TTTCore.getPlugin());
                                        for (BlockFace face : Tester.faces) {
                                            if (e.getClickedBlock().getRelative(face).hasMetadata("body")) {
                                                return;
                                            }
                                            if (e.getClickedBlock().getRelative(BlockFace.UP).getRelative(face).hasMetadata("body")) {
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
                                            Tester tester = new Tester(e.getClickedBlock(), players);
                                            tester.start();
                                        }
                                    }
                                }, 20L * 4);

                            }
                        }
                    }
                }
            }
        }
    }
}
