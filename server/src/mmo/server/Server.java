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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

public class Server {
	private static final Logger L = LoggerFactory.getLogger(Server.class);

	private final Provider<RouteHandler> routeHandlerProvider;
	private final HashedWheelTimer timer;
	private final GameLoop gameLoop;
	private NioEventLoopGroup parentGroup;
	private NioEventLoopGroup childGroup;

	@Inject
    public Server(Provider<RouteHandler> handlerProvider, HashedWheelTimer timer, GameLoop gameLoop) {
		this.routeHandlerProvider = handlerProvider;
		this.timer = timer;
		this.gameLoop = gameLoop;
	}

	public void run(String host, int port) {
		parentGroup = new NioEventLoopGroup();
		childGroup = new NioEventLoopGroup();

		new ServerBootstrap().group(parentGroup, childGroup).channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HttpServerCodec(), //
                                new HttpObjectAggregator(65536), //
                                routeHandlerProvider.get());

					}
				}).option(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(16384)).bind(host, port)
				.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (!future.isSuccess()) {
							L.error("Error setting up server channel: {}", future.cause(), null);
							new Thread(() -> { // TODO shutdown program
												// gracefuller
								try {
									shutdown();
								} catch (InterruptedException e) {
									e.printStackTrace();
								} finally {
									System.exit(1);
								}
							}).start();
						}
					}
				});
	}

	public void shutdown() throws InterruptedException {
		timer.stop();
		Future<?> parentShutdown = parentGroup.shutdownGracefully();
		Future<?> childShutdown = childGroup.shutdownGracefully();
		Future<?> gameLoopShutdown = gameLoop.shutdownGracefully();
		parentShutdown.await();
		childShutdown.await();
		gameLoopShutdown.await();
	}
}