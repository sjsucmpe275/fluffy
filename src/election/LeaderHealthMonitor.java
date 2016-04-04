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
	private AtomicBoolean isRunning;

	public LeaderHealthMonitor(LeaderHealthListener healthListener, long timeout) {
		this.healthListener = healthListener;
		task = new HealthMonitorTask(timeout);
		stop = new AtomicBoolean(false);
		isRunning = new AtomicBoolean (false);
		beatTime = new AtomicLong(System.currentTimeMillis());
	}

	public void start() {
		/* Start the task, only if it is not started */
		if(!isRunning.get ())   {
			task.start();
			isRunning.getAndSet (true);
		}
	}

	public void cancel() {
		/* Cancel the task, only if it not stopped before */
		if(stop.get ()) {
			stop.getAndSet(true);
			task.interrupt ();
			System.out.println("~~~~~~~~Follower - Cancelled Leader Monitor");
		}
	}

	public void onBeat(long beatTime) {
		this.beatTime.getAndSet(beatTime);
	}

	private class HealthMonitorTask extends Thread {

		private final long timeout;

		public HealthMonitorTask(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public void run() {
			try {
				while (!stop.get()) {
					if (debug)
						System.out.println("********Started: " + new Date(System.currentTimeMillis()));

					long currentTime = System.currentTimeMillis();

					System.out.println("*****Last heart beat received from Leader: " + (currentTime - beatTime.get()));
					if ((currentTime - beatTime.get()) > timeout) {
						healthListener.onLeaderBadHealth();
						break;
					}
					synchronized (this) {
						wait(timeout);
					}
				}
			} catch (InterruptedException e) {
				System.out.println("********Timer was interrupted: " + new Date(System.currentTimeMillis()));
			}
		}
	}
}
