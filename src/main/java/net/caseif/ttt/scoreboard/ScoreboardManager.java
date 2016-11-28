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

package net.caseif.ttt.scoreboard;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import net.caseif.flint.challenger.Challenger;
import net.caseif.flint.round.Round;
import net.caseif.ttt.TTTCore;
import net.caseif.ttt.util.Body;
import net.caseif.ttt.util.constant.*;
import net.caseif.ttt.util.helper.gamemode.RoleHelper;
import net.caseif.ttt.util.helper.gamemode.RoundHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import static net.caseif.ttt.util.helper.data.DataVerificationHelper.fromNullableString;

public class ScoreboardManager {

    private static final String OBJECTIVE_ID = "ttt";

    private static final boolean FULL_ENTRY_SUPPORT;
    private static final ImmutableMap<String, String> LIFE_STATUS_PREFIXES = ImmutableMap.<String, String>builder()
            .put(AliveStatus.ALIVE, "")
            .put(AliveStatus.MIA, "§7")
            .put(AliveStatus.CONFIRMED_DEAD, "§m")
            .build();

    private Round round;
    private Scoreboard iBoard = createBoard(false);
    private Scoreboard tBoard = createBoard(true);

    static {
        boolean support = false;
        try {
            Scoreboard.class.getMethod("getEntryTeam", String.class);
            support = true;
        } catch (NoSuchMethodException ignored) {
        }
        FULL_ENTRY_SUPPORT = support;
    }

    public ScoreboardManager(Round round) {
        this.round = round;
    }

    public Round getRound() {
        return round;
    }

    private Scoreboard getInnocentBoard() {
        return iBoard;
    }

    private Scoreboard getTraitorBoard() {
        return tBoard;
    }

    private Scoreboard createBoard(boolean isTBoard) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective(OBJECTIVE_ID, "dummy");
        obj.setDisplayName("Players");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        registerTeams(sb, isTBoard);

