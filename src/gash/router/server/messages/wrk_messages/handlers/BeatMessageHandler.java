package gash.router.server.messages.wrk_messages.handlers;

import gash.router.server.WorkChannelHandler;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.messages.wrk_messages.BeatMessage;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class BeatMessageHandler implements IWrkMessageHandler{

	private final WorkChannelHandler workChannelHandler;
	private IWrkMessageHandler nextHandler;

	public BeatMessageHandler(WorkChannelHandler workChannelHandler) {
		this.workChannelHandler = workChannelHandler;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if(! workMessage.hasBeat () && nextHandler != null)  {
			nextHandler.handleMessage (workMessage, null);
			return;
		}

		if(nextHandler == null) {
			System.out.println("*****No Handler available*****");
			return;
		}

		workChannelHandler.getLogger().info("Received Heartbeat from: " + workMessage.getHeader().getNodeId());
		workChannelHandler.getLogger ().info ("Destination is: " + workMessage.getHeader ().getDestination ());

		// Update out-bound edges with time when heart beat was received as a reply sent.
		EdgeList outBoundEdges = workChannelHandler.getServerState ().getEmon ().getOutboundEdges ();

		EdgeInfo oei = outBoundEdges.getNode (workMessage.getHeader ().getNodeId ());

		if(oei != null) {
			workChannelHandler.getLogger ().info ("Received reply for my beat, Dropping message");
			oei.setLastHeartbeat (workMessage.getHeader ().getTime ());
			return;
		}

		//Update in-bound when heart beat was received..
		EdgeList inBoundEdges = workChannelHandler.getServerState ().getEmon ().getInboundEdges ();
		EdgeInfo iei = inBoundEdges.getNode (workMessage.getHeader ().getNodeId ());

		if(iei != null) {
			iei.setLastHeartbeat (workMessage.getHeader ().getTime ());
		}

		workChannelHandler.getLogger().info("Sending Heartbeat to: " + workMessage.getHeader().getNodeId());
		// construct the message to reply heart beat - notifying I am alive
		BeatMessage beatMessage = new BeatMessage (workChannelHandler.getServerState().getConf().getNodeId());
		beatMessage.setDestination (workMessage.getHeader ().getNodeId ());

		channel.writeAndFlush (beatMessage.getMessage ());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}