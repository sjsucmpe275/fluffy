/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server.edges;

import gash.router.container.Observer;
import gash.router.container.RoutingConf;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.EdgeHealthMonitorTask;
import gash.router.server.QueueManager;
import gash.router.server.ServerState;
import gash.router.server.WorkChannelInitializer;
import gash.router.server.messages.wrk_messages.BeatMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.election.Election;
import pipe.work.Work.WorkMessage;

import java.util.Timer;

public class EdgeMonitor implements EdgeListener, Runnable, Observer {
	private static Logger logger = LoggerFactory.getLogger("edge monitor");
	private static final boolean debug = false;
	
	private EdgeList outboundEdges;
	private EdgeList inboundEdges;
	private long dt = 2000;
	private ServerState state;
	private boolean forever = true;
	private EventLoopGroup group;
	private EdgeHealthMonitorTask edgeHealthMonitorTask;
	
	public EdgeMonitor(ServerState state, QueueManager queues) {
		if (state == null)
			throw new RuntimeException("state is null");

		this.outboundEdges = new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);
		group = new NioEventLoopGroup ();

		if (state.getConf().getRouting() != null) {
			for (RoutingEntry e : state.getConf().getRouting()) {
				outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
			}
		}

		// cannot go below 2 sec
		// if 3000>2000; this.dt=3000
		if (state.getConf().getHeartbeatDt() > this.dt)
			this.dt = state.getConf().getHeartbeatDt();

		edgeHealthMonitorTask = new EdgeHealthMonitorTask (this);

		// Schedule this task only after Delay Time is set..
		Timer timer = new Timer ();
		timer.scheduleAtFixedRate (edgeHealthMonitorTask, 0, getDelayTime ()); //getDelayTime=10000
	}

	public void createInboundIfNew(int ref, String host, int port, Channel channel) {
		EdgeInfo ei = inboundEdges.createIfNew(ref, host, port);
		ei.setChannel (channel);
		ei.setActive (true);
	}

	public void shutdown() {
		forever = false;
	}

	@Override
	public void run() {
		while (forever) {
			try {
				for (EdgeInfo ei : outboundEdges.getEdgesMap ().values()) {
					if (ei.isActive() && ei.getChannel() != null) {
						if (debug)
							logger.info ("*******Sending Heartbeat to: " + ei.getRef ());
						BeatMessage beatMessage = new BeatMessage (state.getConf ().getNodeId ());
						beatMessage.setDestination (ei.getRef ());
						//beatMessage.setMaxHops (state.getConf ().getMaxHops ());
						ei.getChannel().writeAndFlush(beatMessage.getMessage ());
					} else {
						// TODO create a client to the node
						if (debug)
							logger.info("trying to connect to node " + ei.getRef());

						try {
							WorkChannelInitializer wi = new WorkChannelInitializer (state, false);
							Bootstrap b = new Bootstrap();
							b.group(group).channel(NioSocketChannel.class).handler(wi);
							b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
							b.option(ChannelOption.TCP_NODELAY, true);
							b.option(ChannelOption.SO_KEEPALIVE, true);

							// Make the connection attempt.
							ChannelFuture channel = b.connect(ei.getHost(), ei.getPort()).syncUninterruptibly();

							// want to monitor the connection to the server s.t. if we loose the
							// connection, we can try to re-establish it.
//							channel.channel().closeFuture();

							ei.setChannel(channel.channel());
							ei.setActive(channel.channel().isActive());
							ei.setLastHeartbeat (System.currentTimeMillis ());
							System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
									+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());

						} catch (Throwable ex) {
							logger.error("failed to initialize the client connection");
//							ex.printStackTrace();
						}
						logger.info("trying to connect to node " + ei.getRef());
					}
				}

				Thread.sleep(dt);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}
	
	public void broadcastMessage(WorkMessage msg) {
		if(msg.hasLeader () && msg.getLeader ().getAction () == Election.LeaderStatus.LeaderQuery.THELEADERIS)  {
			int termIdBroadCasting = msg.getLeader ().getElectionId ();
			int leaderIdBroadCasting = msg.getLeader ().getLeaderId ();

			System.out.println("Edge Monitor - Term: " + termIdBroadCasting + ", " + Thread.currentThread ().getName ());
			System.out.println("Edge Monitor - Leader Id: " + leaderIdBroadCasting + ", " + Thread.currentThread ().getName ());
		}

		broadCastOutBound (msg);

		broadCastInBound (msg);
	}

	public void broadCastInBound(WorkMessage msg) {
		for(EdgeInfo edge : inboundEdges.getEdgesMap().values()) {
			System.out.println("**********Broadcasting to inbound edges********");
			System.out.println(inboundEdges);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(msg);
			}
		}
	}

	public void broadCastOutBound(WorkMessage msg) {
		System.out.println("**********Broadcasting to outbound edges********");
		System.out.println(outboundEdges);

		for (EdgeInfo edge : outboundEdges.getEdgesMap ().values()) {
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(msg);
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try{
			outboundEdges.clear ();
			inboundEdges.clear ();
			group.shutdownGracefully ();
		}finally {
			super.finalize ();
		}
	}

	public EdgeList getInboundEdges()   {
		return inboundEdges;
	}

	public EdgeList getOutboundEdges()  {
		return outboundEdges;
	}

	public long getDelayTime() {
		return dt;
	}

	public Logger getLogger()  {
		return logger;
	}

	

	@Override
	public void onFileChanged(RoutingConf configuration) {
		logger.info("in edge monitor ");
		outboundEdges = new EdgeList();
		for (RoutingEntry e : configuration.getRouting()) {
			outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
		}
		
	}

}
