package election;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.messages.wrk_messages.LeaderStatusMessage;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.election.Election;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import util.TimeoutListener;
import util.Timer;

import java.util.Random;

public class Follower implements INodeState, TimeoutListener, LeaderHealthListener {

	private static final Logger logger = LoggerFactory.getLogger("Follower");
	private static final Random random = new Random();

	private Timer timer;
	private LeaderHealthMonitor leaderMonitor;
	private ServerState state;
	private EdgeMonitor edgeMonitor;
	//private ConcurrentHashMap<Integer, Object> visitedNodesMap;
	private ElectionUtil util;
	private int nodeId;

	public Follower(ServerState serverState) {
		this.state = serverState;
		this.util = new ElectionUtil();
		this.edgeMonitor = state.getEmon();
		this.nodeId = state.getConf().getNodeId();

		/*Creating Leader Monitor, But will monitor beats only when I learn about Leader in the network*/
		leaderMonitor = new LeaderHealthMonitor (this, state.getConf ().getElectionTimeout ());
		leaderMonitor.start ();

		/*Initially I will always be in Follower State, and wait for some random time before going into Candidate State*/
		timer = new Timer (this, getElectionTimeout (), "Follower - Election Timer");
		timer.start ();
	}

	private int getElectionTimeout() {
		return state.getConf().getElectionTimeout() + 150 + random.nextInt(150);
	}

	public void handleCmdQuery(WorkMessage wrkMessage, Channel channel) {

		System.out.println("Carrying out command:" + wrkMessage.getTask()
			.getTaskMessage().getQuery().getKey());
		Task.Builder t = Task.newBuilder();
		t.setSeqId(wrkMessage.getTask().getTaskMessage().getQuery().getSequenceNo());
		t.setSeriesId(wrkMessage.getTask().getTaskMessage().getQuery().getKey()
			.hashCode());
		t.setTaskMessage(wrkMessage.getTask().getTaskMessage());
		state.getTasks().addTask(t.build());
	}
	
	@Override
	public void handleCmdResponse(WorkMessage workMessage, Channel channel) {
		// If message reach this point. It should be transferred to command server
		try {
			state.getQueues().getFromWorkServer().put(workMessage.getTask().getTaskMessage());
		} catch (InterruptedException e) {
			// Enqueue failure message
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

		/*if(workMessage.getHeader ().getNodeId () == state.getLeaderId ())   {
			System.out.println("Receiving messages to calculate size again from my leader");
			return;
		}
*/
		System.out.println("Replying to :" + workMessage.getHeader().getNodeId());

		state.getEmon ().broadcastMessage(util.createSizeIsMessage(state,
				workMessage.getHeader().getNodeId()));
	}

	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~Follower - Handle Size Is Event~~~~~~~~~");
	}

	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~Follower - Handler Leader Is Event ");
		int incomingTerm = workMessage.getLeader().getElectionId();
		int currentTerm = state.getElectionId ();

		System.out.println("~~~~~~~~~Follower - New Term: " + incomingTerm);
		System.out.println("~~~~~~~~~Follower - Current Term: " + currentTerm);

		int inComingLeader = workMessage.getLeader ().getLeaderId ();
		int myLeader = state.getLeaderId ();

		System.out.println("~~~~~~~~~Follower - In Coming Leader Id: " + inComingLeader);
		System.out.println("~~~~~~~~~Follower - My Leader Id: " + myLeader);

	/*
	* I check if I got this message from a new leader with new terms
	* */
		if (incomingTerm >  currentTerm && inComingLeader != myLeader) {
			System.out.println("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(incomingTerm);
			state.setLeaderId(inComingLeader);

			/*Once I get a new leader, I will cancel my previous leader monitor task and start it again for
			* new monitor*/
			//leaderMonitor.cancel ();
			long currentTime = System.currentTimeMillis ();

			state.setLeaderHeartBeatdt (currentTime);
			leaderMonitor.onBeat (currentTime); //Updating the beat time of leader once I receive Heart Beat and then I start to monitor

			//leaderMonitor.start ();
		}
	}

	/* As part of this event,  Follower will vote if he has not voted in this term and update him term */
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		System.out.println("~~~~~~~~~Follower - Handler Vote Request Event ");

