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
package gash.router.server;

import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.server.messages.FailureMessage;
import gash.router.server.messages.cmd_messages.handlers.CmdFailureMsgHandler;
import gash.router.server.messages.cmd_messages.handlers.CmdMsgHandler;
import gash.router.server.messages.cmd_messages.handlers.CmdPingMsgHandler;
import gash.router.server.messages.cmd_messages.handlers.CmdQueryMsgHandler;
import gash.router.server.messages.cmd_messages.handlers.ICmdMessageHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class CommandChannelHandler extends SimpleChannelInboundHandler<CommandMessage> {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	private RoutingConf conf;
	private ICmdMessageHandler cmdMessageHandler;
	private LinkedBlockingDeque<CommandMessage> outbound;
	private EventLoopGroup group;
	private ChannelFuture channel;
	private QueueManager queues;
	
	public CommandChannelHandler(RoutingConf conf, QueueManager queues) throws Exception {
		if (conf != null) {
			this.conf = conf;
		}
		this.queues = queues;
		initializeMessageHandlers();
		init();
	}

	private void initializeMessageHandlers() throws Exception {
		// Define Handlers
		ICmdMessageHandler failureMsgHandler = new CmdFailureMsgHandler(conf, logger);
		ICmdMessageHandler pingMsgHandler = new CmdPingMsgHandler(this);
		ICmdMessageHandler msgHandler = new CmdMsgHandler(conf, logger);
		ICmdMessageHandler queryHandler = new CmdQueryMsgHandler(this);

		// Chain all the handlers
		failureMsgHandler.setNextHandler(pingMsgHandler);
		pingMsgHandler.setNextHandler(msgHandler);
		msgHandler.setNextHandler(queryHandler);

		// Define the start of Chain
		cmdMessageHandler = failureMsgHandler;
		
		
	}

	public Logger getLogger() {
		return logger;
	}

	public void enqueue(CommandMessage req) throws Exception {
		// enqueue message
		getOutbound().put(req);
		
	}

	/**
	 * override this method to provide processing behavior. This implementation
	 * mimics the routing we see in annotating classes to support a RESTful-like
	 * behavior (e.g., jax-rs).
	 * 
	 * @param msg
	 */
	public void handleMessage(CommandMessage msg, Channel channel) {
		
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

		PrintUtil.printCommand(msg);

		// TODO How can you implement this without if-else statements? - With
		// COR
		try {
			cmdMessageHandler.handleMessage(msg, channel);
			
//			WorkMessage.Builder wb = WorkMessage.newBuilder();
//
//			Header.Builder header = Common.Header.newBuilder();
//			header.setNodeId(-1);
//			header.setDestination(-1);
//			header.setMaxHops(10);
//			header.setTime(System.currentTimeMillis());
//
//			Task.Builder t = Task.newBuilder();
//			t.setSeqId(msg.getQuery().getSequenceNo());
//			t.setSeriesId(msg.getQuery().getKey().hashCode());
//			t.setTaskMessage(msg);
//
//			wb.setHeader(header);
//			wb.setSecret(1);
//			wb.setTask(t);

		} catch (Exception e) {
			logger.error("Got an exception in command", e);
			FailureMessage failureMessage = new FailureMessage(msg, e);
			failureMessage.setNodeId(getConf().getNodeId());
			failureMessage.setDestination(-1);
			channel.write(failureMessage.getCommandMessage());
		}

		System.out.flush();
	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	public void init() {
		setOutbound(new LinkedBlockingDeque<CommandMessage>());
		CommandServerQueueManager queueMonitorThread = new CommandServerQueueManager(this, queues);
		queueMonitorThread.start();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

	public static class ClientClosedListener implements ChannelFutureListener {
		CommandChannelHandler cc;

		public ClientClosedListener(CommandChannelHandler commandChannelHandler) {
			this.cc = commandChannelHandler;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			System.out.println("--> client lost connection to the server");
			System.out.flush();
		}
	}

	public RoutingConf getConf() {
		return conf;
	}

	public Channel connect() {
		if (channel == null) {
			init();
		}

		if (channel != null && channel.isSuccess()
			&& channel.channel().isWritable())
			return channel.channel();
		else
			throw new RuntimeException("Not able to establish connection ");
	}

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

	public LinkedBlockingDeque<CommandMessage> getOutbound() {
		return outbound;
	}

	public void setOutbound(LinkedBlockingDeque<CommandMessage> outbound) {
		this.outbound = outbound;
	}
	
	
	public QueueManager getQueues() {
		return queues;
	}
}

