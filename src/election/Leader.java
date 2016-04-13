package election;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.tasks.IReplicationStrategy;
import gash.router.server.tasks.RoundRobinStrategy;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class Leader implements INodeState, FollowerListener, ITaskListener {

	private final Logger logger = LoggerFactory.getLogger("Leader");

	private ServerState state;
	private int nodeId;
	private final Object theObject = new Object();
	private final ConcurrentHashMap<Integer, Object> activeNodes;
	private FollowerHealthMonitor followerMonitor;
	private ElectionUtil util;
	private IReplicationStrategy strategy;
	private ExecutorService service = Executors.newFixedThreadPool(3);
	private ConcurrentHashMap<String, GetTask> getTaskMap;
	private ConcurrentHashMap<String, StoreTask> storeTaskMap;
	
	public Leader(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.activeNodes = new ConcurrentHashMap<> ();
		this.followerMonitor = new FollowerHealthMonitor(this, state,
				state.getConf().getElectionTimeout());
		this.util = new ElectionUtil();
		this.strategy = new RoundRobinStrategy(2);
		this.getTaskMap = new ConcurrentHashMap<>();
		this.storeTaskMap = new ConcurrentHashMap<> ();
	}

	public synchronized void handleCmdQuery(WorkMessage wrkMessage, Channel channel) {

		logger.info("LEADER RECEIVED MESSAGE");
		CommandMessage taskMessage = wrkMessage.getTask().getTaskMessage();

		/*
		 * I am adding myself also as a Node, which will take part in
		 * storage/replication
		 */

		List<Integer> availableNodes = new ArrayList<>();
		availableNodes.add(nodeId);
		for (Integer i : activeNodes.keySet()) {
			availableNodes.add(i);
		}

		switch (taskMessage.getQuery().getAction()) {
		case GET:
			GetTask getTask = new GetTask(state, this, wrkMessage);
			service.submit(getTask);
			getTaskMap.put(taskMessage.getQuery().getKey(), getTask);
			break;
		case STORE:
			/*
			 * Store task should be created only once for a particular key,
			 * which will be mostly for Meta Data. For Data Chunks existing task
			 * will be used for replication and task will finish execution when
			 * all the chunks are stored...
			 */
			if (!storeTaskMap.containsKey(taskMessage.getQuery().getKey())) {
				StoreTask storeTask = new StoreTask(state, this, wrkMessage, strategy,
					availableNodes);
				service.submit(storeTask);
				storeTaskMap.put(taskMessage.getQuery().getKey(), storeTask);
				return;
			}

			String key = taskMessage.getQuery().getKey();
			if (storeTaskMap.containsKey(key)) {
				// Update the task with new message to broad cast
				storeTaskMap.get(key).handleRequest(wrkMessage, availableNodes);
				return;
			}
			break;
		case DELETE:
		case UPDATE:
		default:
			break;
		}
	}
	
	@Override
	public synchronized void handleCmdResponse(WorkMessage workMessage, Channel channel) {

		CommandMessage taskMessage = workMessage.getTask().getTaskMessage();
		switch (taskMessage.getResponse().getAction()) {

		case GET:
			if (getTaskMap.containsKey(taskMessage.getResponse().getKey())) {
				AbstractTask task = getTaskMap.get(taskMessage.getResponse().getKey());
				task.handleResponse(workMessage);
			}
			break;

		case STORE:
			if (storeTaskMap.containsKey(taskMessage.getResponse().getKey())) {
				AbstractTask task = storeTaskMap.get(taskMessage.getResponse().getKey());
				task.handleResponse(workMessage);
			}
			break;
			
		case DELETE:
		case UPDATE:
		default:
			break;
		}

	}

	@Override
	public void handleCmdError(WorkMessage workMessage, Channel channel) {
		
	}
	
	/*
	* For this event I just reply with SIZEIS message.
	* Since term is set when CANDIDATE starts election, this leader will go to Follower state as part of request vote event.
	*
	* */
	@Override
	public void handleGetClusterSize(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~Leader - Handle Cluster Size Event");

		logger.info("Replying to :" + workMessage.getHeader().getNodeId());
		state.getEmon().broadcastMessage(util.createSizeIsMessage(
			state, workMessage.getHeader().getNodeId()));
	}

	/*
	* In this state we don't need implementation for this event or message type.
	* */
	@Override
	public void handleSizeIs(WorkMessage workMessage, Channel channel) {
	}

	/*
	* In case I get equal term Id, then we can go to candidate state.
	* */
	@Override
	public void handleLeaderIs(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~Leader - Handle Leader Is Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		logger.info("Leader - New Term: " + inComingTerm);
		logger.info("Leader - Current Term: " + currentTerm);

		/*
		* If there is another node which is leader in new term, then I update myself and go back to election state
		* */
		if (inComingTerm > currentTerm) {
			logger.info("LEADER IS: " + workMessage.getLeader().getLeaderId());
			state.setElectionId(workMessage.getLeader().getElectionId());
			state.setLeaderId(workMessage.getLeader().getLeaderId());
			state.setLeaderHeartBeatdt (System.currentTimeMillis ());
			state.setState(NodeStateEnum.FOLLOWER);
		}

		/*
		* If there is another leader which is elected in same term then I go back to candidate state to start new election
		* This scenario might happen because we dont have constant cluster size and we evaluate dynamically and it need not be
		* that in 2 seconds we get entire cluster size and split votes might occur, to be on the safe side its better to go to CANDIDATE
		* */
		if(inComingTerm == currentTerm) {
			state.setState (NodeStateEnum.CANDIDATE);
		}
	}

	/*
	* If Vote Request is for term higher than the term I am leader in then I should vote and move back to follower
	* */
	@Override
	public void handleVoteRequest(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~Leader - Handle Vote Request Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		logger.info("Leader - New Term: " + inComingTerm);
		logger.info("Leader - Current Term: " + currentTerm);

		if (inComingTerm > currentTerm) {
			state.setElectionId (workMessage.getLeader ().getElectionId ());
			state.setLeaderId (workMessage.getLeader ().getLeaderId ());

			VoteResponse vote = new VoteResponse (nodeId,
					workMessage.getLeader().getElectionId(),
					workMessage.getLeader().getLeaderId());

			vote.setDestination (workMessage.getHeader ().getNodeId ());
			vote.setMaxHops (state.getConf ().getMaxHops ());

			//Reply to the person who sent request
			channel.writeAndFlush (vote.getMessage ());

			// Broadcast the message to outbound edges.
			// Because if my in bound edge is down, I am trying to reach my candidate in different path..
			state.getEmon().broadCastOutBound (vote.getMessage());
			state.setVotedFor (workMessage.getHeader ().getNodeId ());

			state.setLeaderHeartBeatdt (System.currentTimeMillis ());
			state.setState (NodeStateEnum.FOLLOWER);
		}
	}

	/*
	* There might be a delay to receive a vote from far neighbour,
	* It is ok, not to consider a vote once I become a leader.
	* */
	@Override
	public void handleVoteResponse(WorkMessage workMessage, Channel channel) {
	}

	@Override
	public void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel) {
	}

	/*
	* Check for the term message came from, i
	* if it is higher than my term then go to follower state,
	* if equal then go to candidate state.
	* if lesser then update beat time from follower
	* */
	@Override
	public void handleBeat(WorkMessage workMessage, Channel channel) {
		logger.info("~~~~~~~~Leader - Handle Beat Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		logger.info("Leader - New Term: " + inComingTerm);
		logger.info("Leader - Current Term: " + currentTerm);

		/*This should never happen, but for safety this means there is another leader with greater term and I should
		go back to Follower*/
		if(inComingTerm > currentTerm)   {
			state.setLeaderHeartBeatdt (System.currentTimeMillis ());
			state.setState (NodeStateEnum.FOLLOWER);
			return;
		}

		/*If I become a leader and find that there is another leader with equal term then I try to re start election*/
		if(inComingTerm == currentTerm)   {
			state.setState (NodeStateEnum.CANDIDATE);
			return;
		}

		/*If not any of the cases above then it means I am getting beat response from my followers, so I
		* add them to the list and also update their beat time*/
		int followerId = workMessage.getHeader ().getNodeId ();
		addFollower (followerId); // Add follower Id

		long currentTime = System.currentTimeMillis ();

		followerMonitor.onBeat (followerId, currentTime); // notify follower monitor about heart beat
	}

	/* Release all the resources. In this case it is only followerMonitor */
	@Override
	public void beforeStateChange() {
		logger.info("~~~~~~~~Leader - Handle Before State Change Event");
		getActiveNodes().clear ();
		followerMonitor.cancel ();
	}

	@Override
	public void afterStateChange() {
		logger.info("~~~~~~~~Leader - Handle After State Change Event");
		followerMonitor.start();
	}

	@Override
	public void addFollower(int followerId) {
		logger.info("~~~~~~~~Leader - Follower Added");
		getActiveNodes().put(followerId, theObject);
	}

	@Override
	public void removeFollower(int followerId) {
		logger.info("~~~~~~~~Leader - Handle Remove Follower");
		getActiveNodes().remove(followerId);
	}

	public ConcurrentHashMap<Integer, Object> getActiveNodes() {
		return activeNodes;
	}

	@Override
	public void notifyTaskCompletion(String key) {
		logger.info("Task Complete Notification");

		if(getTaskMap.containsKey (key))   {
			logger.info("Removing Get Task");
			getTaskMap.remove(key);
		}

		if(storeTaskMap.containsKey (key))  {
			logger.info("Removing Store Task");
			storeTaskMap.remove (key);
		}
	}
}