		if (workMessage.getLeader().getElectionId() > state.getElectionId()/* &&
				workMessage.getLeader ().getLeaderId () != state.getVotedFor ()*/) {

			state.setElectionId (workMessage.getLeader ().getElectionId ());
			state.setLeaderId (workMessage.getLeader ().getLeaderId ());

			VoteResponse vote = new VoteResponse (state.getConf ().getNodeId (),
				workMessage.getLeader().getElectionId(),
				workMessage.getLeader().getLeaderId());

			//Forward to the destination node who requested for the vote..
			vote.setDestination (workMessage.getHeader ().getNodeId ());
			vote.setMaxHops (state.getConf ().getMaxHops ());

			state.setLeaderHeartBeatdt (System.currentTimeMillis ());

			//Reply to the person who sent request
			channel.writeAndFlush (vote.getMessage ());

			// Broadcast the message to outbound edges.
			// Because if my in bound edge is down, I am trying to reach my candidate in different path - saying I have voted for him
			state.getEmon().broadCastOutBound (vote.getMessage());
			state.setVotedFor (workMessage.getHeader ().getNodeId ());

			// Reset timer, so that if nobody becomes leader in near by future I can go to Candidate state.
			timer.cancel ();
			System.out.println("******** Restarting the timer, once I have given my vote*********");
			timer.start ();
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
		System.out.println("~~~~~~~~~Follower - Handle Leader Heart Beat ");
		/* There might be a timer waiting for response from leader, If I am here I assume I got response from leader
		* and cancel my previously started timer*/
		timer.cancel();

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		int newLeaderId = workMessage.getLeader ().getLeaderId ();
		int currentLeaderId = state.getLeaderId ();

		System.out.println("Follower - New Term: " + inComingTerm);
		System.out.println("Follower - Current Term: " + currentTerm);

		System.out.println("Follower - New LeaderId: " +newLeaderId);
		System.out.println("Follower - Current LeaderId: " + currentLeaderId);

		if(inComingTerm > currentTerm && newLeaderId != currentLeaderId)   {
			state.setElectionId (inComingTerm);
			state.setLeaderId (newLeaderId);
			// Reset Leader Monitor Task
			//leaderMonitor.cancel ();

			long currentTime = System.currentTimeMillis ();

			state.setLeaderHeartBeatdt (currentTime);
			leaderMonitor.onBeat (currentTime); //Updating the beat time of leader once I receive Heart Beat and then I start to monitor
			//leaderMonitor.start ();

			//Return the response..
			LeaderStatusMessage leaderBeatResponse = new LeaderStatusMessage (state.getConf ().getNodeId ());
			leaderBeatResponse.setLeaderId (newLeaderId);
			leaderBeatResponse.setDestination (newLeaderId);
			leaderBeatResponse.setMaxHops (state.getConf ().getMaxHops ());
			leaderBeatResponse.setLeaderAction (Election.LeaderStatus.LeaderQuery.BEAT);
			leaderBeatResponse.setLeaderState (Election.LeaderStatus.LeaderState.LEADERALIVE);

			//Write it back to the channel from where I received it...
			channel.writeAndFlush (leaderBeatResponse.getMessage ());

			//Also update other nodes in outbound about the leader heart beat..
			state.getEmon ().broadCastOutBound (leaderBeatResponse.getMessage ());

			return;
		}

		// I check for -1 because, If I go down and come up then I wont be having any leader...
		if(inComingTerm == currentTerm &&
				(newLeaderId == currentLeaderId || currentLeaderId == -1))    {

			if(currentLeaderId ==  -1)  {
				state.setLeaderId (newLeaderId);
			}

			// Reset Leader Monitor Task
			//leaderMonitor.cancel ();

			long currentTime = System.currentTimeMillis ();

			// Updating heartbeat..
			state.setLeaderHeartBeatdt (currentTime);
			leaderMonitor.onBeat(currentTime);
			//leaderMonitor.start ();

			//Return the response..
			LeaderStatusMessage leaderBeatResponse = new LeaderStatusMessage (state.getConf ().getNodeId ());
			leaderBeatResponse.setLeaderId (newLeaderId);
			leaderBeatResponse.setDestination (newLeaderId);
			leaderBeatResponse.setMaxHops (state.getConf ().getMaxHops ());
			leaderBeatResponse.setLeaderAction (Election.LeaderStatus.LeaderQuery.BEAT);
			leaderBeatResponse.setLeaderState (Election.LeaderStatus.LeaderState.LEADERALIVE);

			//Write it back to the channel from where I received it...
			channel.writeAndFlush (leaderBeatResponse.getMessage ());

			//Also update other nodes in outbound about the leader heart beat..
			state.getEmon ().broadCastOutBound (leaderBeatResponse.getMessage ());

			return;
		}
		// TODO: Update visited nodes map
	}

	@Override
	public void beforeStateChange() {
		System.out.println("~~~~~~~~Follower - Before State Change");
		timer.cancel();

		leaderMonitor.onBeat (Long.MAX_VALUE);
		//leaderMonitor.cancel();
	}

	@Override
	public void afterStateChange() {

		//leaderMonitor.cancel ();

		long leaderHeartBeatDt = state.getLeaderHeartBeatdt ();

		if(leaderHeartBeatDt == Long.MAX_VALUE) {
			leaderHeartBeatDt = System.currentTimeMillis ();
		}

		// It might be the case, I came from either Candidate State or Leader State after receiving HB
		leaderMonitor.onBeat (leaderHeartBeatDt);
		//leaderMonitor.start();
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
		timer.cancel ();
		System.out.println("~~~~~~~~Follower - On Leader Bad Health Event");
		timer.start (getRandomTimeout ());
	}

	private int getRandomTimeout() {
		return 150 + random.nextInt(150);
	}
}