        return sb;
    }

    private void registerTeams(Scoreboard sb, boolean isTBoard) {
        final String[] roles = {Role.INNOCENT, Role.TRAITOR, Role.DETECTIVE};
        final String[] aliveStatuses = {AliveStatus.ALIVE, AliveStatus.MIA, AliveStatus.CONFIRMED_DEAD};
        for (String role : roles) {
            for (String alive : aliveStatuses) {
                String teamId = role.charAt(0) + "" + alive.charAt(0);
                if (sb.getTeam(teamId) != null) {
                    return;
                }
                Team team = sb.registerNewTeam(teamId);
                String rolePrefix = role.equals(Role.DETECTIVE)
                        ? Color.DETECTIVE
                        : (role.equals(Role.TRAITOR) && isTBoard ? Color.TRAITOR : "");
                String alivePrefix = fromNullableString(LIFE_STATUS_PREFIXES.get(alive));
                team.setPrefix(rolePrefix + alivePrefix);
            }
        }
    }

    private void applyTeam(Challenger ch) {
        {
            String role = RoleHelper.isTraitor(ch) ? Role.TRAITOR
                    : ch.getMetadata().containsKey(Role.DETECTIVE) ? Role.DETECTIVE
                    : Role.INNOCENT;
            boolean traitorFound = false;
            if (ch.isSpectating() && !ch.getMetadata().containsKey(MetadataKey.Player.BODY_FOUND)) {
                if (ch.getMetadata().get(MetadataKey.Player.BODY).isPresent()) {
                    Body body = (Body) ch.getMetadata().get(MetadataKey.Player.BODY).get();
                    if (body.getKiller().isPresent()) {
                        Optional<Challenger> challengerOptional = ch.getRound().getArena().getMinigame().getChallenger(body.getKiller().get());
                        if (challengerOptional.isPresent()) {
                            if (challengerOptional.get().getMetadata().containsKey(MetadataKey.Player.BODY_FOUND)) {
                                traitorFound = true;
                            }
                        }
                    }
                }
            }
            String alive = !ch.isSpectating() ? (AliveStatus.ALIVE)
                    : ch.getMetadata().containsKey(MetadataKey.Player.BODY_FOUND) ? AliveStatus.CONFIRMED_DEAD
                    : (traitorFound ? AliveStatus.MIA : AliveStatus.ALIVE);
            String teamName = role.charAt(0) + "" + alive.charAt(0);
            ch.getMetadata().set(MetadataKey.Player.TEAM_NAME, teamName);
        }
        {
            String role = RoleHelper.isTraitor(ch) ? Role.TRAITOR
                    : ch.getMetadata().containsKey(Role.DETECTIVE) ? Role.DETECTIVE
                    : Role.INNOCENT;
            String alive = !ch.isSpectating() ? (AliveStatus.ALIVE)
                    : ch.getMetadata().containsKey(MetadataKey.Player.BODY_FOUND) ? AliveStatus.CONFIRMED_DEAD
                    : AliveStatus.MIA;
            String teamName = role.charAt(0) + "" + alive.charAt(0);
            ch.getMetadata().set(MetadataKey.Player.TEAM_NAME_TRAITOR, teamName);
        }
    }

    public void applyScoreboard(Challenger ch) {
        Bukkit.getPlayer(ch.getUniqueId()).setScoreboard(RoleHelper.isTraitor(ch) ? tBoard : iBoard);
    }

    public void updateAllEntries() {
        for (Challenger ch : getRound().getChallengers()) {
            updateEntry(ch);
        }
    }

    @SuppressWarnings("deprecation")
    private void updateEntry(Challenger ch, Scoreboard sb, boolean traitor) {
        assert ch.getRound() == getRound();

        if (ch.getMetadata().has(MetadataKey.Player.PURE_SPECTATOR)) {
            return;
        }

        String teamName = traitor ? ch.getMetadata().<String>get(MetadataKey.Player.TEAM_NAME_TRAITOR).get() : ch.getMetadata().<String>get(MetadataKey.Player.TEAM_NAME).get();

        if (sb.getTeam(teamName) == null) {
            registerTeams(sb, sb == tBoard);
        }
        for (Team team : sb.getTeams()) {
            if (FULL_ENTRY_SUPPORT) {
                if (team.getName().equals(teamName) && !team.hasEntry(ch.getName())) {
                    team.addEntry(ch.getName());
                } else if (!team.getName().equals(teamName) && team.hasEntry(ch.getName())) {
                    team.removeEntry(ch.getName());
                }
            } else {
                Player pl = Bukkit.getPlayer(ch.getUniqueId());
                if (team.getName().equals(teamName) && !team.hasPlayer(pl)) {
                    team.addPlayer(pl);
                } else if (!team.getName().equals(teamName) && team.hasPlayer(pl)) {
                    team.removePlayer(pl);
                }
            }
        }
        sb.getObjective(OBJECTIVE_ID).getScore(ch.getName())
                .setScore(ch.getMetadata().<Integer>get(MetadataKey.Player.DISPLAY_KARMA).or(1000));
    }

    public void updateEntry(Challenger ch) {
        applyTeam(ch);
        updateEntry(ch, getInnocentBoard(), false);
        updateEntry(ch, getTraitorBoard(), true);
    }

    private void remove(Challenger ch, Scoreboard sm) {
        sm.resetScores(ch.getName());
    }

    public void remove(Challenger ch) {
        remove(ch, getInnocentBoard());
        remove(ch, getTraitorBoard());
    }

    public void updateTitle() {
        updateTitle(iBoard.getObjective(OBJECTIVE_ID), false);
        updateTitle(tBoard.getObjective(OBJECTIVE_ID), true);
    }

    private void updateTitle(Objective obj, boolean t) {
        StringBuilder title = new StringBuilder();
        title.append(Color.SECONDARY);
        title.append(TTTCore.locale.getLocalizable("fragment.stage." + round.getLifecycleStage().getId())
                .localize().toUpperCase());

        if (round.getLifecycleStage() != Stage.WAITING) {
            title.append(" - ");
            title.append(RoundHelper.getTimeDisplay(round, t, null));
        }
        obj.setDisplayName(title.toString());
    }

    public void uninitialize() {
        iBoard.getObjective(OBJECTIVE_ID).unregister();
        tBoard.getObjective(OBJECTIVE_ID).unregister();
    }

}
