package election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;

import java.util.concurrent.ConcurrentHashMap;

public class Leader implements INodeState, FollowerListener {

	private final Logger logger = LoggerFactory.getLogger("Leader");

	private ServerState state;
	private int nodeId;
	private final Object theObject = new Object();
	private final ConcurrentHashMap<Integer, Object> activeNodes;
	private FollowerHealthMonitor followerMonitor;
	private ElectionUtil util;

	public Leader(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.activeNodes = new ConcurrentHashMap<> ();
		followerMonitor = new FollowerHealthMonitor(this, state,
				state.getConf().getElectionTimeout());
		this.util = new ElectionUtil();
	}

	/*
	* For this event I just reply with SIZEIS message.
	* Since term is set when CANDIDATE starts election, this leader will go to Follower state as part of request vote event.
	*
	* */
	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {

		logger.info("Replying to :" + workMessage.getHeader().getNodeId());
		state.getEmon().broadcastMessage(util.createSizeIsMessage(
			nodeId, workMessage.getHeader().getNodeId()));
		
/*
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
			.getOutboundEdges().getEdgesMap();
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(
					util.createGetClusterSizeMessage(nodeId, destinationId));
			}
		}
*/
	}

	/*
	* In this state we don't need implementation for this event or message type.
	* */
	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
	}

	/*
	* In case I get equal term Id, then we can go to candidate state.
	* */
	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		/*
		* If there is another node which is leader in new term, then I update myself and go back to election state
		* */
		if (workMessage.getLeader().getElectionId() >= state.getElectionId()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());
			state.setState(NodeStateEnum.FOLLOWER);
		}

		/*
		* If there is another leader which is elected in same term then I go back to candidate state to start new election
		* This scenario might happen because we dont have constant cluster size and we evaluate dynamically and it need not be
		* that in 2 seconds we get entire cluster size and split votes might occur, to be on the safe side its better to go to CANDIDATE
		* */
		if(state.getElectionId () == workMessage.getLeader ().getElectionId ()) {
			state.setState (NodeStateEnum.CANDIDATE);
		}
	}

	/*
	* If Vote Request is for term higher than the term I am leader in then I should vote and move back to follower
	* */
	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		if (workMessage.getLeader().getElectionId() > state.getElectionId()) {
			state.setElectionId (workMessage.getLeader ().getElectionId ());
			VoteMessage vote = new VoteMessage(nodeId,
					workMessage.getLeader().getElectionId(),
					workMessage.getLeader().getLeaderId());
			state.getEmon().broadcastMessage(vote.getMessage());
			state.setState (NodeStateEnum.FOLLOWER);
		}
	}

	/*
	* There might be a delay to receive a vote from far neighbour,
	* It is ok, not to consider a vote once I become a leader.
	* */
	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/*
	* Check for the term message came from, i
	* if it is higher than my term then go to follower state,
	* if equal then go to candidate state.
	* if lesser then update beat time from follower
	* */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {

		int newTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		if(newTerm > currentTerm)   {
			state.setState (NodeStateEnum.FOLLOWER);
			return;
		}

		if(newTerm == currentTerm)   {
			state.setState (NodeStateEnum.CANDIDATE);
			return;
		}

		int followerId = workMessage.getHeader ().getNodeId ();
		addFollower (followerId); // Add follower Id
		followerMonitor.onBeat (followerId, System.currentTimeMillis ()); // notify follower monitor about heart beat
	}

	/* Release all the resources. In this case it is only followerMonitor */
	@Override
	public void beforeStateChange() {
		followerMonitor.cancel ();
	}

	@Override
	public void afterStateChange() {
		followerMonitor.start();
	}

	@Override
	public void addFollower(int followerId) {
			activeNodes.put(followerId, theObject);
	}

	@Override
	public void removeFollower(int followerId) {
			activeNodes.remove(followerId);
	}
}
