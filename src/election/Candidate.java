package election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

import java.util.*;
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
		logger.info("~~~~~~~~Candidate - Handle Cluster Size Event");

		state.getEmon().broadcastMessage(util.createSizeIsMessage(
				state, workMessage.getHeader().getNodeId()));
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("Getting size is:" + workMessage.getHeader().getNodeId());
		visitedNodes.put(workMessage.getHeader().getNodeId(), theObject);
		logger.info("****Getting size is : " + visitedNodes.size ());
		logger.info("****Visited Nodes : " + visitedNodes);
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
	//	validateTermAndUpdateStateIfRequired (workMessage);
		logger.info("~~~~~~~~Candidate - Handle LeaderIs Event");

		int inComingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId ();

		logger.info("Candidate - New Term: " + inComingTerm);
		logger.info("Candidate - Current Term: " + currentTerm);

		if (workMessage.getLeader().getElectionId() >= state.getElectionId()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			//Cancel if there is any timer that is currently started...
			timer.cancel ();

			state.setLeaderHeartBeatdt (System.currentTimeMillis ());

			//Change the state to Follower..
			state.setState(NodeStateEnum.FOLLOWER);
		}
	}

	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~~~~Candidate - Handle Vote Request Event");
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
		logger.info("~~~~~~~~Candidate - Handle Vote Response Event");
		logger.info("Receiving Vote Response from :" + workMessage.getHeader().getNodeId());

		LeaderStatus leader = workMessage.getLeader();
		/*getLeaderId - nothing other than candiate Id, I set this value before I send vote request*/
		if (leader.getVotedFor() == state.getConf ().getNodeId () && leader.getVoteGranted()) {
			votes.put(workMessage.getHeader().getNodeId(), theObject);
			logger.info("Votes: " + votes.toString());
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
		logger.info("~~~~~~~~Candidate - Handle Beat Event");

		int inComingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId ();

		logger.info("Candidate - New Term: " + inComingTerm);
		logger.info("Candidate - Current Term: " + currentTerm);

		if (workMessage.getLeader().getElectionId() >= state.getElectionId()) {
			logger.info("*****Heart Beat From Leader: " + workMessage.getLeader().getLeaderId());
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
		logger.info("~~~~~~~~Candidate - Before State Change Event");
		timer.cancel ();
	}

	@Override
	public void afterStateChange() {
		logger.info("~~~~~~~~Candidate - After State Change Event");
		clear();
		getClusterSize(); // Broadcasts GETCLUSTERSIZE message and starts a timer
	}

	@Override
	public void notifyTimeout() {
		logger.info("~~~~~~~~Candidate - Notify Timeout Event - " + Thread.currentThread ().getName ());
		//Cancel the previous timer...
		timer.cancel ();

		sizeOfCluster = visitedNodes.size() + 1;
		requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);
		logger.info("###############################");
		logger.info("Size of the network is: " + sizeOfCluster);
		logger.info("Required vote count is: " + requiredVotes);
		logger.info("Time: " + System.currentTimeMillis());
		logger.info("###############################");
		
		startElection();

		//start a new timer...
		timer.start (() -> {
			int myVoteCount = votes.size () + 1;
			logger.info("#########################");
			logger.info("Election is over..");
			logger.info("Time: " + System.currentTimeMillis());
			logger.info("Votes I got: " + myVoteCount);
			logger.info("#########################");

			if (myVoteCount > requiredVotes ||
					(myVoteCount == 1 && requiredVotes == 1)) {
				/*Update myself as a Leader in the new term and broad cast LEADERIS message*/
				state.setElectionId(state.getElectionId () + 1);
				state.setLeaderId (state.getConf ().getNodeId ());

				//TODO this should be in separate method.
				state.getEmon().broadcastMessage(util
						.createLeaderIsMessage(state));
				logger.info("State is leader now..");
				state.setState(NodeStateEnum.LEADER);
			}else	{
				timer.start (this, 150 + new Random ().nextInt (150));
			}

			clear();
		}, state.getConf ().getElectionTimeout ());
	}

	private void getClusterSize() {

		state.getEmon ().broadcastMessage (util.createGetClusterSizeMessage(
				state, -1));
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
		logger.info("In candidate state.. dropping message..");
	}
	
	@Override
	public void handleCmdResponse(WorkMessage workMessage, Channel channel) {
		handleCmdQuery(workMessage, channel);
	}
	
	@Override
	public void handleCmdError(WorkMessage workMessage, Channel channel) {
		handleCmdQuery(workMessage, channel);
	}
}