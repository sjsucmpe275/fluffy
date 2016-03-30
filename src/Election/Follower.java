package Election;

import pipe.work.Work.WorkMessage;

public class Follower implements INodeState {

	int lastHeartBeatValue;
	ElectionTimer timer;
	
	public Follower(){
		//this.timer=;
	}

	@Override
	public void HandleMessage(WorkMessage workMessage) {
		// TODO Auto-generated method stub
		
	}
	

}
