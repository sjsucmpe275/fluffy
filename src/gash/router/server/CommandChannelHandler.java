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
import gash.router.server.messages.cmd_messages.handlers.*;
import gash.router.server.messages.FailureMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	//protected IDBHandler dbhandler;

	public CommandChannelHandler(RoutingConf conf) throws Exception {
		if (conf != null) {
			this.conf = conf;
		}
		//dbhandler = new RedisDBHandler();

		initializeMessageHandlers();
	}

	private void initializeMessageHandlers() throws Exception {
		//Define Handlers
		ICmdMessageHandler failureMsgHandler = new CmdFailureMsgHandler (this);
		ICmdMessageHandler pingMsgHandler = new CmdPingMsgHandler (this);
		ICmdMessageHandler msgHandler = new CmdMsgHandler (this);
		ICmdMessageHandler queryHandler = new CmdQueryMsgHandler (this);

		//Chain all the handlers
		failureMsgHandler.setNextHandler (pingMsgHandler);
		pingMsgHandler.setNextHandler (msgHandler);
		msgHandler.setNextHandler (queryHandler);

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

		// TODO How can you implement this without if-else statements? - With COR
		try {
			cmdMessageHandler.handleMessage (msg, channel);
		} catch (Exception e) {
			// TODO add logging
			logger.error("Got an exception in command", e);
			FailureMessage failureMessage = new FailureMessage (msg, e);
			failureMessage.setNodeId (getConf ().getNodeId ());
			failureMessage.setDestination (-1);
			channel.write(failureMessage.getCommandMessage ());
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

	public RoutingConf getConf()    {
		return conf;
	}
}