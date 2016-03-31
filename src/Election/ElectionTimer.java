package Election;

import java.util.TimerTask;

public class ElectionTimer extends TimerTask{
	private Follower follower;
	public ElectionTimer(Follower follower) {
		this.follower=follower;
	}
	
	@Override
	public void run() {
		long currentTime = System.currentTimeMillis ();
		long diff=currentTime - follower.getLastHeartbeat();
		
		if(diff>follower.getDelay()){
			follower.isElectionNeeded(true);
		}
	}	
}
