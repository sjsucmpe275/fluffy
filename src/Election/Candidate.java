package Election;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.webkit.ThemeClient;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

public class Candidate implements INodeState, TimeoutListener {
	private int requiredVotes;
	private int sizeOfCluster;
	private ServerState state;
	private ConcurrentHashMap<Integer, Object> visitedNodes;
	private Timer timer;
	private ConcurrentHashMap<Integer, Object> votes;
	private final Object theObject = new Object();

	public Candidate(ServerState state) {
		this.state = state;
		this.visitedNodes = new ConcurrentHashMap<Integer, Object>();
		this.votes = new ConcurrentHashMap<Integer, Object>();
	}

	public void getClusterSize() {
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon().getOutboundEdges().getEdgesMap();
		for (Integer nodeId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(nodeId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(createGetClusterSizeMessage(nodeId));
			}
		}

		timer = new Timer(this, state.getConf().getElectionTimeout());
		timer.startTimer();
	}

	@Override
	public synchronized void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			System.out.println("Replying to :" + workMessage.getHeader().getNodeId());
			channel.writeAndFlush(createSizeIsMessage(workMessage.getHeader().getNodeId()));
			break;
		case SIZEIS:
			System.out.println("Getting size is:" + workMessage.getHeader().getNodeId());
			visitedNodes.put(workMessage.getHeader().getNodeId(), theObject);
			System.out.print("Visited Nodes Hashmap:");
			System.out.println(visitedNodes);
			break;
		case THELEADERIS:
			break;
		case VOTEREQUEST:
			break;
		case VOTERESPONSE:
			System.out.println("Receiving Vote Response from :" + workMessage.getHeader().getNodeId());

			LeaderStatus leader = workMessage.getLeader();
			if (leader.getVotedFor() == state.getLeaderId() && leader.getVoteGranted()) {
				votes.put(workMessage.getHeader().getNodeId(), theObject);
				System.out.println(votes);
			}

			break;
		case WHOISTHELEADER:
			break;
		default:
		}
	}

	@Override
	public void stateChanged() {
		clear();
		getClusterSize();
	}

	@Override
	public void notifyTimeout() {
		sizeOfCluster = visitedNodes.size() + 1;
		requiredVotes = (int) Math.round((sizeOfCluster / 2.0) + 0.5f);
		System.out.println("###############################");
		System.out.println("Size of the network is" + sizeOfCluster);
		System.out.println("Required vote count is" + requiredVotes);
		System.out.println("###############################");
		startElection();
		timer = null;
		timer = new Timer(new TimeoutListener() {

			@Override
			public void notifyTimeout() {
				state.getCurrentState();
				System.out.println("#########################");
				System.out.println("Election is over..");
				System.out.println("#########################");

				if (votes.size() >= requiredVotes) {
					state.getEmon().broadcastMessage(createLeaderIsMessage());
					System.out.println("State is leader now..");
					state.setState(NodeStateEnum.LEADER);
				}

				clear();
			}
		}, state.getConf().getElectionTimeout());
		timer.startTimer();
	}

	private WorkMessage createSizeIsMessage(int destination) {
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

	private WorkMessage createGetClusterSizeMessage(int destination) {
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

	private WorkMessage createVoteRequest() {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(state.getConf().getNodeId());
		header.setDestination(-1);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.VOTEREQUEST);
		System.out.println(state.getElectionId());
		leaderStatus.setElectionId(state.getElectionId());
		leaderStatus.setLeaderId(state.getConf().getNodeId());

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	private WorkMessage createLeaderIsMessage() {

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(state.getConf().getNodeId());
		header.setDestination(-1);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.THELEADERIS);
		System.out.println(state.getElectionId());
		leaderStatus.setElectionId(state.getElectionId());
		leaderStatus.setLeaderId(state.getConf().getNodeId());
		leaderStatus.setState(LeaderState.LEADERALIVE);
		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	private void startElection() {
		state.setElectionId(state.getElectionId() + 1);
		state.getEmon().broadcastMessage(createVoteRequest());
	}

	private void clear() {
		votes.clear();
		sizeOfCluster = 0;
		requiredVotes = 0;
		visitedNodes.clear();
	}
}
