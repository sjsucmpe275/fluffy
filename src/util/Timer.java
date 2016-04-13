package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets a request to start election timer. Know about the owner(person who
 * starts it) of timer. Owner should be able to cancel the task. Owner should be
 * notified once the timer is out If owner cancel the task, timer should not
 * notify the owner or should be interrupted
 */
public class Timer {

	private static final Logger logger = LoggerFactory.getLogger ("Timer");
	private static final boolean debug = true;
	private final String identifier;
	private long timeout;
	private TimeoutListener listener;
	private TimerThread timerThread;

	public Timer(TimeoutListener listener, long timeout, String identifier) {
		this.listener = listener;
		this.timeout = timeout;
		this.identifier = identifier;
	}

	public void start() {
		if (debug)
			logger.info("******** " + identifier + ", Request to start timer: "
				+ Thread.currentThread().getName());

		timerThread = new TimerThread();
		timerThread.start();
	}

	public void start(long timeout) {
		this.timeout = timeout;
		start ();
	}

	public void start(TimeoutListener listener, long timeout) {
		this.listener = listener;
		this.timeout = timeout;
		start ();
	}

	public void cancel() {
		if (timerThread == null) {
			return;
		}
		if (debug)
			logger.info("********" + identifier + ", Request to cancel timer: "
				+ Thread.currentThread().getName());

		timerThread.interrupt();
	}

	private class TimerThread extends Thread {

		@Override
		public void run() {
			try {
				if (debug)
					logger.info("********Timer started: " + Thread.currentThread().getName());

				synchronized (this) {
					wait(timeout);
				}

				if (debug)
					logger.info("********Timed out: " + Thread.currentThread().getName());

				listener.notifyTimeout();

			} catch (InterruptedException e) {
				logger.info("********Timer was interrupted: " 
					+ Thread.currentThread().getName());
			}
		}
	}
}
