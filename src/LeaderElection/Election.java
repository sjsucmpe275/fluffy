package LeaderElection;

import gash.router.server.ServerState;

public class Election {
	int NodeState;
	ServerState state;
	int leader;
	int voteCount;
	int electionId;
	
	public Election(ServerState st){
		this.state=st;
	}
	
	
	public void getElectionState(){
	}
	
	
	public int createElectionId(){return 0;}
	
	
	public void setLeaderId(int leaderId){}
	
	
	public int getVoteCount(){return voteCount;}
	
	
	public void notifyElectedLeaderId(boolean success,int leaderId){}
	
}
