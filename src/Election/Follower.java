package Election;

import java.util.concurrent.ConcurrentHashMap;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.work.Work.WorkMessage;

public class Follower implements INodeState {

	int lastHeartBeatValue;
	private ServerState state;
	ElectionTimer timer;
	private ConcurrentHashMap<Integer, Object> visitedNodesMap;
	
	public Follower(){
		//this.timer=;
	}
	
	public Follower(ServerState state){
		this.state=state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			System.out.println("Replying to :"+workMessage.getHeader().getNodeId());
			state.getEmon().broadcastMessage(createSizeIsMessage(workMessage.getHeader().getNodeId())) ;
			ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon().getOutboundEdges().getEdgesMap();
			for (Integer nodeId : edgeMap.keySet()) {
				EdgeInfo edge = edgeMap.get(nodeId);
				if (edge.isActive() && edge.getChannel() != null) {
					edge.getChannel().writeAndFlush(createMessage(workMessage.getHeader().getNodeId(),nodeId));
				}
			}
			break;
		case SIZEIS:
			System.out.println("SIZE IS MESSAGE IN FOLLOWER...");
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
	public WorkMessage createMessage(int source, int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(source);
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
	public void getClusterSize() {
		
	}
}
