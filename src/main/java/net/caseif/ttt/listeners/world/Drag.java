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

package net.caseif.ttt.listeners.world;

import net.caseif.flint.round.Round;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.event.DeathHelper;
import net.caseif.ttt.util.helper.platform.LocationHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class Drag {
    private UUID player;
    private Entity entity;
    private Long lastTime;
    private Round round;

    public Drag(Round round, UUID player, Entity entity) {
        this.player = player;
        this.entity = entity;
        this.round = round;
        this.lastTime = System.currentTimeMillis();
    }

    public Round getRound() {
        return round;
    }

    public UUID getPlayer() {
        return player;
    }

    public Entity getEntity() {
        return entity;
    }

    public Long getLastTime() {
        return lastTime;
    }

    public void setLastTime(Long lastTime) {
        this.lastTime = lastTime;
    }

    public boolean isHookable() {
        Long diff = System.currentTimeMillis() - getLastTime();
        return diff < 1000 && getRound().getLifecycleStage() == Stage.PLAYING;
    }

    public boolean isDropping() {
        Long diff = System.currentTimeMillis() - getLastTime();
        return diff > 1100;
    }

    public void update() {
        setLastTime(System.currentTimeMillis());
    }

    public void drop() {
        // Remove data
        entity.removeMetadata("drag", TTTCore.getPlugin());
        Player p = Bukkit.getPlayer(player);
        // drop block
        final Location location = entity.getLocation();
        if (location.getBlock().getType() == Material.AIR) {
            if (getRound().getLifecycleStage() == Stage.PLAYING) {
                // if game running?
                Location loc = location.clone();
                loc = DeathHelper.relocate(round, loc);
                try {
                    round.getArena().markForRollback(LocationHelper.convert(loc));
                    Bukkit.getScheduler().scheduleSyncDelayedTask(TTTCore.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            if (getRound().getLifecycleStage() == Stage.PLAYING) {
                                location.getBlock().setType(Material.PISTON_BASE);
                                location.getBlock().setData((byte) 1);
                                location.getBlock().setMetadata("body", new FixedMetadataValue(TTTCore.getPlugin(), entity.getMetadata("body").get(0).value()));
                            }
                        }
                    }, 2L);
                } catch (Exception e) {
                }
            }
            // set metadata
            entity.remove();
        }
        if (p != null) {
            p.removeMetadata("drag", TTTCore.getPlugin());
        }
    }
}
