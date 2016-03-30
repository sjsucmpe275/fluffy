package gash.router.server;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;

import java.util.HashMap;

import Election.NodeState;

public class ServerState {
	private RoutingConf conf;
	private EdgeMonitor emon;
	private TaskList tasks;
	private HashMap<Integer, Follower> followers;
	private NodeState state;

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

	public NodeState getState() {
		return state;
	}

	public void setState(NodeState state) {
		this.state = state;
	}

	public void setTasks(TaskList tasks) {
		this.tasks = tasks;
	}

	public void addFollower(Follower follower) {
		followers.put(follower.getFollowerId(), follower);
	}

	public HashMap<Integer, Follower> getFollowers() {
		return followers;
	}

	public void resetFollowersState() {
		followers.forEach((key, follower) -> follower.setIsAlive(false));
	}
}
