/*
 * Copyright 2015 Florian Hassanen
 *
 * This file is part of mmo-server.
 *
 * mmo-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * mmo-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with mmo-server.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package mmo.server.message;

/**
 * Denotes actual movement of a player.
 * <p>
 * Note that the movement is considered to be completed now. The positions of
 * the players are indeed updated.
 * <p>
 * If the player moved onto a tile occupied by another player, their positions
 * are exchanged.
 * <p>
 * Note that, if a player walks off of a room, a <code>Left</code> message will be sent
 * instead.
 *
 * @server sent when a player completed his/her previously started move
 */
public class Moved implements Message {
    /**
     * local room id of moved player
     *
     * @server 5
     */
    private int id;

    public Moved() {
    }

    public Moved(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
