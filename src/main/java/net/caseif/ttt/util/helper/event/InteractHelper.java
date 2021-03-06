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
import net.caseif.flint.util.physical.Location3D;
import net.caseif.rosetta.Localizable;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.scoreboard.ScoreboardManager;
import net.caseif.ttt.util.Body;
import net.caseif.ttt.util.config.ConfigKey;
import net.caseif.ttt.util.constant.Color;
import net.caseif.ttt.util.constant.MetadataKey;
import net.caseif.ttt.util.constant.Role;
import net.caseif.ttt.util.constant.Stage;
import net.caseif.ttt.util.helper.data.CollectionsHelper;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.platform.InventoryHelper;
import net.caseif.ttt.util.shop.ShopHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static-utility class for player interact-related functionality.
 *
 * @author Max Roncace
 */
public final class InteractHelper {

    private InteractHelper() {
    }

    public static void handleEvent(PlayerInteractEvent event, Challenger opener) {
        // handle body checking
        Location3D clicked = new Location3D(
                event.getClickedBlock().getWorld().getName(),
                event.getClickedBlock().getX(),
                event.getClickedBlock().getY(),
                event.getClickedBlock().getZ());
        if (!(event.getClickedBlock().getType() == Material.PISTON_BASE)) {
            return;
        }
        if (event.getPlayer().isSneaking()) return;

        if (!event.getClickedBlock().hasMetadata("body")) {
            return;
        }
        Body body = (Body) event.getClickedBlock().getMetadata("body").get(0).value();
        foundBody(body, event.getPlayer(), opener);
    }
    public static void foundBody(Body body, Player player, Challenger opener){

        if (opener.getMetadata().containsKey(Role.DETECTIVE) && !opener.isSpectating()) { // handle DNA scanning
            if (player.getItemInHand() != null
                    && player.getItemInHand().getType() == Material.COMPASS
                    && player.getItemInHand().getItemMeta() != null
                    && player.getItemInHand().getItemMeta().getDisplayName() != null
                    && player.getItemInHand().getItemMeta().getDisplayName().endsWith(
                    TTTCore.locale.getLocalizable("item.dna-scanner.name").localize())) {
                doDnaCheck(body, opener, player);
                return;
            }
        }
        searchBody(body, player, 27);

        if (opener.isSpectating() || player.isSneaking()) {
            TTTCore.locale.getLocalizable("info.personal.status.discreet-search").withPrefix(Color.INFO)
                    .sendTo(player);
            return;
        } else if (!body.isFound()) {
            Optional<Challenger> bodyPlayer = TTTCore.mg.getChallenger(body.getPlayer());
            String color;
            switch (body.getRole()) {
                case Role.INNOCENT: {
                    color = Color.INNOCENT;
                    break;
                }
                case Role.TRAITOR: {
                    color = Color.TRAITOR;
                    break;
                }
                case Role.DETECTIVE: {
                    color = Color.DETECTIVE;
                    break;
                }
                default: {
                    throw new AssertionError("Failed to determine role of found body. Report this immediately.");
                }
            }

            body.setFound();
            if (bodyPlayer.isPresent() && bodyPlayer.get().getRound() == body.getRound()) {
                bodyPlayer.get().getMetadata().set(MetadataKey.Player.BODY_FOUND, true);

                ScoreboardManager sm = body.getRound().getMetadata()
                        .<ScoreboardManager>get(MetadataKey.Round.SCOREBOARD_MANAGER).get();
                sm.updateAllEntries();
            }

            Localizable loc = TTTCore.locale.getLocalizable("info.global.round.event.body-find").withPrefix(color);
            Localizable roleMsg
                    = TTTCore.locale.getLocalizable("info.global.round.event.body-find." + body.getRole());
            for (Challenger c : body.getRound().getChallengers()) {
                Player pl = Bukkit.getPlayer(c.getUniqueId());
                loc.withReplacements(opener.getName(), body.getName())
                        .withSuffix(" " + roleMsg.localizeFor(pl)).sendTo(pl);
            }
        }
    }

