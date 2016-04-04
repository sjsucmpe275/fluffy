package gash.router.server;

import election.*;
import gash.router.container.RoutingConf;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.edges.AdaptorEdgeMonitor;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.TaskList;
import gash.router.container.Observer;

public class ServerState implements Observer{
	private final RoutingConf conf;
	private EdgeMonitor emon;
	private AdaptorEdgeMonitor adapEmon;
	private TaskList tasks;
	private INodeState leader;
	private INodeState candidate;
	private INodeState follower;
	private INodeState currentState;
	private int leaderId;
	private int electionId;// termId

	public ServerState(RoutingConf conf) {
		this.conf = conf;
		this.leader = new Leader(this);
		this.candidate = new Candidate(this);
		this.follower = new Follower(this);
		this.currentState = follower;
		this.leaderId = -1;
		this.electionId = 0;
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
	public void setAdaptorEmon(AdaptorEdgeMonitor emon) {
		this.adapEmon = emon;
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

	public int getElectionId() {
		return electionId;
	}

	public void setElectionId(int electionId) {
		this.electionId = electionId;
	}

	@Override
	public void onFileChanged(RoutingConf configuration) {
		System.out.println("in server state");
			this.conf.setNodeId(configuration.getNodeId());;
			this.conf.setCommandPort(configuration.getCommandPort());
			this.conf.setWorkPort(configuration.getWorkPort());
			this.conf.setInternalNode(configuration.isInternalNode());
			this.conf.setHeartbeatDt(configuration.getHeartbeatDt());
			this.conf.setDatabase(configuration.getDatabase());
			this.conf.setElectionTimeout(configuration.getElectionTimeout());
			for(int i=0;i<configuration.routing.size();i++){
				this.conf.routing.add(configuration.routing.get(i));
			}
			for(int i=0;i<configuration.adaptorRouting.size();i++){
				this.conf.adaptorRouting.add(configuration.adaptorRouting.get(i));
			}
			
		}
		
	
}
