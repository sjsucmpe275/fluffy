package election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: codepenman.
 * @date: 4/1/16
 */
public class LeaderHealthMonitor {

	private final LeaderHealthListener healthListener;
	private boolean debug = false;
	private final Logger logger = LoggerFactory.getLogger("Leader Health Monitor");
	private AtomicBoolean stop;
	private HealthMonitorTask task;
	private AtomicLong beatTime;
	private final long timeout;

	public LeaderHealthMonitor(LeaderHealthListener healthListener, long timeout) {
		this.healthListener = healthListener;
		this.timeout = timeout;
		task = new HealthMonitorTask();
		stop = new AtomicBoolean(false);
		beatTime = new AtomicLong(Long.MAX_VALUE);
	}

	public void start() {
		/* Start the task, only if it is not started */
		logger.info("~~~~~~~~Follower - Started Leader Monitor");
		stop.getAndSet (false);
		task.start();
	}

	/* This method will be called by the owner of Health Monitor to update the beat time of leader
	* Internal task will be monitoring this time
	* This time can be set to Long.MAX_VALUE if this thread has to be alive instead of creating
	* montor multiple times..*/
	public void onBeat(long beatTime) {
		this.beatTime.getAndSet(beatTime);
	}

	private class HealthMonitorTask extends Thread {

		@Override
		public void run() {
			try {
				while (!stop.get()) {
					if (debug)
						logger.info("********Started: " + new Date(System.currentTimeMillis()));

					long currentTime = System.currentTimeMillis();

					if ((currentTime - beatTime.get()) > timeout) {
						healthListener.onLeaderBadHealth();
					}
					synchronized (this) {
						wait((long) (timeout * 0.9));
					}
				}
			} catch (InterruptedException e) {
				logger.info("********Timer was interrupted: " + new Date(System.currentTimeMillis()));
			}
		}
	}
}
