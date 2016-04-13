package gash.router.server.messages.wrk_messages;

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
	private WorkMessage.Builder workMessage;

	public BeatMessage(int nodeId) {
		this.nodeId = nodeId;
		this.workState = WorkState.newBuilder();
		this.workState.setEnqueued(-1);
		this.workState.setProcessed(-1);
		this.header = Common.Header.newBuilder();
		this.header.setNodeId(nodeId);
		this.header.setDestination(destination);
		this.header.setMaxHops(10);
		this.header.setTime(System.currentTimeMillis());
		this.beat = Heartbeat.newBuilder();
		this.workMessage = WorkMessage.newBuilder();
	}

	public WorkMessage getMessage() {

		beat.setState(workState);
		// I update current time again here. There might be a chance that
		// message is created and used later.
		// I assume when user calls getMessage then message will be used in very
		// near time.
		header.setTime(System.currentTimeMillis());

		workMessage.setHeader(header);
		workMessage.setBeat(beat);
		workMessage.setSecret(secret);
		return workMessage.build();
	}

	public void setEnqueued(int enqueued) {
		workState.setEnqueued(enqueued);
	}

	public void setProcessed(int processed) {
		workState.setProcessed(processed);
	}

	public void setNodeId(int nodeId) {
		header.setNodeId(nodeId);
	}

	public void setDestination(int destination) {
		header.setDestination(destination);
	}

	// Todo:Harish Number of max hops can be adjusted based on the number of
	// nodes may be outBoundEdges size()
	public void setMaxHops(int maxHops) {
		header.setMaxHops(maxHops);
	}

	public void setSecret(int secret) {
		this.secret = secret;
	}
}
