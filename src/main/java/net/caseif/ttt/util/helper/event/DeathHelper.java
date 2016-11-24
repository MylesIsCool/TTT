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

package net.caseif.ttt.util.helper.event;

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.round.Round;
import net.caseif.flint.util.physical.Boundary;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.scoreboard.ScoreboardManager;
import net.caseif.ttt.util.Body;
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.constant.MetadataKey;
import net.caseif.ttt.util.constant.Role;
import net.caseif.ttt.util.helper.gamemode.KarmaHelper;
import net.caseif.ttt.util.helper.platform.LocationHelper;
import net.caseif.ttt.util.helper.platform.NmsHelper;
import net.caseif.ttt.util.helper.platform.PlayerHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

import static net.caseif.ttt.util.helper.gamemode.RoleHelper.isTraitor;

/**
 * Utility class for player death-related functionality.
 */
public final class DeathHelper {

    private final PlayerDeathEvent event;
    private final Player player;

    public DeathHelper(PlayerDeathEvent event) {
        this.event = event;
        this.player = event.getEntity();
    }

    public DeathHelper(Player player) {
        this.event = null;
        this.player = player;
    }

    public void handleEvent() {
        // admittedly not the best way of doing this, but easiest for the purpose of porting
        Optional<Challenger> chOpt = TTTCore.mg.getChallenger(player.getUniqueId());
        if (!chOpt.isPresent()) {
            return;
        }
        Challenger ch = chOpt.get();
        Location loc = player.getLocation();

        Optional<Challenger> killer = getKiller();

        cancelEvent(ch);

        if (killer.isPresent()) {
            // set killer's karma
            KarmaHelper.applyKillKarma(killer.get(), ch);
            if (killer.get().getRound().getLifecycleStage().getDuration() != -1
                    && isTraitor(killer.get()) && !isTraitor(chOpt.get())) {
                killer.get().getRound().getMetadata().set(MetadataKey.Round.HASTE_TIME,
                        killer.get().getRound().getMetadata().<Integer>get(MetadataKey.Round.HASTE_TIME).or(0)
                                + TTTCore.config.get(ConfigKey.HASTE_SECONDS_PER_DEATH));
                killer.get().getRound().setTime(killer.get().getRound().getTime()
                        - TTTCore.config.get(ConfigKey.HASTE_SECONDS_PER_DEATH));
            }
        }

        Block block = loc.getBlock();
        while (block.getType() != Material.AIR && block.getType() != Material.WATER
                && block.getType() != Material.LAVA && block.getType() != Material.STATIONARY_WATER
                && block.getType() != Material.STATIONARY_LAVA) {
            block = loc.add(0, 1, 0).getBlock();
        }

        ch.getRound().getArena().markForRollback(LocationHelper.convert(block.getLocation()));

        createBody(block.getLocation(), ch, killer.orNull());

        ch.getRound().getMetadata().<ScoreboardManager>get(MetadataKey.Round.SCOREBOARD_MANAGER).get().updateEntry(ch);
    }

    private void cancelEvent(final Challenger ch) {
        Location loc = player.getLocation(); // sending the packet resets the location

        if (event != null) {
            event.setDeathMessage("");
            event.getDrops().clear();

            NmsHelper.sendRespawnPacket(player);
            player.teleport(loc);
        }

        ch.setSpectating(true);
        PlayerHelper.watchPlayerGameMode(ch);

        player.setHealth(player.getMaxHealth());
    }

