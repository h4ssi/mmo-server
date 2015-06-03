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

package mmo.server;

import mmo.server.model.Coord;
import mmo.server.model.PlayerInRoom;
import org.testng.annotations.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RoomTest {
    private static GameLoop.Callback createDummyCallback() {
        return new GameLoop.Callback() {
            @Override
            public void tick() {

            }

            @Override
            public void tock() {

            }

            @Override
            public void cannotEnter() {

            }

            @Override
            public void entered(int id, Coord coord) {

            }

            @Override
            public void left(int id) {

            }

            @Override
            public void inRoom(List<PlayerInRoom> inRoom) {

            }

            @Override
            public void chat(String message) {

            }
        };
    }

    @Test
    public void enterEmptyRoom() {
        Room room = new Room();

        GameLoop.Callback cb = createDummyCallback();
        assertThat("empty room can be entered",
                room.enter(new Coord(0, 0), cb),
                is(new Coord(0, 0)));

        assertThat("player is in room",
                room.contents(), hasItem(cb));
        assertThat("tile is occupied",
                room.getCoord(cb), is(new Coord(0, 0)));
        assertThat("first id to be used 0",
                room.getId(cb), is(0));
    }

    @Test
    public void enterRoomAndGetDisplaced() {
        Room room = new Room();

        room.enter(new Coord(0, 0), createDummyCallback());

        assertThat("player is displaced upon entering if tile is occupied",
                room.enter(new Coord(0, 0), createDummyCallback()),
                allOf(
                        notNullValue(),
                        not(is(new Coord(0, 0)))));

    }

    @Test
    public void enterFullRoom() {
        Room room = new Room();

        for (int i = 0; i < Room.SIZE * Room.SIZE; ++i) {
            room.enter(new Coord(0, 0), createDummyCallback());
        }

        assertThat("room cannot be entered if it is already full",
                room.enter(new Coord(0, 0), createDummyCallback()),
                nullValue());
    }
}