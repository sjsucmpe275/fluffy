package election;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

public class Candidate implements INodeState, TimeoutListener {
	private final Logger logger = LoggerFactory.getLogger("Candidate");
	private final Object theObject = new Object();
	
	private ServerState state;
	private int nodeId;
	private int requiredVotes;
	private int sizeOfCluster;
	private Timer timer;
	private ConcurrentHashMap<Integer, Object> visitedNodes;
	private ConcurrentHashMap<Integer, Object> votes;
	private ElectionUtil util;

	public Candidate(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.visitedNodes = new ConcurrentHashMap<Integer, Object>();
		this.votes = new ConcurrentHashMap<Integer, Object>();
		this.util = new ElectionUtil();
	}

	public void getClusterSize() {
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
			.getOutboundEdges().getEdgesMap();
		
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(util.createGetClusterSizeMessage(
					nodeId, destinationId));
			}
		}

		timer = new Timer(this, 10000);
		timer.startTimer();
	}

	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		// logger.info ("Replying to :" + workMessage.getHeader().getNodeId());
		channel.writeAndFlush(util.createSizeIsMessage(nodeId,
			workMessage.getHeader().getNodeId()));

	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("Getting size is:" + workMessage.getHeader().getNodeId());
		visitedNodes.put(workMessage.getHeader().getNodeId(), theObject);
		logger.info("Visited Nodes Hashmap:" + visitedNodes);
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		if (state.getElectionId() < workMessage.getLeader().getElectionId()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());
			state.setState(NodeStateEnum.FOLLOWER);
		}
	}

	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
		logger.info("Receiving Vote Response from :"
			+ workMessage.getHeader().getNodeId());

		LeaderStatus leader = workMessage.getLeader();
		if (leader.getVotedFor() == state.getLeaderId()
			&& leader.getVoteGranted()) {
			votes.put(workMessage.getHeader().getNodeId(), theObject);
			logger.info("Votes: " + votes.toString());
		}

	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeStateChange() {

	}

	@Override
	public void afterStateChange() {
		clear();
		getClusterSize();
	}

	@Override
	public void onNewOrHigherTerm() {

	}

	@Override
	public void onLeaderDiscovery() {

	}

	@Override
	public void onHigherTerm() {

	}

	@Override
	public void notifyTimeout() {
		sizeOfCluster = visitedNodes.size() + 1;
		requiredVotes = (int) Math.round((sizeOfCluster / 2) + 0.5f);
		logger.info("###############################");
		logger.info("Size of the network is" + sizeOfCluster);
		logger.info("Required vote count is" + requiredVotes);
		logger.info("Time:" + System.currentTimeMillis());
		logger.info("###############################");
		
		startElection();
		timer = null;
		timer = new Timer(new TimeoutListener() {

			@Override
			public void notifyTimeout() {
				state.getCurrentState();
				logger.info("#########################");
				logger.info("Election is over..");
				logger.info("Time:" + System.currentTimeMillis());
				logger.info("#########################");

				if (votes.size() + 1 >= requiredVotes) {
					//TODO this should be in separate method.
					state.getEmon().broadcastMessage(util
						.createLeaderIsMessage(state));
					logger.info("State is leader now..");
					state.setState(NodeStateEnum.LEADER);
				}

				clear();
			}
		}, state.getConf().getElectionTimeout());
		timer.startTimer();
	}

	private void startElection() {
		state.setElectionId(state.getElectionId() + 1);
		state.getEmon().broadcastMessage(util.createVoteRequest(state));
	}

	private void clear() {
		votes.clear();
		sizeOfCluster = 0;
		requiredVotes = 0;
		visitedNodes.clear();
	}
}
