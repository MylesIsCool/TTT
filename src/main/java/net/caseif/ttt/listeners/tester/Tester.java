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
import net.caseif.flint.round.Round;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class Tester implements Runnable {
    public static final BlockFace[] faces = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final BlockFace[] ns = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH};
    private static final BlockFace[] ew = new BlockFace[]{BlockFace.EAST, BlockFace.WEST};
    private final Block pressurePlate;
    private final List<UUID> players;
    private int task = -1;
    private int stage = 0;
    private int colour = 0;
    private boolean done = false;
    private Round round;

    public Tester(Block clickedBlock, List<UUID> players, Round round) {
        this.pressurePlate = clickedBlock;
        this.players = players;
        this.round = round;
    }

    public void cleanPlayers() {
        for (int i = 0; i < faces.length; i++) {
            Block block = pressurePlate.getRelative(faces[i]);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ShopHelper.isAlive(p)) {
                    if (!players.contains(p.getUniqueId())) {
                        if (p.getLocation().getBlockX() == block.getX() && p.getLocation().getBlockZ() == block.getZ()) {
                            if (Math.abs(p.getLocation().getBlockY() - block.getY()) < 3) { // 3 blocks
                                // Teleport player
                                p.teleport(pressurePlate.getRelative(faces[i]).getRelative(faces[(i + 1) % faces.length]).getLocation().add(0.5, 0, 0.5).setDirection(p.getLocation().getDirection()));
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isDone() {
        return done;
    }

    public void start() {
        cleanPlayers();
        pressurePlate.setMetadata("tester", new FixedMetadataValue(TTTCore.getPlugin(), this));
        RoundHelper.addToCleaner(round, pressurePlate, "tester");
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(TTTCore.getPlugin(), this, 0L, 2L);
    }

    public int getColour() {
        if (colour == 0) {
            int last = 10;
            for (UUID uuid : players) {
                Optional<Challenger> ch = TTTCore.getInstance().mg.getChallenger(uuid);
                if (ch.isPresent()) {
                    if (ShopHelper.isAlive(ch.get())) {
                        last = RoleHelper.isTraitor(ch.get()) ? 14 : 5;
                    }
                }
            }
            colour = last;
        }
        // cache
        // if traitor
        return colour;
        // if not traitor
        // return 5;
    }

    public void run() {
        stage++;
        if (stage % 2 == 0 && stage < 105) {
            pressurePlate.getWorld().playSound(pressurePlate.getLocation(), Sound.BLOCK_NOTE_PLING, 0.1f, 1f);
        }
        if (stage % 2 == 1 && stage < 105) {
            pressurePlate.getWorld().playSound(pressurePlate.getLocation(), Sound.BLOCK_NOTE_PLING, 0.1f, 8f);
        }
        if (stage < 105) {
            pressurePlate.getWorld().spawnParticle(Particle.SMOKE_NORMAL, pressurePlate.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0f);
        }
        if (stage == 1) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setType(Material.STAINED_GLASS);
            }
        }
        if (stage == 2) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setType(Material.STAINED_GLASS);
            }
        }
        if (stage == 5) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 8);
            }
        }
        if (stage == 10) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 8);
            }
        }
        if (stage == 15) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 7);
            }
        }
        if (stage == 20) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 7);
            }
        }
        if (stage == 25) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 15);
            }
        }
        if (stage == 30) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 15);
            }
        }
        if (stage == 35) {
            for (BlockFace face : ns) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 0);
            }
        }
        if (stage == 40) {
            for (BlockFace face : ns) {
                pressurePlate.getRelative(face).setData((byte) 0);
            }
        }
        if (stage == 45 || stage == 65 || stage == 85) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 4);
            }
        }
        if (stage == 50 || stage == 70 || stage == 90) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 4);
            }
        }
        if (stage == 55 || stage == 75 || stage == 95) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 1);
            }
        }
        if (stage == 60 || stage == 80 || stage == 100) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 1);
            }
        }

        if (stage == 105 || stage == 115) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) getColour());
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) getColour());
            }
            if (getColour() == 14) {
                pressurePlate.getWorld().playSound(pressurePlate.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 3f);
            } else {
                pressurePlate.getWorld().playSound(pressurePlate.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 3f);
            }
        }
        if (stage == 105) {
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setData((byte) 0);
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setData((byte) 0);
            }
        }

        if (stage == 150) {
            // clean up
            for (BlockFace face : faces) {
                pressurePlate.getRelative(face).setType(Material.AIR);
            }
        }
        if (stage == 165) {
            // clean up
            for (BlockFace face : faces) {
                pressurePlate.getRelative(BlockFace.UP).getRelative(face).setType(Material.AIR);
            }
            Bukkit.getScheduler().cancelTask(task);
            pressurePlate.removeMetadata("tester", TTTCore.getPlugin());
            done = true;
            // Teleport players out
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    if (ShopHelper.isAlive(player))
                        player.teleport(pressurePlate.getRelative(faces[new SecureRandom().nextInt(faces.length)]).getLocation().add(0.5, 0, 0.5).setDirection(player.getLocation().getDirection()));
                }
            }
        }
    }

    public Round getRound() {
        return round;
    }
}
