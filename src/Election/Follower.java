package Election;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;

public class Follower implements INodeState {

	private long lastHeartBeatValue;
	private Timer timer;
	private ServerState state;
	private ConcurrentHashMap<Integer, Object> visitedNodesMap;

	public Follower(ServerState serverState) {
		this.state = serverState;
	}

	protected long getDelay() {
		return state.getConf().getElectionTimeout();
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			System.out.println("Replying to :" + workMessage.getHeader().getNodeId());
			state.getEmon().broadcastMessage(createSizeIsMessage(workMessage.getHeader().getNodeId()));
			ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon().getOutboundEdges().getEdgesMap();
			for (Integer nodeId : edgeMap.keySet()) {
				EdgeInfo edge = edgeMap.get(nodeId);
				if (edge.isActive() && edge.getChannel() != null) {
					edge.getChannel().writeAndFlush(createMessage(workMessage.getHeader().getNodeId(), nodeId));
				}
			}
			break;
		case SIZEIS:
			System.out.println("SIZE IS MESSAGE IN FOLLOWER...");
			break;
		case THELEADERIS:
			break;
		case VOTEREQUEST:
			if (workMessage.getLeader().getElectionId() > state.getElectionId()) {
				VoteMessage vote = new VoteMessage(state.getConf().getNodeId(), workMessage.getLeader().getElectionId(),
						workMessage.getLeader().getLeaderId());
				state.getEmon().broadcastMessage(vote.getMessage());
			}
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

	@Override
	public void stateChanged() {
		// Start the timer
//		timer.scheduleAtFixedRate(new ElectionTimer(this), 0, getDelay());
	}

	public long getLastHeartbeat() {
		return lastHeartBeatValue;
	}

	public void setLastHeartbeat(long lastHeartbeat) {
		this.lastHeartBeatValue = lastHeartbeat;
	}

	public void isElectionNeeded(boolean result) {
		if (result) {
			timer.cancel();
			state.setState(NodeStateEnum.CANDIDATE);
		}
	}

	private class VoteMessage {
		private WorkState.Builder workState;
		private LeaderState leaderState;
		private Common.Header.Builder header;
		private LeaderStatus.Builder leaderStatus;
		private int nodeId;
		private int destination = -1; // By default Heart Beat Message will be
										// sent to all Nodes..
		private int secret = 1;
		private int electionId;
		/*
		 * 
		 * optional string leader_host= 3; optional int32 leader_id = 4;
		 * optional int32 election_id = 5; optional int32 votedFor = 6; optional
		 * bool voteGranted = 7;
		 */

		public VoteMessage(int nodeId, int electionId, int VoteFor) {
			this.nodeId = nodeId;
			workState = WorkState.newBuilder();
			workState.setEnqueued(-1);
			workState.setProcessed(-1);
			leaderState = LeaderState.LEADERDEAD;
			header = Common.Header.newBuilder();
			header.setNodeId(nodeId);
			header.setDestination(destination);
			header.setMaxHops(1);
			header.setTime(System.currentTimeMillis());
			this.electionId = electionId;
			leaderStatus = LeaderStatus.newBuilder();
			leaderStatus.setElectionId(electionId);
			leaderStatus.setVotedFor(VoteFor);
			leaderStatus.setVoteGranted(true);
		}

		public WorkMessage getMessage() {

			header.setTime(System.currentTimeMillis());

			WorkMessage.Builder workMessage = WorkMessage.newBuilder();
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
}
