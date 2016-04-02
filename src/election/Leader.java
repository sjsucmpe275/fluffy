package election;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
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
		this.util = new ElectionUtil();
	}

	/*
	* In this state, we don't need implementation for this state.
	* Because If I have become leader, and before sending LeaderIs a candidate might come and ask for cluster size
	* but its better to reply with LeaderIs message instead of cluster size message.
	* */
	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		logger.info("Replying to :" + workMessage.getHeader().getNodeId());
		state.getEmon().broadcastMessage(util.createSizeIsMessage(
			nodeId, workMessage.getHeader().getNodeId()));
		
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
			.getOutboundEdges().getEdgesMap();
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(
					util.createGetClusterSizeMessage(nodeId, destinationId));
			}
		}
	}

	/*
	* In this state we don't need implementation for this state.
	* */
	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {

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

	/*
	* Todo:Harish If Vote Request is for term higher than my curren term then I should move back to follower
	* */
	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {

	}

	/*
	* There might be a delay to receive a vote from far neighbour,
	* It is ok, not to consider a vote once I become a leader.
	* */
	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {

	}

	/*
	* Todo:Harish Will respond with LeaderIs message with my NodeId
	* */
	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {

	}

	/*
	* Todo:Harish, forward the heart beat message to followerHealthMonitor, he is in need of it to keep
	* track of my followers status.
	* Also add the follower to my list of follower's as part of beat messages.
	* Health monitor will later remove follower if he don't respond for my heat beat
	* */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {

	}

	/*Todo:Harish, release all the resources. In this case it is only followerMonitor I see*/
	@Override
	public void beforeStateChange() {

	}

	@Override
	public void afterStateChange() {
		followerMonitor = new FollowerHealthMonitor(this, state,
			state.getConf().getElectionTimeout());
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
