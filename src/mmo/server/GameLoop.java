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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.Future;
import mmo.server.message.CannotEnter;
import mmo.server.message.Chat;
import mmo.server.message.Entered;
import mmo.server.message.InRoom;
import mmo.server.message.Left;
import mmo.server.message.Moved;
import mmo.server.message.Moving;
import mmo.server.model.Coord;
import mmo.server.model.Direction;
import mmo.server.model.Player;
import mmo.server.model.PlayerInRoom;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class GameLoop {
    private final DefaultEventExecutorGroup loop =
            new DefaultEventExecutorGroup(1);
    private final MessageHub messageHub;
    private Room room = new Room();

    private final HashedWheelTimer timer;
    private boolean tickTock = false;
    private TimerTask task = new TimerTask() {

        @Override
        public void run(Timeout timeout) throws Exception {
            tickTock ^= true;
            if (tickTock) {
                tick();
            } else {
                tock();
            }
            schedule();
        }
    };
    private Set<Player> players = Sets.newConcurrentHashSet();

    @Inject
    public GameLoop(MessageHub messageHub, HashedWheelTimer timer) {
        this.messageHub = messageHub;
        this.timer = timer;
        schedule();
    }

    private void tock() {
        System.out.println("tock"); // TODO remove
    }

    private void tick() {
        System.out.println("tick"); // TODO remove
    }

    private void schedule() {
        timer.newTimeout(task, 1, TimeUnit.SECONDS);
    }

    public void login(final Player entering) {
        loop.submit(new Runnable() {
            @Override
            public void run() {
                players.add(entering);

                PlayerInRoom enteringPlayerInRoom =
                        room.enter(new Coord(8, 8), entering);
                if (enteringPlayerInRoom == null) {
                    messageHub.sendMessage(entering, new CannotEnter());
                } else {
                    messageHub.sendMessage(
                            room.contents(),
                            new Entered(enteringPlayerInRoom));

                    Iterable<Player> others = Iterables.filter(
                            room.contents(),
                            new Predicate<Player>() {
                                @Override
                                public boolean apply(Player input) {
                                    return input != entering;
                                }
                            }
                    );
                    Iterable<PlayerInRoom> othresInRoom = Iterables.transform(
                            others, new Function<Player, PlayerInRoom>() {
                                @Override
                                public PlayerInRoom apply(Player input) {
                                    return new PlayerInRoom(room
                                            .getId(input),
                                            input,
                                            room.getCoord(input));
                                }
                            }
                    );
                    messageHub.sendMessage(
                            entering,
                            new InRoom(Iterables.toArray(
                                    othresInRoom, PlayerInRoom.class)));
                }
            }
        });
    }

    public void logout(final Player leaving) {
        loop.submit(new Runnable() {
            @Override
            public void run() {
                int id = room.leave(leaving);

                messageHub.sendMessage(room.contents(), new Left(id));

                players.remove(leaving);
            }
        });
    }

    public void chat(final Player author, final String message) {
        loop.submit(new Runnable() {
            @Override
            public void run() {
                messageHub.sendMessage(
                        room.contents(),
                        new Chat(room.getId(author), message));
            }
        });
    }

    public Future<?> shutdownGracefully() {
        return loop.shutdownGracefully();
    }

    public void moving(final Player player, final Direction dir) {
        loop.submit(new Runnable() {
            @Override
            public void run() {
                messageHub.sendMessage(
                        room.contents(),
                        new Moving(room.getId(player), dir)
                );
                timer.newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        move(player, dir);
                    }
                }, 66, TimeUnit.MILLISECONDS);
            }
        });
    }

    private void move(final Player player, final Direction dir) {
        loop.submit(new Runnable() {
            @Override
            public void run() {
                messageHub.sendMessage(
                        room.contents(),
                        new Moved(room.getId(player))
                );
            }
        });
    }
}
