package Election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class Candidate implements INodeState {
	private int VoteCount;
	private int sizeOfCluster;
	private ServerState state;
	public Candidate(ServerState serverState) {
		this.state=serverState;
	}
	
	public void requestVote(){
		
	}
	
	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
			state.addListOfNode(state.getConf().getNodeId());
		
		
	}
	
	@Override
	public void stateChanged()	{
		
	}
	
}
