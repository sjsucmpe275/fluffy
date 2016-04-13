package election;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.messages.wrk_messages.LeaderStatusMessage;
import io.netty.channel.Channel;
import pipe.election.Election;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
import storage.Storage.Query;
import util.TimeoutListener;
import util.Timer;

public class Follower implements INodeState, TimeoutListener, LeaderHealthListener {

	private static final Logger logger = LoggerFactory.getLogger("Follower");
	private static final Random random = new Random();

	private Timer timer;
	private LeaderHealthMonitor leaderMonitor;
	private ServerState state;
	private ElectionUtil util;

	public Follower(ServerState serverState) {
		this.state = serverState;
		this.util = new ElectionUtil();

		// Creating Leader Monitor, But will monitor beats only when I learn
		// about Leader in the network
		leaderMonitor = new LeaderHealthMonitor(this,
			state.getConf().getElectionTimeout());
		leaderMonitor.start();
		
		// Initially I will always be in Follower State, and wait for some
		// random time before going into Candidate State
		timer = new Timer(this, getElectionTimeout(), "Follower - Election Timer");
		timer.start();
	}

	private int getElectionTimeout() {
		return state.getConf().getElectionTimeout() + 150 + random.nextInt(150);
	}

	public void handleCmdQuery(WorkMessage wrkMessage, Channel channel) {
		Query query = wrkMessage.getTask().getTaskMessage().getQuery();
		logger.info("Carrying out command:" + query.getKey());
		Task.Builder t = Task.newBuilder();
		t.setSeqId(query.getSequenceNo());
		t.setSeriesId(query.getKey().hashCode());
		t.setTaskMessage(wrkMessage.getTask().getTaskMessage());
		state.getTasks().addTask(t.build());
	}

	@Override
	public void handleCmdResponse(WorkMessage workMessage, Channel channel) {

		try {
			CommandMessage taskMessage = workMessage.getTask().getTaskMessage();
			switch (taskMessage.getResponse().getAction()) {
			case GET:
			case STORE:
				// I have this condition because even TaskWorker can call
				// handleCommandMessage...
				if (workMessage.getHeader().getDestination() == state.getConf()
					.getNodeId()) {
					state.getQueues().getFromWorkServer().put(taskMessage);
				} else {
					state.getEmon().broadcastMessage(workMessage);
				}
				break;
			default:
				state.getQueues().getFromWorkServer().put(taskMessage);
				break;
			}
		} catch (InterruptedException e) {
			// TODO Enqueue failure message
			e.printStackTrace();
		}
	}

	@Override
	public void handleCmdError(WorkMessage workMessage, Channel channel) {
		// This should handle same as response message
		handleCmdResponse(workMessage, channel);
	}

	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		logger.info("Replying to :" + workMessage.getHeader().getNodeId());

		state.getEmon().broadcastMessage(
			util.createSizeIsMessage(state, workMessage.getHeader().getNodeId()));
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~Follower - Handle Size Is Event~~~~~~~~~");
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~~Follower - Handler Leader Is Event ");
		int incomingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId();

		logger.info("~~~~~~~~~Follower - New Term: " + incomingTerm);
		logger.info("~~~~~~~~~Follower - Current Term: " + currentTerm);

		int inComingLeader = workMessage.getLeader().getLeaderId();
		int myLeader = state.getLeaderId();

		logger.info("~~~~~~~~~Follower - In Coming Leader Id: " + inComingLeader);
		logger.info("~~~~~~~~~Follower - My Leader Id: " + myLeader);

		// I check if I got this message from a new leader with new terms

		if (incomingTerm > currentTerm && inComingLeader != myLeader) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(incomingTerm);
			state.setLeaderId(inComingLeader);
			
			// Once I get a new leader, I will cancel my previous leader monitor
			// task and start it again for new monitor
			 
			long currentTime = System.currentTimeMillis();

