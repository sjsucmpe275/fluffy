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

import gash.router.container.RoutingConf;
import gash.router.server.cmd_messages.CmdFailureMessage;
import gash.router.server.cmd_messages.CmdMsgHandler;
import gash.router.server.cmd_messages.CmdPingMessage;
import gash.router.server.cmd_messages.ICmdMessageHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
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
	private static Logger logger = LoggerFactory.getLogger("cmd");
	private RoutingConf conf;
	private ICmdMessageHandler cmdMessageHandler;

	public CommandChannelHandler(RoutingConf conf) {
		if (conf != null) {
			this.conf = conf;
		}

		initializeMessageHandlers();
	}

	private void initializeMessageHandlers() {
		//Define Handlers
		ICmdMessageHandler failureMsgHandler = new CmdFailureMessage (this);
		ICmdMessageHandler pingMsgHandler = new CmdPingMessage (this);
		ICmdMessageHandler msgHandler = new CmdMsgHandler (this);

		//Chain all the handlers
		failureMsgHandler.nextHandler (pingMsgHandler);
		pingMsgHandler.nextHandler (msgHandler);

		//Define the start of Chain
		cmdMessageHandler = failureMsgHandler;
	}

	public Logger getLogger() {
		return logger;
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

		try {
			cmdMessageHandler.handleMessage (msg, channel);
/*
			// TODO How can you implement this without if-else statements?
			if (msg.hasPing()) {
				logger.info("ping from " + msg.getHeader().getNodeId());
				// construct the message to send
				Common.Header.Builder hb = Common.Header.newBuilder();
				hb.setNodeId(888);
				hb.setTime(System.currentTimeMillis());
				hb.setDestination(-1);

				CommandMessage.Builder rb = CommandMessage.newBuilder();
				rb.setHeader(hb);
				rb.setPing(true);

				ChannelFuture cf = channel.writeAndFlush (rb.build());
				if(!cf.isSuccess ())    {
					System.out.println("Reasion for failure : " + cf);
				}
			} else if (msg.hasMessage()) {
				logger.info(msg.getMessage());
			} else {
			}

*/
		} catch (Exception e) {
			// TODO add logging
			logger.info ("Got an exception in command");
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(conf.getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
			rb.setErr(eb);
			channel.write(rb.build());
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
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}
}