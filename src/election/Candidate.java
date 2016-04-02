package election;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
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
	//Todo: Harish, why do we need visitedNodes MAP, if we have written this did we forget any functionality ?
	private ConcurrentHashMap<Integer, Object> visitedNodes;
	private ConcurrentHashMap<Integer, Object> votes;
	private ElectionUtil util;

	public Candidate(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.visitedNodes = new ConcurrentHashMap<> ();
		this.votes = new ConcurrentHashMap<> ();
		this.util = new ElectionUtil();
	}

	/* Todo:Harish I believe this method should first try to getClusterSize and then reply to the message */
	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		channel.writeAndFlush(util.createSizeIsMessage(nodeId,
			workMessage.getHeader().getNodeId()));
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("Getting size is:" + workMessage.getHeader().getNodeId());
		visitedNodes.put(workMessage.getHeader().getNodeId(), theObject);
		logger.info("Visited Nodes Hashmap:" + visitedNodes);
	}

	/*Todo:Harish - Should stop the election timer started if started and if this candidate finds a new leader/new term*/
	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		if (state.getElectionId() < workMessage.getLeader().getElectionId()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());
			state.setState(NodeStateEnum.FOLLOWER);
		}
	}

	/* Todo:Should respond with vote, if Vote Request is for new term and stop the timer if started */
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

	/* Todo: harish I believe I should ask him to vote me, as part of this message */
	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub

	}

	/*Todo:harish, I think we dont have to do anything here, because even before I receive beat message
	* I will recieve Leader Is and I move to follower state */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub

	}

	/*Todo: Harish Stop the timers - we can stop it here instead of doing it in handlerLeaderIs and handleVoteRequest*/
	@Override
	public void beforeStateChange() {

	}

	@Override
	public void afterStateChange() {
		clear();
		getClusterSize();
	}

	@Override
	public void notifyTimeout() {
		sizeOfCluster = visitedNodes.size() + 1;
		requiredVotes = Math.round((sizeOfCluster / 2) + 0.5f);
		logger.info("###############################");
		logger.info("Size of the network is: " + sizeOfCluster);
		logger.info("Required vote count is: " + requiredVotes);
		logger.info("Time: " + System.currentTimeMillis());
		logger.info("###############################");
		
		startElection();
		timer = null;
		timer = new Timer(new TimeoutListener() {

			@Override
			public void notifyTimeout() {
				state.getCurrentState();
				logger.info("#########################");
				logger.info("Election is over..");
				logger.info("Time: " + System.currentTimeMillis());
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

	/* Todo: How is this really working? We are not broad casting messages, so how this message is sent to all*/
	private void getClusterSize() {
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
				.getOutboundEdges().getEdgesMap();

		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);

			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(util.createGetClusterSizeMessage(
						nodeId, destinationId));
			}
		}

		timer = new Timer(this, state.getConf ().getElectionTimeout ());
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