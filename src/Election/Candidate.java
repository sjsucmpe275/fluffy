package Election;

import java.util.concurrent.ConcurrentHashMap;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeMonitor;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.work.Work.WorkMessage;

public class Candidate implements INodeState {
	int VoteCount;
	int sizeOfCluster;
	private ServerState state;
	private EdgeMonitor edgeMonitor;
	private ConcurrentHashMap<Integer, Object> visitedNodesMap;
	private long clusterSizeTimeout;

	public Candidate(ServerState state) {
		this.state = state;
		this.visitedNodesMap = new ConcurrentHashMap<>();
	}

	public Candidate() {
	}

	public void requestVote() {
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			System.out.println("Replying to :" + workMessage.getHeader().getNodeId());
			channel.writeAndFlush(createSizeIsMessage(workMessage.getHeader().getNodeId()));

			break;
		case SIZEIS:
			System.out.println("Getting size is:" + workMessage.getHeader().getNodeId());
			visitedNodesMap.put(workMessage.getHeader().getNodeId(), new Object());
			System.out.println("Visited Nodes Hashmap:");
			System.out.println(visitedNodesMap);
			break;
		case THELEADERIS:
			break;
		case VOTEREQUEST:
			break;
		case VOTERESPONSE:
			break;
		case WHOISTHELEADER:
			break;
		default:
		}
	}

	public void getClusterSize() {
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon().getOutboundEdges().getEdgesMap();
		for (Integer nodeId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(nodeId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(createMessage(nodeId));
			}
		}
	}

	public WorkMessage createSizeIsMessage(int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(state.getConf().getNodeId());
		header.setDestination(destination);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.SIZEIS);

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();

	}

	public int returnClusterSize() {
		return visitedNodesMap.size();
	}

	public WorkMessage createMessage(int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(state.getConf().getNodeId());
		header.setDestination(destination);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.GETCLUSTERSIZE);

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	@Override
	public void stateChanged() {

	}
}
