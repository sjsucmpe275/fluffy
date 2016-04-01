package Election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class Leader implements INodeState{

	private ServerState state;
	public Leader(ServerState serverState) {
		this.state=serverState;
	}

	public void sendHeartBeat(){}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
			state.addListOfNode(state.getConf().getNodeId());
		
		
	}
	
	@Override
	public void stateChanged()	{
		
	}
}
