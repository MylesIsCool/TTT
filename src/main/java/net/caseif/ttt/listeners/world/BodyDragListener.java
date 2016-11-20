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

import com.google.common.base.Optional;
import net.caseif.flint.challenger.Challenger;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.Body;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.event.InteractHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class BodyDragListener implements Listener, Runnable {
    @Override
    public void run() {
        // Cleanup entities that need dropping
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e.hasMetadata("drag")) {
                    // is being dragged :)
                    Drag drag = (Drag) e.getMetadata("drag").get(0).value();
                    if (drag.isDropping()) {
                        drag.drop();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().equals(e.getFrom())) return;
        if (e.getPlayer().hasMetadata("drag") && ShopHelper.isAlive(e.getPlayer())) {
            Drag drag = (Drag) e.getPlayer().getMetadata("drag").get(0).value();
            if (drag.isHookable()) {
                drag.getEntity().setTicksLived(1);
                drag.getEntity().teleport(getCloseLocation(e.getTo().clone().add(0, 1, 0)));
            }
        }
    }

    @EventHandler
    public void onEntityRight(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof FallingBlock) {
            if (e.getRightClicked().hasMetadata("moveable")) {
                drag(e.getPlayer(), e.getRightClicked());
            }
        }
    }

    public void drag(Player player, Entity entity) {
        if (entity.hasMetadata("drag")) {
            Drag drag = (Drag) entity.getMetadata("drag").get(0).value();
            if (!drag.getPlayer().equals(player.getUniqueId())) {
                // cancel, open
                Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
                if (challengerOptional.isPresent()) {
                    if (ShopHelper.isAlive(challengerOptional.get())) {
                        if (entity.hasMetadata("body")) {
                            InteractHelper.foundBody((Body) entity.getMetadata("body").get(0).value(), player, challengerOptional.get());
                        }
                    }
                }
                return;
            }
        }
        if (player.hasMetadata("drag")) {
            Drag drag = (Drag) player.getMetadata("drag").get(0).value();
            if (drag.getEntity().equals(entity)) {
                if (drag.isHookable()) {
                    drag.update();
                }
                return;
            } else {
                drag.drop();
            }
        }
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
        if (challengerOptional.isPresent()) {
            if (ShopHelper.isAlive(challengerOptional.get())) {
                Drag drag = new Drag(challengerOptional.get().getRound(), player.getUniqueId(), entity);
                player.setMetadata("drag", new FixedMetadataValue(TTTCore.getPlugin(), drag));
                entity.setMetadata("drag", new FixedMetadataValue(TTTCore.getPlugin(), drag));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(e.getPlayer().getUniqueId());
        if (challengerOptional.isPresent()) {
            if (ShopHelper.isAlive(challengerOptional.get())) {
                if (challengerOptional.get().getRound().getLifecycleStage() == Stage.PLAYING) {
                    if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        if (e.getClickedBlock().hasMetadata("body")) {
                            if (e.getClickedBlock().getType() == Material.PISTON_BASE) {
                                if (!e.getPlayer().isSneaking()) return;
                                e.setCancelled(true);

                                e.getClickedBlock().setType(Material.AIR);
                                FallingBlock fb = e.getClickedBlock().getWorld().spawnFallingBlock(e.getClickedBlock().getLocation().add(0.5, 0, 0.5), Material.PISTON_BASE, (byte) 1);

                                fb.setGravity(false);
                                fb.setInvulnerable(true);
                                fb.setDropItem(false);
                                fb.setHurtEntities(false);

                                fb.setMetadata("moveable", new FixedMetadataValue(TTTCore.getPlugin(), true));
                                fb.setMetadata("body", new FixedMetadataValue(TTTCore.getPlugin(), e.getClickedBlock().getMetadata("body").get(0).value()));
                                e.getClickedBlock().removeMetadata("body", TTTCore.getPlugin());
                                drag(e.getPlayer(), fb);
                            }
                        }

                    } else {
                        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            // update drag
                            if (e.getPlayer().hasMetadata("drag")) {
                                Drag drag = (Drag) e.getPlayer().getMetadata("drag").get(0).value();
                                if (drag.isHookable()) {
                                    e.setCancelled(true);
                                    drag.update();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static Location getCloseLocation(Location loc) {
        Location l = loc.clone();
        Vector v = l.getDirection();
        l.setYaw(l.getYaw() + 180);
        l.setPitch(l.getPitch() + 180);
        v.multiply(2);
        l.add(v);
        return l;
    }
}
