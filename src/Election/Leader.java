package Election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class Leader implements INodeState{

	
	public Leader(ServerState serverState) {
		// TODO Auto-generated constructor stub
	}

	public void sendHeartBeat(){}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stateChanged()	{
		
	}
}
