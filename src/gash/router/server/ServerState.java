package gash.router.server;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;

import java.util.HashMap;
import Election.Candidate;
import Election.Follower;
import Election.INodeState;
import Election.Leader;
import Election.NodeStateEnum;

public class ServerState {
	private RoutingConf conf;
	private EdgeMonitor emon;
	private TaskList tasks;
	private INodeState leader;
	private INodeState candidate;
	private INodeState follower;
	private INodeState currentState;

	public ServerState()	{
		leader = new Leader();
		candidate = new Candidate();
		follower = new Follower();
		currentState = follower;
	}
	
	public RoutingConf getConf() {
		return conf;
	}

	public void setConf(RoutingConf conf) {
		this.conf = conf;
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
		if(state == NodeStateEnum.CANDIDATE)	{
			currentState = candidate;
		}
		if(state == NodeStateEnum.FOLLOWER)	{
			currentState = follower;
		}
		if(state == NodeStateEnum.LEADER)	{
			currentState = leader;
		}
	}

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}
}
