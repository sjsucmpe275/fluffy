package election;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeMonitor;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import util.TimeoutListener;
import util.Timer;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Follower implements INodeState, TimeoutListener, LeaderHealthListener {

	private static final Logger logger = LoggerFactory.getLogger("Follower");
	private static final Random random = new Random();

	private Timer timer;
	private LeaderHealthMonitor leaderMonitor;
	private ServerState state;
	private EdgeMonitor edgeMonitor;
	private int nodeId;
	//private ConcurrentHashMap<Integer, Object> visitedNodesMap;
	private ElectionUtil util;

	public Follower(ServerState serverState) {
		this.state = serverState;
		this.util = new ElectionUtil();
		this.edgeMonitor = state.getEmon();
		this.nodeId = state.getConf().getNodeId();
		timer = new Timer(this, state.getConf().getElectionTimeout() 
				+ random.nextInt(200));
		timer.startTimer();
		//Todo:Harish Should we broad cast who is leader message here ?
	}

	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {

		logger.info("Replying to :" + workMessage.getHeader().getNodeId());
		edgeMonitor.broadcastMessage(util.createSizeIsMessage(nodeId, 
				workMessage.getHeader().getNodeId()));
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = edgeMonitor
				.getOutboundEdges().getEdgesMap();
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(util
						.createGetClusterSizeMessage(nodeId, destinationId));
			}
		}
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("SIZE IS MESSAGE IN FOLLOWER...");
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		if (state.getElectionId() < workMessage.getLeader().getElectionId()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());
		}
	}

	/* Todo: Harish Should reset election timer once I give my vote in new term */
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		logger.info("VOTE REQUEST RECEIVED...");
		if (workMessage.getLeader().getElectionId() > state.getElectionId()) {
			VoteMessage vote = new VoteMessage(nodeId,
				workMessage.getLeader().getElectionId(),
				workMessage.getLeader().getLeaderId());
			state.getEmon().broadcastMessage(vote.getMessage());
		}
	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
		channel.writeAndFlush (util.createLeaderIsMessage (state));
	}

	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		// Updating heartbeat
		// TODO: Update visited nodes map
		leaderMonitor.onBeat(System.currentTimeMillis());
		timer.cancel();
	}

	@Override
	public void beforeStateChange() {
		logger.info("Canceling timer task in follower..");
		timer.cancel();

		if (leaderMonitor != null) {
			logger.info("Canceling leader health task in follower..");
			leaderMonitor.cancel();
		}
	}

	@Override
	public void afterStateChange() {
		leaderMonitor = new LeaderHealthMonitor(this,
			state.getConf().getHeartbeatDt(), "Follower-LeaderHealthMonitor");
		leaderMonitor.start();
	}

	private void onElectionTimeout() {
		logger.info("ELECTION TIMED OUT!");
		state.setState(NodeStateEnum.CANDIDATE);
	}

	@Override
	public void notifyTimeout() {
		onElectionTimeout();
	}

	@Override
	public void onLeaderBadHealth() {
		logger.info("Leader dead.. Going for random time out..");
		timer.cancel();
		timer = new Timer(this, 150 + random.nextInt(150));
		timer.startTimer();
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
			leaderStatus.setAction(LeaderQuery.VOTERESPONSE);
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
	}}
