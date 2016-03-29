package LeaderElection;

public class NodeState {
	public int currentState;
	public static enum State{
		Leader,Follower,Candidate;
	}
	public void respondToVote(){}
	
	public void respondToLeader(){}
	
	public void respondToWhoIsLeader(){}
	
	public void AskWhoIsLeader(){}
}
