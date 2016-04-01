package Election;

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
	private final Logger logger = LoggerFactory.getLogger ("Leader Health Monitor");
	private AtomicBoolean stop;
	private HealthMonitorTask task;
	private AtomicLong beatTime;

	public LeaderHealthMonitor(LeaderHealthListener healthListener, long timeout)    {
		this.healthListener = healthListener;
		stop.set (false);
		task = new HealthMonitorTask (timeout);
		stop = new AtomicBoolean (false);
		beatTime = new AtomicLong (Long.MAX_VALUE);
	}

	public void start() {
		task.start ();
	}

	public void cancel()    {
		stop.getAndSet (true);
	}

	public void onBeat(long beatTime)    {
		this.beatTime.getAndSet (beatTime);
	}

	private class HealthMonitorTask extends Thread{

		private final long timeout;

		public HealthMonitorTask(long timeout) {
			this.timeout = timeout;
		}

		@Override
		public void run()   {
			try {
				while(!stop.get ())    {
					if(debug)
						logger.info ("********Started: " + new Date (System.currentTimeMillis ()));

					long currentTime = System.currentTimeMillis ();

					if((currentTime - beatTime.get ()) > timeout)    {
						healthListener.onLeaderBadHealth ();
						break;
					}

					wait (timeout);
				}
			} catch (InterruptedException e) {
				logger.info ("********Timer was interrupted: " + new Date (System.currentTimeMillis ()));
			}
		}
	}
}
