package election;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private Object lock;

	public LeaderHealthMonitor(LeaderHealthListener healthListener, long timeout, String identifier) {
		this.healthListener = healthListener;
		task = new HealthMonitorTask(timeout);
		stop = new AtomicBoolean(false);
		beatTime = new AtomicLong(System.currentTimeMillis());
		lock = new Object();
		System.out.println(Thread.currentThread() + ":" + identifier);
	}

	public void start() {
		task.start();
	}

	public void cancel() {
		stop.getAndSet(true);
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
						logger.info("********Started: " + new Date(System.currentTimeMillis()));

					long currentTime = System.currentTimeMillis();

					if ((currentTime - beatTime.get()) > timeout) {
						healthListener.onLeaderBadHealth();
						break;
					}
					synchronized (lock) {
						lock.wait(timeout);
					}
				}
			} catch (InterruptedException e) {
				logger.info("********Timer was interrupted: " + new Date(System.currentTimeMillis()));
			}
		}
	}
}
