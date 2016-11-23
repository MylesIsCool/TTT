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
