package gash.router.server.messages.wrk_messages.handlers;

import Election.Candidate;
import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.messages.wrk_messages.BeatMessage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class BeatMessageHandler implements IWrkMessageHandler{

	private final ServerState state;
	private final Logger logger;
	private IWrkMessageHandler nextHandler;

	public BeatMessageHandler(ServerState state, Logger logger) {
		this.state = state;
		this.logger = logger;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if(workMessage.hasBeat ())  {
			handle(workMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (workMessage, channel);
			}else   {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {

		logger.info("Received Heartbeat from: " + workMessage.getHeader().getNodeId());
		logger.info ("Destination is: " + workMessage.getHeader ().getDestination ());

		// Update out-bound edges with time when heart beat was received as a reply sent.
		EdgeList outBoundEdges = state.getEmon ().getOutboundEdges ();

		EdgeInfo oei = outBoundEdges.getNode (workMessage.getHeader ().getNodeId ());

		if(oei != null) {
			logger.info ("Received reply for my beat, Dropping message");
			oei.setLastHeartbeat (workMessage.getHeader ().getTime ());
			return;
		}

		//Update in-bound when heart beat was received..
		EdgeList inBoundEdges = state.getEmon ().getInboundEdges ();
		EdgeInfo iei = inBoundEdges.getNode (workMessage.getHeader ().getNodeId ());

		if(iei != null) {
			iei.setLastHeartbeat (workMessage.getHeader ().getTime ());
		}

		logger.info("Sending Heartbeat to: " + workMessage.getHeader().getNodeId());
		// construct the message to reply heart beat - notifying I am alive
		BeatMessage beatMessage = new BeatMessage (state.getConf().getNodeId());
		beatMessage.setDestination (workMessage.getHeader ().getNodeId ());

		//todo:Harish This piece of code should be removed
		if (state.getCurrentState() instanceof Candidate) {
			((Candidate)state.getCurrentState()).getClusterSize();
		}
		channel.writeAndFlush (beatMessage.getMessage ());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