    public static void doDnaCheck(Body body, Challenger ch, Player pl) {
        if (!body.isFound()) {
            TTTCore.locale.getLocalizable("info.personal.status.dna-id")
                    .withPrefix(Color.ALERT).sendTo(pl);
            return;
        }

        if (!body.getKiller().isPresent() || body.getExpiry() == -1) {
            TTTCore.locale.getLocalizable("info.personal.status.no-dna")
                    .withPrefix(Color.ALERT).sendTo(pl);
            return;
        }

        if (System.currentTimeMillis() > body.getExpiry()) {
            TTTCore.locale.getLocalizable("info.personal.status.dna-decayed")
                    .withPrefix(Color.ALERT).sendTo(pl);
            return;
        }

        Player killer = Bukkit.getPlayer(body.getKiller().get());
        if (killer != null
                && TTTCore.mg.getChallenger(killer.getUniqueId()).isPresent()
                && !TTTCore.mg.getChallenger(killer.getUniqueId()).get().isSpectating()) {
            ch.getMetadata().set("tracking", body.getKiller().get());
            TTTCore.locale.getLocalizable("info.personal.status.collect-dna")
                    .withPrefix(Color.INFO)
                    .withReplacements(body.getName())
                    .sendTo(pl);
        } else {
            TTTCore.locale.getLocalizable("error.round.killer-left")
                    .withPrefix(Color.ALERT).sendTo(pl);
        }
    }

    @SuppressWarnings("deprecation")
    public static void handleGun(PlayerInteractEvent event) {
        // guns
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
                || event.getPlayer().getItemInHand() == null
                || event.getPlayer().getItemInHand().getItemMeta() == null
                || event.getPlayer().getItemInHand().getItemMeta().getDisplayName() == null) {
            return;
        }

