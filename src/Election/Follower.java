package Election;

import pipe.work.Work.WorkMessage;

public class Follower implements NodeState {

	int lastHeartBeatValue;
	ElectionTimer timer;
	
	public Follower(ElectionTimer t){
		this.timer=t;
	}

	@Override
	public void HandleMessage(WorkMessage workMessage) {
		// TODO Auto-generated method stub
		
	}
	

}
