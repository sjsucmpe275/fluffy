package gash.router.server;

import Election.*;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;


import java.util.HashMap;
import java.util.TreeSet;

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
	private int leaderId;
	private int electionId;//termId
	private TreeSet listOfNodeIds;

	public ServerState()	{
		leader = new Leader(this);
		candidate = new Candidate(this);
		follower = new Follower(this);
		currentState = follower;
		leaderId = -1;
		electionId=0;
		listOfNodeIds = new TreeSet();
	}
	
	public RoutingConf getConf() {
		return conf;
	}
	
	public TreeSet getListOfNodeIds(){
		return listOfNodeIds;
	}
	
	public void addListOfNode(int nodeId){
		if(!listOfNodeIds.contains(nodeId))
			listOfNodeIds.add(nodeId);
	}

	public void setConf(RoutingConf conf) {
		this.conf = conf;
		if (conf.getNodeId() == 1) {
			currentState = candidate;
		}
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
		currentState.beforeStateChange ();

		if(state == NodeStateEnum.CANDIDATE)	{
			currentState = candidate;
		}
		if(state == NodeStateEnum.FOLLOWER)	{
			currentState = follower;
		}
		if(state == NodeStateEnum.LEADER)	{
			currentState = leader;
		}

		currentState.afterStateChange ();
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

	public int getElectionId() {
		return electionId;
	}

	public void setElectionId(int electionId) {
		this.electionId = electionId;
	}
}
