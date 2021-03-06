package gash.router.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import election.Candidate;
import election.Follower;
import election.INodeState;
import election.Leader;
import election.NodeStateEnum;
import gash.router.container.Observer;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;

public class ServerState implements Observer {

	private final RoutingConf conf;
	private EdgeMonitor emon;
	private TaskList tasks;
	private INodeState leader;
	private INodeState candidate;
	private INodeState follower;
	private INodeState currentState;
	private AtomicInteger leaderId;
	private AtomicLong leaderHeartBeatdt;
	private AtomicInteger electionId; // termId
	private AtomicInteger votedFor;
	private QueueManager queues;

	public ServerState(RoutingConf conf) {
		this.conf = conf;
		this.leader = new Leader(this);
		this.candidate = new Candidate(this);
		this.follower = new Follower(this);
		this.currentState = follower;
		this.leaderId = new AtomicInteger(-1);
		this.leaderHeartBeatdt = new AtomicLong(Long.MAX_VALUE);
		// To ensure that I will wait for heart beat timeout
		this.electionId = new AtomicInteger(0);
		this.votedFor = new AtomicInteger(-1);
	}

	public RoutingConf getConf() {
		return conf;
	}

	public EdgeMonitor getEmon() {
		return emon;
	}

	public void setEmon(EdgeMonitor emon) {
		this.emon = emon;
	}

	public TaskList getTasks() {
		return tasks;
	}

	public void setState(NodeStateEnum state) {
		synchronized (this) {
			currentState.beforeStateChange();

			if (state == NodeStateEnum.CANDIDATE) {
				currentState = candidate;
			}
			if (state == NodeStateEnum.FOLLOWER) {
				currentState = follower;
			}
			if (state == NodeStateEnum.LEADER) {
				currentState = leader;
			}

			currentState.afterStateChange();
		}
	}

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}

	public INodeState getCurrentState() {
		return currentState;
	}

	public int getLeaderId() {
		return leaderId.get();
	}

	public void setLeaderId(int leaderId) {
		this.leaderId.getAndSet(leaderId);
	}

	public void setVotedFor(int votedFor) {
		this.votedFor.getAndSet(votedFor);
	}

	public int getVotedFor() {
		return votedFor.get();
	}

	public void setQueues(QueueManager queues) {
		this.queues = queues;
	}

	public QueueManager getQueues() {
		return queues;
	}

	public int getElectionId() {
		return electionId.get();
	}

	public void setElectionId(int electionId) {
		this.electionId.getAndSet(electionId);
	}

	public long getLeaderHeartBeatdt() {
		return leaderHeartBeatdt.get();
	}

	public void setLeaderHeartBeatdt(long leaderHeartBeatdt) {
		this.leaderHeartBeatdt.getAndSet(leaderHeartBeatdt);
	}

	@Override
	public void onFileChanged(RoutingConf configuration) {
		conf.setNodeId(configuration.getNodeId());
		conf.setCommandPort(configuration.getCommandPort());
		conf.setWorkPort(configuration.getWorkPort());
		conf.setInternalNode(configuration.isInternalNode());
		conf.setHeartbeatDt(configuration.getHeartbeatDt());
		conf.setDatabase(configuration.getDatabase());
		conf.setElectionTimeout(configuration.getElectionTimeout());
		conf.setMaxHops(configuration.getMaxHops());

		for (int i = 0; i < configuration.routing.size(); i++) {
			conf.routing.add(configuration.routing.get(i));
		}

		for (int j = 0; j < configuration.adaptorRouting.size(); j++) {
			this.conf.adaptorRouting.add(configuration.adaptorRouting.get(j));
		}
	}
}
