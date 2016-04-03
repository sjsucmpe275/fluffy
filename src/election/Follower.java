package election;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeMonitor;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.work.Work.WorkMessage;
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
		leaderMonitor = new LeaderHealthMonitor (this, state.getConf ().getHeartbeatDt ());
		timer = new Timer(this, getElectionTimeout ());
		timer.startTimer();
	}

	private int getElectionTimeout() {
		return state.getConf().getElectionTimeout()
				+ random.nextInt(200);
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

	/*
	* I check if I got this message from a new leader with new terms
	* */
		if (workMessage.getLeader().getElectionId() >  state.getElectionId() &&
				workMessage.getLeader ().getLeaderId () != state.getLeaderId ()) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			/*Once I get a new leader, I will cancel my previous leader monitor task and start it again for
			* new monitor*/
			leaderMonitor.cancel ();
			leaderMonitor.start ();
		}
	}

	/* As part of this event,  Follower will vote if he has not voted in this term and update him term */
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		logger.info("VOTE REQUEST RECEIVED...");
		if (workMessage.getLeader().getElectionId() > state.getElectionId()/* &&
				workMessage.getLeader ().getLeaderId () != state.getVotedFor ()*/) {
			state.setElectionId (workMessage.getLeader ().getElectionId ());
			VoteMessage vote = new VoteMessage(nodeId,
				workMessage.getLeader().getElectionId(),
				workMessage.getLeader().getLeaderId());
			state.getEmon().broadcastMessage(vote.getMessage());

			// Reset timer, so that if nobody becomes leader in near by future I can go to Candidate state.
			timer.resetTimer (this, getElectionTimeout ());
		}
	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	/*
	* I do nothing because, node that is sending this message will receive Heart Beat/Leader Is message
	* in future by my leader
	* */
	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/* if it is equal or higher then notify leaderMonitor. and reset timer. (only if leaderId and electionId is same)
	* if it is lesser then drop it, */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		/* There might be a timer waiting for response from leader, If I am here I assume I got response from leader
		* and cancel my previously started timer*/
		timer.cancel();

		int newTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		int newLeaderId = workMessage.getLeader ().getLeaderId ();
		int currentLeaderId = state.getLeaderId ();

		if(newTerm > currentTerm && newLeaderId > currentLeaderId)   {
			state.setElectionId (newTerm);
			state.setLeaderId (newLeaderId);
			// Reset Leader Monitor Task
			leaderMonitor.cancel ();
			leaderMonitor.start ();
		}

		if(newTerm == currentTerm && newLeaderId == currentLeaderId)    {
			// Updating heartbeat
			leaderMonitor.onBeat(System.currentTimeMillis());
		}
		// TODO: Update visited nodes map
	}

	@Override
	public void beforeStateChange() {
		logger.info("Canceling timer task in follower..");
		timer.cancel();

		logger.info("Canceling leader health task in follower..");
		leaderMonitor.cancel();
	}

	@Override
	public void afterStateChange() {
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
		timer.resetTimer (this, getRandomTimeout ());
	}

	private int getRandomTimeout() {
		return 150 + random.nextInt(150);
	}
}
