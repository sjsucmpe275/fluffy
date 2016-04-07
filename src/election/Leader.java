package election;

import gash.router.server.ServerState;
import gash.router.server.tasks.IReplicationStrategy;
import gash.router.server.tasks.RoundRobinStrategy;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Leader implements INodeState, FollowerListener {

	private final Logger logger = LoggerFactory.getLogger("Leader");

	private ServerState state;
	private int nodeId;
	private final Object theObject = new Object();
	private final ConcurrentHashMap<Integer, Object> activeNodes;
	private FollowerHealthMonitor followerMonitor;
	private ElectionUtil util;
	private IReplicationStrategy strategy;
	private ConcurrentHashMap<String, Integer> key2node;
	
	public Leader(ServerState state) {
		this.state = state;
		this.nodeId = state.getConf().getNodeId();
		this.activeNodes = new ConcurrentHashMap<> ();
		this.followerMonitor = new FollowerHealthMonitor(this, state,
				state.getConf().getElectionTimeout());
		this.util = new ElectionUtil();
		this.strategy = new RoundRobinStrategy(2);
		this.key2node = new ConcurrentHashMap<>();
	}

	public void handleCmdQuery(WorkMessage wrkMessage, Channel channel) {

		System.out.println ("LEADER RECEIVED MESSAGE");
		System.out.println (wrkMessage);
		CommandMessage taskMessage = wrkMessage.getTask ().getTaskMessage ();

		List<Integer> activeNodeIds = new ArrayList<> ();

			/*I am adding myself also as a Node, which will take part in storage/replication*/
		activeNodeIds.add (nodeId);

		for (Integer i : activeNodes.keySet ()) {
			activeNodeIds.add (i);
		}

//			activeNodeIds.add(wrkMessage.getHeader().getDestination());

		switch (taskMessage.getQuery ().getAction ()) {
			case GET:
				System.out.println ("Do Things");
				break;
			case STORE:
				System.out.println ("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
				System.out.println (wrkMessage);
				System.out.println ("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

				key2node.putIfAbsent (taskMessage.getQuery ().getKey (),
						wrkMessage.getHeader ().getNodeId ());
				System.out.println (key2node);

				List<Integer> replicationNodes = strategy.getNodeIds (activeNodeIds);

				for (Integer destinationId : replicationNodes) {
					WorkMessage.Builder wb = WorkMessage.newBuilder (wrkMessage);
					Header.Builder hb = Header
							.newBuilder (wrkMessage.getHeader ());
					hb.setNodeId (nodeId);
					hb.setDestination (destinationId);
					wb.setHeader (hb);
					wb.setSecret (1);

					if (destinationId != nodeId) {
						state.getEmon ().broadcastMessage (wb.build ());
					} else {
						state.getTasks ().addTask (wrkMessage.getTask ());
					}
				}
				break;
			case DELETE:
				break;
			case UPDATE:
				break;
			default:
				break;
		}

	}
	
	@Override
	public void handleCmdResponse(WorkMessage workMessage, Channel channel) {

		CommandMessage taskMessage = workMessage.getTask().getTaskMessage();
		switch (taskMessage.getResponse ().getAction()) {
		
		case GET:
			System.out.println("Get is going");
			break;
			
		case STORE:
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			System.out.println(workMessage);
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			WorkMessage.Builder wb = WorkMessage.newBuilder(workMessage);
			Header.Builder hb = Header
				.newBuilder(workMessage.getHeader());
			hb.setDestination(key2node.get(taskMessage.getResponse ().getKey()));
			wb.setHeader(hb);
			wb.setSecret(1);
			
			if (hb.getDestination() == nodeId) {
				try {
					state.getQueues().getFromWorkServer().put(wb.build().getTask().getTaskMessage());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				state.getEmon().broadcastMessage(wb.build());
			}
			break;
			
		case DELETE:
			break;
		case UPDATE:
			break;
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
		System.out.println("~~~~~~~~Leader - Handle Cluster Size Event");

		System.out.println("Replying to :" + workMessage.getHeader().getNodeId());
		state.getEmon().broadcastMessage(util.createSizeIsMessage(
			state, workMessage.getHeader().getNodeId()));
		
/*
		ConcurrentHashMap<Integer, EdgeInfo> edgeMap = state.getEmon()
			.getOutboundEdges().getEdgesMap();
		for (Integer destinationId : edgeMap.keySet()) {
			EdgeInfo edge = edgeMap.get(destinationId);
			if (edge.isActive() && edge.getChannel() != null) {
				edge.getChannel().writeAndFlush(
					util.createGetClusterSizeMessage(nodeId, destinationId));
			}
		}
*/
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
		System.out.println("~~~~~~~~Leader - Handle Leader Is Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		System.out.println("Leader - New Term: " + inComingTerm);
		System.out.println("Leader - Current Term: " + currentTerm);

		/*
		* If there is another node which is leader in new term, then I update myself and go back to election state
		* */
		if (inComingTerm > currentTerm) {
			System.out.println("LEADER IS: " + workMessage.getLeader().getLeaderId());
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
		System.out.println("~~~~~~~~Leader - Handle Vote Request Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		System.out.println("Leader - New Term: " + inComingTerm);
		System.out.println("Leader - Current Term: " + currentTerm);

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
		System.out.println("~~~~~~~~Leader - Handle Beat Event");

		int inComingTerm = workMessage.getLeader ().getElectionId ();
		int currentTerm = state.getElectionId ();

		System.out.println("Leader - New Term: " + inComingTerm);
		System.out.println("Leader - Current Term: " + currentTerm);

		if(inComingTerm > currentTerm)   {
			state.setLeaderHeartBeatdt (System.currentTimeMillis ());
			state.setState (NodeStateEnum.FOLLOWER);
			return;
		}

		if(inComingTerm == currentTerm)   {
			state.setState (NodeStateEnum.CANDIDATE);
			return;
		}

		//state.setLeaderHeartBeatdt (currentTime);

		int followerId = workMessage.getHeader ().getNodeId ();
		addFollower (followerId); // Add follower Id

		long currentTime = System.currentTimeMillis ();

		followerMonitor.onBeat (followerId, currentTime); // notify follower monitor about heart beat
	}

	/* Release all the resources. In this case it is only followerMonitor */
	@Override
	public void beforeStateChange() {
		System.out.println("~~~~~~~~Leader - Handle Before State Change Event");
		getActiveNodes().clear ();
		followerMonitor.cancel ();
	}

	@Override
	public void afterStateChange() {
		System.out.println("~~~~~~~~Leader - Handle After State Change Event");
		followerMonitor.start();
	}

	@Override
	public void addFollower(int followerId) {
		System.out.println("~~~~~~~~Leader - Follower Added");
		getActiveNodes().put(followerId, theObject);
	}

	@Override
	public void removeFollower(int followerId) {
		System.out.println("~~~~~~~~Leader - Handle Remove Follower");
		getActiveNodes().remove(followerId);
	}

	public ConcurrentHashMap<Integer, Object> getActiveNodes() {
		return activeNodes;
	}
}
