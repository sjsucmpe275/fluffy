package election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

import java.util.concurrent.ConcurrentHashMap;

public class Candidate implements INodeState, TimeoutListener {
	private final Logger logger = LoggerFactory.getLogger("Candidate");
	private final Object theObject = new Object();
	
	private ServerState state;
	private int nodeId;
	private int requiredVotes;
	private int sizeOfCluster;
	private Timer timer;
	private ConcurrentHashMap<Integer, Object> visitedNodes; // I need this to identify size of cluster
	private ConcurrentHashMap<Integer, Object> votes;
	private ElectionUtil util;

	public Candidate(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		timer = new Timer (this, state.getConf ().getElectionTimeout (), "Cand - Election Timer");
		this.visitedNodes = new ConcurrentHashMap<> ();
		this.votes = new ConcurrentHashMap<> ();
		this.util = new ElectionUtil();
	}

	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~Candidate - Handle Cluster Size Event");

		state.getEmon().broadcastMessage(util.createSizeIsMessage(
				state, workMessage.getHeader().getNodeId()));

		/*channel.writeAndFlush(util.createSizeIsMessage(nodeId,
			workMessage.getHeader().getNodeId()));

		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon().getOutboundEdges().getEdgesMap();

		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(
						util.createGetClusterSizeMessage(nodeId, destinationId));
			}
		}*/
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("Getting size is:" + workMessage.getHeader().getNodeId());
		visitedNodes.put(workMessage.getHeader().getNodeId(), theObject);
		System.out.println("****Getting size is : " + visitedNodes.size ());
		System.out.println("****Visited Nodes : " + visitedNodes);
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
	//	validateTermAndUpdateStateIfRequired (workMessage);
		System.out.println("~~~~~~~~Candidate - Handle LeaderIs Event");

		int inComingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId ();

		System.out.println("Candidate - New Term: " + inComingTerm);
		System.out.println("Candidate - Current Term: " + currentTerm);

		if (workMessage.getLeader().getElectionId() >= state.getElectionId()) {
			System.out.println("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			//Cancel if there is any timer that is currently started...
			timer.cancel ();

			//Change the state to Follower..
			state.setState(NodeStateEnum.FOLLOWER);
		}
	}

	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~~~Candidate - Handle Vote Request Event");
		if (workMessage.getLeader().getElectionId() > state.getElectionId()) {
			VoteResponse vote = new VoteResponse (nodeId,
					workMessage.getLeader().getElectionId(),
					workMessage.getLeader().getLeaderId());

			vote.setDestination (workMessage.getHeader ().getNodeId ());
			vote.setMaxHops (state.getConf ().getMaxHops ());
			//Update the term Id I participated in
			state.setElectionId (workMessage.getLeader ().getElectionId ());
			state.setLeaderId (workMessage.getLeader ().getLeaderId ());

			//Reply to the person who sent request
			channel.writeAndFlush (vote.getMessage ());

			// Broadcast the message to outbound edges.
			// Because if my in bound edge is down, I am trying to reach my candidate in different path..
			state.getEmon().broadCastOutBound (vote.getMessage());

			state.setVotedFor (workMessage.getHeader ().getNodeId ());
			state.setLeaderHeartBeatdt (System.currentTimeMillis ());
			state.setState (NodeStateEnum.FOLLOWER);
		}
	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~Candidate - Handle Vote Response Event");
		System.out.println("Receiving Vote Response from :" + workMessage.getHeader().getNodeId());

		LeaderStatus leader = workMessage.getLeader();
		/*getLeaderId - nothing other than candiate Id, I set this value before I send vote request*/
		if (leader.getVotedFor() == state.getConf ().getNodeId () && leader.getVoteGranted()) {
			votes.put(workMessage.getHeader().getNodeId(), theObject);
			System.out.println("Votes: " + votes.toString());
		}

	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/*
	* Term is more than or equal my term I will become follower, if lesser don't do anything
	* */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~Candidate - Handle Beat Event");

		int inComingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId ();

		System.out.println("Candidate - New Term: " + inComingTerm);
		System.out.println("Candidate - Current Term: " + currentTerm);

		if (workMessage.getLeader().getElectionId() >= state.getElectionId()) {
			System.out.println("*****Heart Beat From Leader: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			//Cancel if there is any timer that is currently started...
			timer.cancel ();

			//Update the Leader Heart Beat in my Server State
			state.setLeaderHeartBeatdt (System.currentTimeMillis ());

			//Change the state to Follower..
			state.setState(NodeStateEnum.FOLLOWER);
		}
	}

	@Override
	public void beforeStateChange() {
		System.out.println("~~~~~~~~Candidate - Before State Change Event");
		timer.cancel ();
	}

	@Override
	public void afterStateChange() {
		System.out.println("~~~~~~~~Candidate - After State Change Event");
		clear();
		getClusterSize();
	}

	@Override
	public void notifyTimeout() {
		System.out.println("~~~~~~~~Candidate - Notify Timeout Event - " + Thread.currentThread ().getName ());
		//Cancel the previous timer...
		timer.cancel ();

		sizeOfCluster = visitedNodes.size() + 1;
		requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);
		System.out.println("###############################");
		System.out.println("Size of the network is: " + sizeOfCluster);
		System.out.println("Required vote count is: " + requiredVotes);
		System.out.println("Time: " + System.currentTimeMillis());
		System.out.println("###############################");
		
		startElection();

		//start a new timer...
		timer.start (() -> {
			int myVoteCount = votes.size () + 1;
			System.out.println("#########################");
			System.out.println("Election is over..");
			System.out.println("Time: " + System.currentTimeMillis());
			System.out.println("Votes I got: " + myVoteCount);
			System.out.println("#########################");

			if (myVoteCount >= requiredVotes) {
				/*Update myself as a Leader in the new term and broad cast LEADERIS message*/
				state.setElectionId(state.getElectionId () + 1);
				state.setLeaderId (state.getConf ().getNodeId ());

				//TODO this should be in separate method.
				state.getEmon().broadcastMessage(util
						.createLeaderIsMessage(state));
				System.out.println("State is leader now..");
				state.setState(NodeStateEnum.LEADER);
			}

			clear();
		}, state.getConf ().getElectionTimeout ());
	}

	private void getClusterSize() {

		state.getEmon ().broadcastMessage (util.createGetClusterSizeMessage(
				state, -1));

/*		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
				.getOutboundEdges().getEdgesMap();


		//Broad Cast on Each outbound edge
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);

			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(util.createGetClusterSizeMessage(
						state, destinationId));
			}
		}

		//Broadcast on each in-bound edge
		edgeMap = state.getEmon()
				.getInboundEdges().getEdgesMap();

		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);

			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(util.createGetClusterSizeMessage(
						state, destinationId));
			}
		}
*/

		timer.start ();
	}

	private void startElection() {
		int newElectionId = state.getElectionId() + 1;
		state.getEmon().broadcastMessage(util.createVoteRequest(state, newElectionId));
	}

	private void clear() {
		votes.clear();
		sizeOfCluster = 0;
		requiredVotes = 0;
		visitedNodes.clear();
	}

	@Override
	public void handleCmdQuery(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub
		
	}
}