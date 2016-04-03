package gash.router.server;

import election.*;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;

public class ServerState {
	private final RoutingConf conf;
	private EdgeMonitor emon;
	private TaskList tasks;
	private INodeState leader;
	private INodeState candidate;
	private INodeState follower;
	private INodeState currentState;
	private int leaderId;
	private int electionId;// termId
	private int votedFor;

	public ServerState(RoutingConf conf) {
		this.conf = conf;
		this.leader = new Leader(this);
		this.candidate = new Candidate(this);
		this.follower = new Follower(this);
		this.currentState = follower;
		this.leaderId = -1;
		this.electionId = 0;
		this.votedFor = -1;
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

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}

	public INodeState getCurrentState() {
		return currentState;
	}

	public int getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(int leaderId) {
		this.leaderId = leaderId;
	}

	public void setVotedFor(int votedFor)   {
		this.votedFor = votedFor;
	}

	public int getVotedFor()   {
		return votedFor;
	}

	public int getElectionId() {
		return electionId;
	}

	public void setElectionId(int electionId) {
		this.electionId = electionId;
	}
}
