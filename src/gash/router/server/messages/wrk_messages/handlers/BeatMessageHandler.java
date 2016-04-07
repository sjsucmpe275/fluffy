package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.messages.wrk_messages.BeatMessage;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class BeatMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(BeatMessageHandler.class);
	private IWrkMessageHandler nextHandler;
	private final static boolean debug = false;

	public BeatMessageHandler(ServerState state) {
		this.state = state;
	}

	/*
	 * Messages related to leader should be forwarded to current state of the
	 * node.
	 */
	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (workMessage.hasBeat()) {
			handle(workMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(workMessage, channel);
			} else {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {

		if (debug) {
			logger.info("Received Heartbeat from: " + workMessage.getHeader().getNodeId());
			logger.info("Destination is: " + workMessage.getHeader().getDestination());
		}

		/*
		 * Update out-bound edges with time when heart beat was received as a
		 * reply sent. I consider heart beat as a reply when source node is in
		 * my outbound edges.
		 */
		EdgeList outBoundEdges = state.getEmon().getOutboundEdges();

		EdgeInfo oei = outBoundEdges.getNode(workMessage.getHeader().getNodeId());

		if (oei != null) {
			if (debug)
				logger.info("Received reply for my beat, Dropping message");
			oei.setLastHeartbeat(System.currentTimeMillis());
			// oei.setLastHeartbeat (workMessage.getHeader ().getTime ());
			return;
		}

		// Update in-bound when heart beat was received..
		EdgeList inBoundEdges = state.getEmon().getInboundEdges();
		EdgeInfo iei = inBoundEdges.getNode(workMessage.getHeader().getNodeId());

		if (iei != null) {
			iei.setLastHeartbeat(System.currentTimeMillis());
			// iei.setLastHeartbeat (workMessage.getHeader ().getTime ());
		}

		if (debug)
			logger.info("Sending Heartbeat to: " + workMessage.getHeader().getNodeId());
		// construct the message to reply heart beat - notifying I am alive
		BeatMessage beatMessage = new BeatMessage(state.getConf().getNodeId());
		beatMessage.setDestination(workMessage.getHeader().getNodeId());
		//beatMessage.setMaxHops (state.getConf ().getMaxHops ());
		channel.writeAndFlush(beatMessage.getMessage());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