        if (ChatColor.stripColor(event.getPlayer().getItemInHand().getItemMeta().getDisplayName())
                .equalsIgnoreCase(TTTCore.locale.getLocalizable("item.gun.name").localize())) {

            Optional<Challenger> ch = TTTCore.mg.getChallenger(event.getPlayer().getUniqueId());
            if (!ch.isPresent() || ch.get().isSpectating()
                    || (ch.get().getRound().getLifecycleStage() == Stage.WAITING
                    || ch.get().getRound().getLifecycleStage() == Stage.PREPARING)) {
                return;
            }

            event.setCancelled(true);
            if (event.getPlayer().getInventory().contains(Material.ARROW)
                    || !TTTCore.config.get(ConfigKey.REQUIRE_AMMO_FOR_GUNS)) {
                if (TTTCore.config.get(ConfigKey.REQUIRE_AMMO_FOR_GUNS)) {
                    InventoryHelper.removeArrow(event.getPlayer().getInventory());
                    event.getPlayer().updateInventory();
                }
                event.getPlayer().launchProjectile(Arrow.class);
            } else {
                TTTCore.locale.getLocalizable("info.personal.status.no-ammo")
                        .withPrefix(Color.ALERT).sendTo(event.getPlayer());
            }
        }
    }

    private static void searchBody(Body body, Player player, int size) {
        Inventory inv = Bukkit.createInventory(player, size, "Body Casket");

        // give token
        if (!body.getTokens().contains(player.getUniqueId())) {
            Optional<Challenger> challengerOptional = TTTCore.getInstance().mg.getChallenger(player.getUniqueId());
            if (challengerOptional.isPresent()) {
                if (ShopHelper.isAlive(challengerOptional.get())) {
                    boolean traitor = false;
                    Optional<Challenger> killer = TTTCore.getInstance().mg.getChallenger(body.getPlayer());
                    if(killer.isPresent()) {
                        if (RoleHelper.isTraitor(killer.get())) {
                            traitor = true;
                        }
                    }
                    if (RoleHelper.isTraitor(challengerOptional.get()) && !traitor) {
                        // Pick up token
                        body.getTokens().add(player.getUniqueId());
                        int tokens = ShopHelper.getTokens(challengerOptional.get());
                        challengerOptional.get().getMetadata().set(ShopHelper.TOKEN_KEY, tokens + 1);
                        player.sendMessage(ChatColor.GRAY + "You have picked up a token from this body, spend it on the shop /ttt shop");
                    } else {
                        // If detective & traitor then :)
                        if (challengerOptional.get().getMetadata().get(Role.DETECTIVE).isPresent()) {
                            body.getTokens().add(player.getUniqueId());
                            int tokens = ShopHelper.getTokens(challengerOptional.get());
                            challengerOptional.get().getMetadata().set(ShopHelper.TOKEN_KEY, tokens + 1);
                            player.sendMessage(ChatColor.GRAY + "You have picked up a token from this body, spend it on the shop /ttt shop");
                        }
                    }
                }
            }
        }
        // player identifier
        {
            ItemStack id = new ItemStack(TTTCore.HALLOWEEN ? Material.JACK_O_LANTERN : Material.PAPER, 1);
            ItemMeta idMeta = id.getItemMeta();
            idMeta.setDisplayName(TTTCore.locale.getLocalizable("item.id.name").localizeFor(player));
            List<String> idLore = new ArrayList<>();
            idLore.add(TTTCore.locale.getLocalizable("item.id.desc").withReplacements(body.getName())
                    .localizeFor(player));
            idMeta.setLore(idLore);
            id.setItemMeta(idMeta);
            inv.addItem(id);
        }

        // role identifier
        {
            ItemStack roleId = new ItemStack(Material.WOOL, 1);
            ItemMeta roleIdMeta = roleId.getItemMeta();
            short durability;
            String roleStr = body.getRole();
            String prefix;
            switch (body.getRole()) {
                case Role.DETECTIVE: {
                    durability = 11;
                    prefix = Color.DETECTIVE;
                    break;
                }
                case Role.INNOCENT: {
                    durability = 5;
                    prefix = Color.INNOCENT;
                    break;
                }
                case Role.TRAITOR: {
                    durability = 14;
                    prefix = Color.TRAITOR;
                    break;
                }
                default: {
                    throw new AssertionError();
                }
            }
            roleId.setDurability(durability);
            roleIdMeta.setDisplayName(TTTCore.locale.getLocalizable("fragment." + roleStr).withPrefix(prefix)
                    .localizeFor(player));
            roleIdMeta.setLore(Collections.singletonList(
                    TTTCore.locale.getLocalizable("item.role." + roleStr).localizeFor(player)
            ));
            roleId.setItemMeta(roleIdMeta);
            inv.addItem(roleId);
        }

        // death clock
        {
            ItemStack clock = new ItemStack(Material.WATCH, 1);
            ItemMeta clockMeta = clock.getItemMeta();
            long deathSeconds = (System.currentTimeMillis() - body.getDeathTime()) / 1000L;
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumIntegerDigits(2);
            String deathTime = nf.format(deathSeconds / 60) + ":" + nf.format(deathSeconds % 60);
            clockMeta.setDisplayName(deathTime);
            clockMeta.setLore(CollectionsHelper.formatLore(
                    TTTCore.locale.getLocalizable("item.deathclock.desc").withReplacements(deathTime)
                            .withReplacements(deathTime).localizeFor(player)
            ));
            clock.setItemMeta(clockMeta);
            inv.addItem(clock);
        }

        // DNA sample
        if (body.getExpiry() > System.currentTimeMillis()) { // still has DNA
            long decaySeconds = (body.getExpiry() - System.currentTimeMillis()) / 1000L;
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumIntegerDigits(2);
            String decayTime = nf.format(decaySeconds / 60) + ":" + nf.format(decaySeconds % 60);
            ItemStack dna = new ItemStack(Material.LEASH, 1);
            ItemMeta dnaMeta = dna.getItemMeta();
            dnaMeta.setDisplayName(TTTCore.locale.getLocalizable("item.dna.name").localizeFor(player));
            dnaMeta.setLore(CollectionsHelper.formatLore(
                    TTTCore.locale.getLocalizable("item.dna.desc").withReplacements(decayTime).localizeFor(player)
            ));
            dna.setItemMeta(dnaMeta);
            inv.addItem(dna);
        }

        player.openInventory(inv);
        TTTCore.mg.getChallenger(player.getUniqueId()).get().getMetadata().set(MetadataKey.Player.SEARCHING_BODY, true);
    }

}
