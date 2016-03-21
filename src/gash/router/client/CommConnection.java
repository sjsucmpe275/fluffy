/*
 * copyright 2016, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.client;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.CommandInit;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import routing.Pipe.CommandMessage;

/**
 * provides an abstraction of the communication to the remote server.
 * 
 * @author gash
 * 
 */
public class CommConnection {
	protected static Logger logger = LoggerFactory.getLogger("connect");

	protected static AtomicReference<CommConnection> instance = new AtomicReference<CommConnection>();

	private String host;
	private int port;
	private ChannelFuture channel; // do not use directly call
									// connect()!

	private EventLoopGroup group;

	// our surge protection using a in-memory cache for messages
	LinkedBlockingDeque<CommandMessage> outbound;

	// message processing is delegated to a threading model
	private CommWorker worker;

	/**
	 * Create a connection instance to this host/port. On construction the
	 * connection is attempted.
	 * 
	 * @param host
	 * @param port
	 */
	protected CommConnection(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}

	public static CommConnection initConnection(String host, int port) {
		instance.compareAndSet(null, new CommConnection(host, port));
		return instance.get();
	}

	public static CommConnection getInstance() {
		// TODO throw exception if not initialized!
		return instance.get();
	}

	/**
	 * release all resources
	 */
	public void release() {
		channel.cancel(true);
		if (channel.channel() != null)
			channel.channel().close();
		group.shutdownGracefully();
	}

	/**
	 * enqueue a message to write - note this is asynchronous. This allows us to
	 * inject behavior, routing, and optimization
	 * 
	 * @param req
	 *            The request
	 * @exception An
	 *                exception is raised if the message cannot be enqueued.
	 */
	public void enqueue(CommandMessage req) throws Exception {
		// enqueue message
		outbound.put(req);
	}

	/**
	 * messages pass through this method (no queueing). We use a blackbox design
	 * as much as possible to ensure we can replace the underlining
	 * communication without affecting behavior.
	 * 
	 * NOTE: Package level access scope
	 * 
	 * @param msg
	 * @return
	 */
	public boolean write(CommandMessage msg) {
		if (msg == null)
			return false;
		else if (channel == null)
			throw new RuntimeException("missing channel");

		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = connect().writeAndFlush(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			logger.error("failed to send message to server");
			return false;
		}

		return true;
	}

	/**
	 * abstraction of notification in the communication
	 * 
	 * @param listener
	 */
	public void addListener(CommListener listener) {
		CommHandler handler = connect().pipeline().get(CommHandler.class);
		if (handler != null)
			handler.addListener(listener);
	}

	private void init() {
		System.out.println("--> initializing connection to " + host + ":" + port);

		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<CommandMessage>();

		group = new NioEventLoopGroup();
		try {
			CommandInit si = new CommandInit(null, false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();

			// want to monitor the connection to the server s.t. if we loose the
			// connection, we can try to re-establish it.
			ClientClosedListener ccl = new ClientClosedListener(this);
			channel.channel().closeFuture().addListener(ccl);

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());

		} catch (Throwable ex) {
			logger.error("failed to initialize the client connection", ex);
			ex.printStackTrace();
		}

		// start outbound message processor
		worker = new CommWorker(this);
		worker.setDaemon(true);
		worker.start();
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			init();
		}

		if (channel != null && channel.isSuccess() && channel.channel().isWritable())
			return channel.channel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}

	/**
	 * usage:
	 * 
	 * <pre>
	 * channel.getCloseFuture().addListener(new ClientClosedListener(queue));
	 * </pre>
	 * 
	 * @author gash
	 * 
	 */
	public static class ClientClosedListener implements ChannelFutureListener {
		CommConnection cc;

		public ClientClosedListener(CommConnection cc) {
			this.cc = cc;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			System.out.println("--> client lost connection to the server");
			System.out.flush();

			// @TODO if lost, try to re-establish the connection
		}
	}
}
