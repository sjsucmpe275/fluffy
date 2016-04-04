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
package deven.monitor.client;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.monitor.Monitor.ClusterMonitor;

public class MonitorClient {
	
	protected static AtomicReference<MonitorClient> instance = new AtomicReference<MonitorClient>();
	private String host;
	private int port;
	private ChannelFuture channel; // do not use directly call connect()!
	private EventLoopGroup group;
	private static Logger logger = LoggerFactory.getLogger("MonitorCLient");
	/*
	 * Point the host and port to Monitor Server
	 */
	protected MonitorClient(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}
	
	public static MonitorClient initConnection(String host, int port) {
		instance.compareAndSet(null, new MonitorClient(host, port));
		return instance.get();
	}

	public static MonitorClient getInstance() {
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
	
	public boolean write(ClusterMonitor msg) {
		if (msg == null)
			return false;
		else if (channel == null)
			throw new RuntimeException("missing channel");

		ChannelFuture cf = connect().writeAndFlush(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			System.out.println("failed to send message to server");
			return false;
		}
		return true;
	}
	
	/**
	 * abstraction of notification in the communication
	 * 
	 * @param listener
	 */
	public void addListener(MonitorListener listener) {
		MonitorHandler handler = connect().pipeline().get(MonitorHandler.class);
		if (handler != null)
			handler.addListener(listener);
	}
	
	private void init() {
		System.out.println("--> initializing connection to " + host + ":" + port);

		group = new NioEventLoopGroup();
		boolean forever=true;
		while(forever){
		try {
			MonitorServiceInitializer si = new MonitorServiceInitializer(false);
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
			logger.error("failed to initialize the client connection ");
			logger.info("Trying to connect to monitor server ");
			//ex.printStackTrace();
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		}
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
		MonitorClient mc;

		public ClientClosedListener(MonitorClient mc) {
			this.mc = mc;
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
