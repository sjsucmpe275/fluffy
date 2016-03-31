package Election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class Candidate implements INodeState {
	int VoteCount;
	int sizeOfCluster;
	
	public Candidate(ServerState serverState) {
		// TODO Auto-generated constructor stub
	}
	
	public void requestVote(){
		
	}
	
	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stateChanged()	{
		
	}
	
}
