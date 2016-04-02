package election;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class Leader implements INodeState, FollowerListener {

	private final Logger logger = LoggerFactory.getLogger("Leader");

	private ServerState state;
	private int nodeId;
	private ArrayList<Integer> activeNodes;
	private FollowerHealthMonitor followerMonitor;
	private ElectionUtil util;

	public Leader(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.activeNodes = new ArrayList<>();
		this.util = new ElectionUtil();
	}

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

	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {

	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {

	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {

	}

	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {

	}

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
	public void onNewOrHigherTerm() {

	}

	@Override
	public void onLeaderDiscovery() {

	}

	@Override
	public void onHigherTerm() {

	}

	@Override
	public void addFollower(int followerId) {
		activeNodes.add(followerId);
	}

	@Override
	public void removeFollower(int followerId) {
		activeNodes.remove(followerId);
	}
}
