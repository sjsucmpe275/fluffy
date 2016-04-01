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

import gash.router.server.messages.FailureMessage;
import gash.router.server.messages.wrk_messages.handlers.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;

import java.net.InetSocketAddress;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class WorkChannelHandler extends SimpleChannelInboundHandler<WorkMessage> {
	private static Logger logger = LoggerFactory.getLogger("work");
	private ServerState state;
	private boolean debug = true;
	private IWrkMessageHandler wrkMessageHandler;

	public WorkChannelHandler(ServerState state) {
		if (state != null) {
			this.state = state;
		}
		initializeMessageHandlers();
	}

	public Logger getLogger() {
		return logger;
	}

	private void initializeMessageHandlers() {
		//Define Handlers
		IWrkMessageHandler beatMessageHandler = new BeatMessageHandler (state, logger);
		IWrkMessageHandler failureMessageHandler = new WrkFailureMessageHandler (state, logger);
		IWrkMessageHandler pingMessageHandler = new WrkPingMessageHandler (state, logger);
		IWrkMessageHandler stateMessageHandler = new StateMessageHandler (state, logger);
		IWrkMessageHandler taskMessageHandler = new TaskMessageHandler (state, logger);
		IWrkMessageHandler electionMessageHandler=new ElectionMessageHandler(state, logger);

		//Chain all the handlers
		beatMessageHandler.setNextHandler (failureMessageHandler);
		failureMessageHandler.setNextHandler (pingMessageHandler);
		pingMessageHandler.setNextHandler (stateMessageHandler);
		stateMessageHandler.setNextHandler (taskMessageHandler);
		taskMessageHandler.setNextHandler(electionMessageHandler);

		//Define the start of Chain
		wrkMessageHandler = beatMessageHandler;
	}

	/**
	 * override this method to provide processing behavior. T
	 * 
	 * @param msg
	 */
	public void handleMessage(WorkMessage msg, Channel channel) {
		
		if (msg == null) {
			logger.info ("ERROR: Null message is received");
			return;
		}
		
		if (debug)
			PrintUtil.printWork(msg);

/*
		logger.info ("Received message from: " + msg.getHeader ().getNodeId ());
		logger.info ("Destination is: " + msg.getHeader ().getDestination ());
*/
		
		if (msg.getHeader().getNodeId() == state.getConf().getNodeId()) {
			getLogger ().info ("Same message received by source! Dropping message...");
			return;
		}

		if (msg.getHeader().getDestination() != state.getConf().getNodeId()) {
			
			if (msg.getHeader().getMaxHops() == 0) {
				//TODO This might be the detination.. Think before dropping..
				getLogger ().info ("MAX HOPS is Zero! Dropping message...");
				return;
			}

			getLogger ().info ("Forwarding message...");
			WorkMessage.Builder wb = WorkMessage.newBuilder(msg);
			Header.Builder hb = Header.newBuilder(wb.getHeader());
			hb.setMaxHops(hb.getMaxHops() - 1);
			wb.setHeader(hb);
			state.getEmon().broadcastMessage(wb.build());
			return;
		}

		// TODO How can you implement this without if-else statements? - Implemented COR
		try {
			wrkMessageHandler.handleMessage (msg, channel);

			/*
			* Create in-bound edge's if it is not created/if it was removed when connection was down.
			* */
			InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress ();
			getLogger ().info ("Remote Address I rec msg from: " + socketAddress.getHostName ());
			getLogger ().info ("Remote Port I rec msg from: " + socketAddress.getPort ());

			getServerState ().getEmon ().createInboundIfNew (msg.getHeader ().getNodeId (),
					socketAddress.getHostName (),
					socketAddress.getPort (),
					channel);
		} catch (Exception e) {
			// TODO add logging
			getLogger ().info ("Got an exception in work");
			FailureMessage failureMessage = new FailureMessage (msg, e);
			failureMessage.setNodeId (state.getConf ().getNodeId ());
			channel.write(failureMessage.getWorkMessage ());
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
	protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
		throw new Exception (cause);
	}

	public ServerState getServerState() {
		return state;
	}
}