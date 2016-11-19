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

package net.caseif.ttt.util;

import net.caseif.ttt.util.constant.Role;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import net.caseif.flint.round.Round;
import net.caseif.flint.util.physical.Location3D;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Body {

    private final Round round;
    private final Location3D location;
    private final UUID player;
    private final String name;
    private final UUID killer;
    private final String role;
    private final long deathTime;
    private final long expiry;

    private boolean found = false;
    private List<UUID> tokens = new ArrayList<>();

    public Body(Round round, Location3D location, UUID player, String name, UUID killer, String role, long deathTime,
                long expireTime) {
        this.round = round;
        this.location = location;
        this.player = player;
        this.name = name;
        this.killer = killer;
        this.role = role != null ? role : Role.INNOCENT;
        this.deathTime = deathTime;
        this.expiry = expireTime;
    }

    public Round getRound() {
        return round;
    }

    public Location3D getLocation() {
        return location;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public Optional<UUID> getKiller() {
        return Optional.fromNullable(killer);
    }

    public String getRole() {
        return role;
    }

    public long getDeathTime() {
        return deathTime;
    }

    public long getExpiry() {
        return expiry;
    }

    public boolean isFound() {
        return found;
    }

    public void  setFound() {
        found = true;
    }

    public List<UUID> getTokens() {
        return tokens;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(round, location, player, name, killer, role, expiry);
    }

}
