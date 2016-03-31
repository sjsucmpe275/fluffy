package Election;

import io.netty.channel.Channel;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.WorkMessage;

public class Leader implements INodeState{
	
	public void sendHeartBeat(){}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus=workMessage.getLeader();
		switch(leaderStatus.getAction()){
			case GETCLUSTERSIZE:
				break;
			case SIZEIS:
				break;
			case THELEADERIS:
				break;
			case VOTEREQUEST:
				break;
			case VOTERESPONSE:
				break;
			case WHOISTHELEADER:
				break;
			default:
				
		
		}
		
	}

	
}