			state.setLeaderHeartBeatdt(currentTime);
			leaderMonitor.onBeat(currentTime); 
			// Updating the beat time of leader once I receive Heart Beat and
			// then I start to monitor
		}
	}

	/**
	 * As part of this event, Follower will vote if he has not voted in this
	 * term and update him term
	 */
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~~Follower - Handler Vote Request Event ");

		if (workMessage.getLeader().getElectionId() > state.getElectionId()) {

			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());

			VoteResponse vote = new VoteResponse(state.getConf().getNodeId(),
				workMessage.getLeader().getElectionId(),
				workMessage.getLeader().getLeaderId());

			// Forward to the destination node who requested for the vote..
			vote.setDestination(workMessage.getHeader().getNodeId());
			vote.setMaxHops(state.getConf().getMaxHops());

			state.setLeaderHeartBeatdt(System.currentTimeMillis());

			// Reply to the person who sent request
			channel.writeAndFlush(vote.getMessage());

			// Broadcast the message to outbound edges.
			// Because if my in bound edge is down, I am trying to reach my
			// candidate in different path - saying I have voted for him
			state.getEmon().broadCastOutBound(vote.getMessage());
			state.setVotedFor(workMessage.getHeader().getNodeId());

			// Reset timer, so that if nobody becomes leader in near by future I
			// can go to Candidate state.
			timer.cancel();
			logger.info(
				"******** Restarting the timer, once I have given my vote*********");
			timer.start();
		}
	}

	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	/**
	 * I do nothing because, node that is sending this message will receive
	 * Heart Beat/Leader Is message in future by my leader
	 */
	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/**
	 * if it is equal or higher then notify leaderMonitor. and reset timer.
	 * (only if leaderId and electionId is same) if it is lesser then drop it,
	 */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~~Follower - Handle Leader Heart Beat ");
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

		logger.info("Follower - New Term: " + inComingTerm);
		logger.info("Follower - Current Term: " + currentTerm);

		logger.info("Follower - New LeaderId: " + newLeaderId);
		logger.info("Follower - Current LeaderId: " + currentLeaderId);

		if (inComingTerm > currentTerm && newLeaderId != currentLeaderId) {
			state.setElectionId(inComingTerm);
			state.setLeaderId(newLeaderId);

			long currentTime = System.currentTimeMillis();

			state.setLeaderHeartBeatdt(currentTime);
			leaderMonitor.onBeat(currentTime); 
			// Updating the beat time of leader once I receive Heart Beat and
			// then I start to monitor

			// Return the response..
			LeaderStatusMessage leaderBeatResponse = new LeaderStatusMessage(
				state.getConf().getNodeId());
			leaderBeatResponse.setLeaderId(newLeaderId);
			leaderBeatResponse.setDestination(newLeaderId);
			leaderBeatResponse.setMaxHops(state.getConf().getMaxHops());
			leaderBeatResponse.setLeaderAction(Election.LeaderStatus.LeaderQuery.BEAT);
			leaderBeatResponse
				.setLeaderState(Election.LeaderStatus.LeaderState.LEADERALIVE);

			// Write it back to the channel from where I received it...
			channel.writeAndFlush(leaderBeatResponse.getMessage());
			
			// Also update other nodes in outbound about the leader heart beat..
			state.getEmon().broadCastOutBound(leaderBeatResponse.getMessage());
			return;
		}

		// I check for -1 because, If I go down and come up then I wont be
		// having any leader...
		if (inComingTerm == currentTerm	
				&& (newLeaderId == currentLeaderId || currentLeaderId == -1)) {

			if (currentLeaderId == -1) {
				state.setLeaderId(newLeaderId);
			}

			long currentTime = System.currentTimeMillis();

			// Updating heartbeat..
			state.setLeaderHeartBeatdt(currentTime);
			leaderMonitor.onBeat(currentTime);

			// Return the response..
			LeaderStatusMessage leaderBeatResponse = new LeaderStatusMessage(
				state.getConf().getNodeId());
			leaderBeatResponse.setLeaderId(newLeaderId);
			leaderBeatResponse.setDestination(newLeaderId);
			leaderBeatResponse.setMaxHops(state.getConf().getMaxHops());
			leaderBeatResponse.setLeaderAction(Election.LeaderStatus.LeaderQuery.BEAT);
			leaderBeatResponse.setLeaderState(Election.LeaderStatus.LeaderState.LEADERALIVE);

			// Write it back to the channel from where I received it...
			channel.writeAndFlush(leaderBeatResponse.getMessage());

			// Also update other nodes in outbound about the leader heart beat..
			state.getEmon().broadCastOutBound(leaderBeatResponse.getMessage());
		}
		// TODO: Update visited nodes map
	}

	@Override
	public void beforeStateChange() {
		logger.info("~~~~~~~~Follower - Before State Change");
		timer.cancel();
		leaderMonitor.onBeat(Long.MAX_VALUE);
	}

	@Override
	public void afterStateChange() {
		long leaderHeartBeatDt = state.getLeaderHeartBeatdt();

		if (leaderHeartBeatDt == Long.MAX_VALUE) {
			leaderHeartBeatDt = System.currentTimeMillis();
		}

		// It might be the case, I came from either Candidate State or Leader
		// State after receiving HB
		leaderMonitor.onBeat(leaderHeartBeatDt);
	}

	private void onElectionTimeout() {
		logger.info("******ELECTION TIMED OUT******");
		state.setState(NodeStateEnum.CANDIDATE);
	}

	@Override
	public void notifyTimeout() {
		onElectionTimeout();
	}

	@Override
	public void onLeaderBadHealth() {
		timer.cancel();
		logger.info("~~~~~~~~Follower - On Leader Bad Health Event");
		timer.start(getRandomTimeout());
	}

	private int getRandomTimeout() {
		return 150 + random.nextInt(150);
	}
}