    private Optional<Challenger> getKiller() {
        if (event == null || player.getKiller() == null) {
            return Optional.absent();
        }

        UUID uuid = null;
        if (player.getKiller().getType() == EntityType.PLAYER) {
            uuid = player.getKiller().getUniqueId();
        } else if (player.getKiller() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) player).getShooter();
            if (shooter instanceof Player) {
                uuid = ((Player) shooter).getUniqueId();
            }
        }

        if (uuid != null) {
            return TTTCore.mg.getChallenger(uuid);
        }
        return Optional.absent();
    }

    public static boolean isInBounds(Location l, Round round, boolean playerFlag) {
        // if dead body in way, not in bounds :D?
        if (playerFlag) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (ShopHelper.isAlive(player)) {
                    if (player.getLocation().getBlockX() == l.getBlockX() && player.getLocation().getBlockY() == l.getBlockY() && player.getLocation().getBlockZ() == l.getBlockZ()) {
                        return false;
                    }
                }
            }
        }
        Boundary bound = round.getArena().getBoundary();
        return bound.contains(LocationHelper.convert(l));
    }

    public static Location relocate(Round round, Location loc, boolean checkPlayers) {
        loc = loc.getBlock().getLocation();
        if (loc.getBlock().getType() == Material.AIR && DeathHelper.isInBounds(loc, round, checkPlayers)) {
            Block b = loc.getBlock();
            for (int i = 0; i < 5; i++) {
                if (b.getRelative(BlockFace.DOWN).getType() != Material.AIR) {
                    return b.getLocation();
                }
                b = b.getRelative(BlockFace.DOWN);
                if (b.getType() == Material.AIR && DeathHelper.isInBounds(b.getLocation(), round, checkPlayers)) {
                    return b.getLocation();
                }
            }
            return loc;
        } else {
            BlockFace[] faces = new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH};
            for (BlockFace face : faces) {
                Block b = loc.getBlock();
                for (int i = 0; i < 5; i++) {
                    b = b.getRelative(face);
                    for (BlockFace face2 : faces) {
                        for (int i2 = 0; i2 < 5; i2++) {
                            b = b.getRelative(face2);
                            if (b.getType() == Material.AIR && DeathHelper.isInBounds(b.getLocation(), round, checkPlayers)) {
                                return b.getLocation();
                            }
                        }
                    }
                }
            }
            // Failed :(
            return new Location(loc.getWorld(), round.getArena().getBoundary().getLowerBound().getX(), round.getArena().getBoundary().getLowerBound().getY(), round.getArena().getBoundary().getLowerBound().getZ());
        }
    }

    private void createBody(Location loc, Challenger ch, Challenger killer) {
        Boundary bound = ch.getRound().getArena().getBoundary();
        if (!bound.contains(LocationHelper.convert(loc))) {
            loc = relocate(ch.getRound(), loc, true);
        }
        loc.getBlock().setType(Material.PISTON_BASE);
        loc.getBlock().setData((byte) 1);

        storeBody(loc.getBlock(), ch, killer);
    }

    private void storeBody(Block block, Challenger ch, Challenger killer) {
        long expiry = -1;
        if (killer != null) {
            double dist = player.getLocation().toVector()
                    .distance(Bukkit.getPlayer(killer.getUniqueId()).getLocation().toVector());
            if (dist <= TTTCore.config.get(ConfigKey.KILLER_DNA_RANGE)) {
                final double a = 0.2268; // copied from the official gamemode and scaled to account for different units
                int decayTime
                        = TTTCore.config.get(ConfigKey.KILLER_DNA_BASETIME) - (int) Math.floor(a * Math.pow(dist, 2));
                if (decayTime > 0) {
                    expiry = System.currentTimeMillis() + (decayTime * 1000);
                }
            }
        }

        Body body = new Body(
                ch.getRound(),
                LocationHelper.convert(block.getLocation()),
                ch.getUniqueId(),
                ch.getName(),
                killer != null ? killer.getUniqueId() : null,
                ch.getMetadata().containsKey(Role.DETECTIVE)
                        ? Role.DETECTIVE
                        : (ch.getTeam().isPresent() ? ch.getTeam().get().getId() : null),
                System.currentTimeMillis(),
                expiry);
        block.setMetadata("body", new FixedMetadataValue(TTTCore.getPlugin(), body));
        ch.getMetadata().set(MetadataKey.Player.BODY, body);
    }

}
