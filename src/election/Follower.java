package election;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.messages.wrk_messages.LeaderStatusMessage;
import io.netty.channel.Channel;
import pipe.election.Election;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

public class Follower
	implements INodeState, TimeoutListener, LeaderHealthListener {

	private static final Logger logger = LoggerFactory.getLogger("Follower");
	private static final Random random = new Random();

	private Timer timer;
	private LeaderHealthMonitor leaderMonitor;
	private ServerState state;
	private EdgeMonitor edgeMonitor;
	private int nodeId;
	// private ConcurrentHashMap<Integer, Object> visitedNodesMap;
	private ElectionUtil util;

	public Follower(ServerState serverState) {
		this.state = serverState;
		this.util = new ElectionUtil();
		this.edgeMonitor = state.getEmon();
		this.nodeId = state.getConf().getNodeId();

		/*
		 * Creating Leader Monitor, But will be started only when I learn about
		 * Leader in the network
		 */
		leaderMonitor = new LeaderHealthMonitor(this,
			state.getConf().getHeartbeatDt());

		/*
		 * Initially I will always be in Follower State, and wait for some
		 * random time before going into Candidate State
		 */
		timer = new Timer(this, getElectionTimeout(),
			"Follower - Election Timer");
		timer.start();
	}

	private int getElectionTimeout() {
		return state.getConf().getElectionTimeout() + 150 + random.nextInt(150);
	}

	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {

		System.out
			.println("Replying to :" + workMessage.getHeader().getNodeId());

		edgeMonitor.broadcastMessage(util.createSizeIsMessage(
			state.getConf().getNodeId(), workMessage.getHeader().getNodeId()));

		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = edgeMonitor
			.getOutboundEdges().getEdgesMap();

		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel()
					.writeAndFlush(util.createGetClusterSizeMessage(
						state.getConf().getNodeId(), destinationId));
			}
		}
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~Follower - Handle Size Is Event~~~~~~~~~");
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~Follower - Handler Leader Is Event ");
		int incomingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId();

		System.out.println("~~~~~~~~~Follower - New Term: " + incomingTerm);
		System.out.println("~~~~~~~~~Follower - Current Term: " + currentTerm);

		int inComingLeader = workMessage.getLeader().getLeaderId();
		int myLeader = workMessage.getLeader().getLeaderId();

		System.out.println(
			"~~~~~~~~~Follower - In Coming Leader Id: " + inComingLeader);
		System.out.println("~~~~~~~~~Follower - My Leader Id: " + myLeader);

		/*
		 * I check if I got this message from a new leader with new terms
		 */
		if (incomingTerm > currentTerm && inComingLeader != myLeader) {
			System.out
				.println("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			/*
			 * Once I get a new leader, I will cancel my previous leader monitor
			 * task and start it again for new monitor
			 */
			leaderMonitor.cancel();
			leaderMonitor.start();
		}
	}

	/*
	 * As part of this event, Follower will vote if he has not voted in this
	 * term and update him term
	 */
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~Follower - Handler Vote Request Event ");
		if (workMessage.getLeader().getElectionId() > state
			.getElectionId()/*
							 * && workMessage.getLeader ().getLeaderId () !=
							 * state.getVotedFor ()
							 */) {
			state.setElectionId(workMessage.getLeader().getElectionId());
			VoteMessage vote = new VoteMessage(state.getConf().getNodeId(),
				workMessage.getLeader().getElectionId(),
				workMessage.getLeader().getLeaderId());
			state.getEmon().broadcastMessage(vote.getMessage());

			// Reset timer, so that if nobody becomes leader in near by future I
			// can go to Candidate state.
			timer.cancel();
			timer.start();
		}
	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	/*
	 * I do nothing because, node that is sending this message will receive
	 * Heart Beat/Leader Is message in future by my leader
	 */
	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/*
	 * if it is equal or higher then notify leaderMonitor. and reset timer.
	 * (only if leaderId and electionId is same) if it is lesser then drop it,
	 */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~Follower - Handler Leader Heart Beat ");
		/*
		 * There might be a timer waiting for response from leader, If I am here
		 * I assume I got response from leader and cancel my previously started
		 * timer
		 */
		timer.cancel();

		int inComingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId();

		int newLeaderId = workMessage.getLeader().getLeaderId();
		int currentLeaderId = state.getLeaderId();

		System.out.println("Follower - New Term: " + inComingTerm);
		System.out.println("Follower - Current Term: " + currentTerm);

		System.out.println("Follower - New LeaderId: " + newLeaderId);
		System.out.println("Follower - Current LeaderId: " + currentLeaderId);

		if (inComingTerm > currentTerm && newLeaderId != currentLeaderId) {
			state.setElectionId(inComingTerm);
			state.setLeaderId(newLeaderId);
			// Reset Leader Monitor Task
			leaderMonitor.cancel();
			leaderMonitor.start();
			return;
		}

		// I check for -1 because, If I go down and come up then I wont be
		// having any leader...
		if (inComingTerm == currentTerm
			&& (newLeaderId == currentLeaderId || currentLeaderId == -1)) {
			// Updating heartbeat..
			leaderMonitor.onBeat(System.currentTimeMillis());

			// Return the response..
			LeaderStatusMessage leaderBeatResponse = new LeaderStatusMessage(
				state.getConf().getNodeId());
			leaderBeatResponse.setLeaderId(currentLeaderId);
			leaderBeatResponse.setDestination(currentLeaderId);
			leaderBeatResponse
				.setLeaderAction(Election.LeaderStatus.LeaderQuery.BEAT);
			leaderBeatResponse
				.setLeaderState(Election.LeaderStatus.LeaderState.LEADERALIVE);

			// Write it back to the channel from where I received it...
			channel.write(leaderBeatResponse.getMessage());

			return;
		}
		// TODO: Update visited nodes map
	}

	@Override
	public void beforeStateChange() {
		System.out.println("~~~~~~~~Follower - Before State Change");
		timer.cancel();

		leaderMonitor.cancel();
	}

	@Override
	public void afterStateChange() {
		// It might be the case, I came from either Follower State or Leader
		// State
		leaderMonitor.onBeat(state.getLeaderHeartBeatdt());
		leaderMonitor.start();
	}

	private void onElectionTimeout() {
		System.out.println("******ELECTION TIMED OUT******");
		state.setState(NodeStateEnum.CANDIDATE);
	}

	@Override
	public void notifyTimeout() {
		onElectionTimeout();
	}

	@Override
	public void onLeaderBadHealth() {
		System.out.println("~~~~~~~~Follower - On Leader Bad Health Event");
		timer.start(getRandomTimeout());
	}

	private int getRandomTimeout() {
		return 150 + random.nextInt(150);
	}
}
