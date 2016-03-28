package gash.router.server.wrk_messages;

import pipe.common.Common;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;

/**
 * @author: codepenman
 * @date: 28/03/2016
 * This class is responsible to create Heart Beat Message of this Node.
 */

public class BeatMessage {

	private WorkState.Builder workState;
	private Common.Header.Builder header;
	private Heartbeat.Builder beat;
	private int nodeId;
	private int destination = -1; // By default Heart Beat Message will be sent to all Nodes..
	private int secret = 1;

	public BeatMessage(int nodeId)    {
		this.nodeId = nodeId;
		workState = WorkState.newBuilder();
		workState.setEnqueued (-1);
		workState.setProcessed (-1);
		header = Common.Header.newBuilder ();
		header.setNodeId (nodeId);
		header.setDestination (destination);
		header.setMaxHops (1);
		header.setTime (System.currentTimeMillis ());
		beat = Heartbeat.newBuilder();
	}

	public WorkMessage getMessage()   {

		beat.setState (workState);
		// I update current time again here. There might be a chance that message is created and used later.
		// I assume when user calls getMessage then message will be used in very near time.
		header.setTime (System.currentTimeMillis ());

		WorkMessage.Builder workMessage = WorkMessage.newBuilder ();
		workMessage.setHeader (header);
		workMessage.setBeat (beat);
		workMessage.setSecret (secret);

		return workMessage.build ();
	}

	public void setEnqueued(int enqueued)   {
		workState.setEnqueued (enqueued);
	}

	public void setProcessed(int processed) {
		workState.setProcessed (processed);
	}

	public void setNodeId(int nodeId)   {
		header.setNodeId (nodeId);
	}

	public void setDestination(int destination) {
		header.setDestination (destination);
	}

	//Todo:Harish Number of max hops can be adjusted based on the number of nodes may be outBoundEdges size()
	public void setMaxHops(int maxHops)    {
		header.setMaxHops (maxHops);
	}

	public void setSecret(int secret) {
		this.secret = secret;
	}
}
