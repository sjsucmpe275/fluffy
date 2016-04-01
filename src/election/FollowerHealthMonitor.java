package election;

import gash.router.server.ServerState;
import gash.router.server.messages.wrk_messages.BeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: codepenman.
 * @date: 3/31/16
 *
 * This class is mainly for Leader to keep track of all the followers health status..
 *
 * This task, when started broad cast heart messages to the follower and starts listening to the reply.
 *
 * On reply from follower's I maintain follower2BeatTimeMap which will be updated with time beat was received
 * mapped to the follower who sent beat.
 *
 * Thread will be invoked in frequent intervals of time and checks if there is any follower who didn't reply, if yes
 * notify FollowerListener to remove the node. Once the iteration is done, I again send broad cast messages to all my followers
 * and go back to wait.
 *
*/
public class FollowerHealthMonitor {

	private boolean debug = false;
	private final Logger logger = LoggerFactory.getLogger ("Follower Health Monitor");
	private ConcurrentHashMap<Integer, Long> follower2BeatTimeMap;
	private final ServerState state;
	private FollowerListener followerListener;
	//private long interval;
	private HealthMonitorTask task;
	private AtomicBoolean stop;

	public FollowerHealthMonitor(FollowerListener followerListener, ServerState state, long timeout)  {
		this.followerListener = followerListener;
		this.state = state;
		//this.interval = timeout - (long)(0.1 * timeout); // 10% lesser than the original timeout
		task = new HealthMonitorTask (timeout);
		stop = new AtomicBoolean (false);
	}

	public void onBeat(int followerId, long heartBeatTime)    {
			follower2BeatTimeMap.put (followerId , heartBeatTime);
	}

	public void start() {
		//Broadcast heartbeat to all the followers
		BeatMessage beat = new BeatMessage (state.getConf ().getNodeId ());
		beat.setIsLeader (true);
		state.getEmon ().broadcastMessage (beat.getMessage ());

		task.start ();
	}

	public void cancel()    {
		stop.getAndSet (true);
	}

	private class HealthMonitorTask extends Thread{

		private final long timeout;
		private boolean broadCastBeat = false;

		public HealthMonitorTask(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public void run()   {
			try {
				while(!stop.get ())    {
					long currentTime = System.currentTimeMillis ();

					if(debug)
						logger.info ("********Started: " + new Date (System.currentTimeMillis ()));

					if(broadCastBeat)   {
						//Broadcast heartbeat to all the followers
						BeatMessage beat = new BeatMessage (state.getConf ().getNodeId ());
						beat.setIsLeader (true);
						state.getEmon ().broadcastMessage (beat.getMessage ());
						broadCastBeat = false;
					}else   {
						follower2BeatTimeMap.entrySet ()
								.stream ()
								.filter (entry -> currentTime - entry.getValue () > timeout)
								.forEach (entry -> followerListener.removeFollower (entry.getKey ()));
						broadCastBeat = true;
					}

					wait (timeout);
				}
			} catch (InterruptedException e) {
				logger.info ("********Timer was interrupted: " + new Date (System.currentTimeMillis ()));
			}
		}
	}
/*
	private class FollowerConnectionInfo {
		private boolean active;
		private final Channel channel;

		FollowerConnectionInfo(Channel channel, boolean active)  {
			this.channel = channel;
			this.active = active;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public Channel getChannel() {
			return channel;
		}
	}*/
}
