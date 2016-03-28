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

import gash.router.server.wrk_messages.handlers.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
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
		IWrkMessageHandler beatMessageHandler = new BeatMessageHandler (this);
		IWrkMessageHandler failureMessageHandler = new WrkFailureMessageHandler (this);
		IWrkMessageHandler pingMessageHandler = new WrkPingMessageHandler (this);
		IWrkMessageHandler stateMessageHandler = new StateMessageHandler (this);
		IWrkMessageHandler taskMessageHandler = new TaskMessageHandler (this);

		//Chain all the handlers
		beatMessageHandler.setNextHandler (failureMessageHandler);
		failureMessageHandler.setNextHandler (pingMessageHandler);
		pingMessageHandler.setNextHandler (stateMessageHandler);
		stateMessageHandler.setNextHandler (taskMessageHandler);

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

		if (msg.getHeader().getDestination() != state.getConf().getNodeId()) {
			
			if (msg.getHeader().getMaxHops() == 0) {
				//TODO This might be the detination.. Think before dropping..
				System.out.println("MAX HOPS is Zero! Dropping message...");
				return;
			}
			
			if (msg.getHeader().getNodeId() == state.getConf().getNodeId()) {
				System.out.println("Same message received by source! Dropping message...");
				return;
			}
			
			System.out.println("Forwarding message...");
			WorkMessage.Builder wb = WorkMessage.newBuilder(msg);
			Header.Builder hb = Header.newBuilder(wb.getHeader());
			hb.setMaxHops(hb.getMaxHops() - 1);
			wb.setHeader(hb);
			state.getEmon().broadcastMessage(wb.build());
			return;
		}

		// TODO How can you implement this without if-else statements?
		try {
			wrkMessageHandler.handleMessage (msg, channel);

			/*
			* Create in-bound edge's if it is not created/if it was removed when connection was down.
			* */
			InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress ();
			logger.info ("Remote Address I rec msg from: " + socketAddress.getHostName ());
			logger.info ("Remote Port I rec msg from: " + socketAddress.getPort ());

			getServerState ().getEmon ().createInboundIfNew (msg.getHeader ().getNodeId (),
					socketAddress.getHostName (),
					socketAddress.getPort (),
					channel);

/*
			if (msg.hasBeat()) {
				Heartbeat hb = msg.getBeat();
				logger.info("heartbeat from " + msg.getHeader().getNodeId());
				// construct the message to send
				Common.Header.Builder wm = Common.Header.newBuilder();
				wm.setNodeId(state.getConf().getNodeId());
				wm.setDestination(-1);
				wm.setTime(System.currentTimeMillis());

				WorkMessage.Builder wb = WorkMessage.newBuilder();
				wb.setHeader(wm);
				wb.setPing (true);
				wb.setSecret(1);

				ChannelFuture cf = channel.writeAndFlush (wb.build ());
				if(!cf.isSuccess ())    {
					System.out.println (cf.cause ());
				}

			} else if (msg.hasPing()) {
				logger.info("ping from " + msg.getHeader().getNodeId());
				//Todo: I commented this code to avoid infinite loop. Will update later
				*/
/*boolean p = msg.getPing();
				WorkMessage.Builder rb = WorkMessage.newBuilder();
				rb.setPing(true);
				channel.write(rb.build());*//*

			} else if (msg.hasErr()) {
				Failure err = msg.getErr();
				logger.error("failure from " + msg.getHeader().getNodeId());
				// PrintUtil.printFailure(err);
			} else if (msg.hasTask()) {
				Task t = msg.getTask();
			} else if (msg.hasState()) {
				WorkState s = msg.getState();
			}
*/
		} catch (Exception e) {
			// TODO add logging
			logger.info ("Got an exception in work");
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(state.getConf().getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			WorkMessage.Builder rb = WorkMessage.newBuilder(msg);
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
	protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}

	public ServerState getServerState() {
		return state;
	}
}