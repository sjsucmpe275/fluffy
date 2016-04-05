package gash.router.server.messages.wrk_messages;

import pipe.common.Common;
import pipe.election.Election;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work;

/**
 * @author: codepenman.
 * @date: 4/3/16
 */
public class LeaderStatusMessage {

	private Work.WorkState.Builder workState;
	private Common.Header.Builder header;
	private Election.LeaderStatus.Builder leaderStatus;
	private int nodeId;
	private int destination = -1; // By default Heart Beat Message will be sent to all Nodes..
	private int secret = 1;

	public LeaderStatusMessage(int nodeId)    {
		this.nodeId = nodeId;
		workState = Work.WorkState.newBuilder();
		workState.setEnqueued (-1);
		workState.setProcessed (-1);
		header = Common.Header.newBuilder ();
		header.setNodeId (nodeId);
		header.setDestination (destination);
		header.setMaxHops (10);
		header.setTime (System.currentTimeMillis ());
		leaderStatus = Election.LeaderStatus.newBuilder ();
	}

	public Work.WorkMessage getMessage()   {
		Work.WorkMessage.Builder workMessage = Work.WorkMessage.newBuilder ();

		// I update current time again here. There might be a chance that message is created and used later.
		// I assume when user calls getMessage then message will be used in very near time.
		header.setTime (System.currentTimeMillis ());

		workMessage.setHeader (header);
		workMessage.setLeader (leaderStatus);
		workMessage.setSecret (secret);

		return workMessage.build ();
	}

	public void setElectionId(int electionId)   {
		leaderStatus.setElectionId (electionId);
	}

	public void setLeaderId(int leaderId)   {
		leaderStatus.setLeaderId (leaderId);
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

	public void setLeaderAction(LeaderQuery leaderAction) {
		leaderStatus.setAction (leaderAction);
	}

	public void setLeaderState(LeaderState leaderState) {
		leaderStatus.setState (leaderState);
	}
}
