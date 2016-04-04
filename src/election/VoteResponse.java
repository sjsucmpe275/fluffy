package election;

import pipe.common.Common;
import pipe.election.Election;
import pipe.work.Work;

/**
 * @author: codepenman.
 * @date: 4/2/16
 */
public class VoteResponse {

	private Work.WorkState.Builder workState;
	private Election.LeaderStatus.LeaderState leaderState;
	private Common.Header.Builder header;
	private Election.LeaderStatus.Builder leaderStatus;
	private int nodeId;
	private int destination = -1; // By default Heart Beat Message will be
	// sent to all Nodes..
	private int secret = 1;
	private int electionId;

	public VoteResponse(int nodeId, int electionId, int VoteFor) {
		this.nodeId = nodeId;
		workState = Work.WorkState.newBuilder();
		workState.setEnqueued(-1);
		workState.setProcessed(-1);
		leaderState = Election.LeaderStatus.LeaderState.LEADERDEAD;
		header = Common.Header.newBuilder();
		header.setNodeId(nodeId);
		header.setDestination(destination);
		header.setMaxHops(3);
		header.setTime(System.currentTimeMillis());
		this.electionId = electionId;
		leaderStatus = Election.LeaderStatus.newBuilder();
		leaderStatus.setElectionId(electionId);
		leaderStatus.setVotedFor(VoteFor);
		leaderStatus.setVoteGranted(true);
		leaderStatus.setAction(Election.LeaderStatus.LeaderQuery.VOTERESPONSE);
	}

	public Work.WorkMessage getMessage() {

		header.setTime(System.currentTimeMillis());

		Work.WorkMessage.Builder workMessage = Work.WorkMessage.newBuilder();
		workMessage.setHeader(header);
		workMessage.setLeader(leaderStatus);
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
