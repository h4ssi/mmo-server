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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import javax.inject.Inject;
import java.net.URI;
import java.util.regex.Pattern;

public class RouteHandler extends ChannelInboundHandlerAdapter {
    private ChannelInboundHandler handler = null;

    private final DefaultHandlerFactory defaultHandlerFactory;
    private final StatusHandlerFactory statusHandlerFactory;
    private final RoomHandlerFactory roomHandlerFactory;

    private static final Pattern PATH_SEP = Pattern.compile(Pattern.quote("/"));

    @Inject
    public RouteHandler(DefaultHandlerFactory defaultHandlerFactory,
                        StatusHandlerFactory statusHandlerFactory,
                        RoomHandlerFactory roomHandlerFactory) {
        this.defaultHandlerFactory = defaultHandlerFactory;
        this.statusHandlerFactory = statusHandlerFactory;
        this.roomHandlerFactory = roomHandlerFactory;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            String[] path = PATH_SEP.split(request.getUri(), -1);
            for (int i = 0; i < path.length; ++i) {
                path[i] = URI.create(path[i]).getPath();
            }

            String first = path.length == 1 ? "" : path[1];

            switch (first) {
                case "":
                    installHandler(ctx, defaultHandlerFactory.create());
                    break;
                case "status":
                    installHandler(ctx, statusHandlerFactory.create());
                    break;
                case "room":
                    int roomId;
                    if (path.length < 3) {
                        roomId = 0;
                    } else {
                        try {
                            roomId = Integer.parseInt(path[2]);
                        } catch (NumberFormatException e) {
                            roomId = 0;
                        }
                    }
                    installHandler(ctx, roomHandlerFactory.create(roomId));
                    break;
                default:
                    installHandler(ctx, null);
            }
        }

        if (handler != null) {
            super.channelRead(ctx, msg);
        } else {
            ctx.writeAndFlush(create404());
            ReferenceCountUtil.release(msg);
        }
    }

    private void installHandler(ChannelHandlerContext ctx,
                                ChannelInboundHandler newHandler) {
        if (handler != null) {
            ctx.pipeline().removeLast();
            handler = null;
        }
        if (newHandler != null) {
            ctx.pipeline().addLast(newHandler);
            handler = newHandler;
        }
    }

    private FullHttpResponse create404() {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpHeaders.setKeepAlive(response, true);
        HttpHeaders.setContentLength(response, 0);

        return response;
    }
}