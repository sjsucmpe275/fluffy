package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TimeoutListener;

import java.util.Date;

/*
* Gets a request to start election timer.
* Know about the owner(person who starts it) of timer.
* Owner should be able to cancel the task.
* Owner should be notified once the timer is out
* If owner cancel the task, timer should not notify the owner or should be interrupted
* */
public class Timer {

	private static final Logger logger = LoggerFactory.getLogger ("Election Timer");
	private final long electionTimeout;
	private TimeoutListener listener;
	private TimerThread timerThread;

	public Timer(TimeoutListener listener, long electionTimeout) {
		this.listener = listener;
		this.electionTimeout = electionTimeout;
		timerThread = new TimerThread ();
	}

	public void startTimer()   {
		logger.info ("********Request to start election timer: " + new Date (System.currentTimeMillis ()));
		timerThread.start ();
	}

	public void cancel()    {
		logger.info ("********Request to cancel election timer: " + new Date (System.currentTimeMillis ()));
		timerThread.interrupt ();
	}

	private class TimerThread extends Thread{

		@Override
		public void run(){
			synchronized (this){
				try {
					logger.info ("********Election timer started: " + new Date (System.currentTimeMillis ()));
					wait (electionTimeout);
					logger.info ("********Election timed out: " + new Date (System.currentTimeMillis ()));
					listener.notifyTimeout ();
				} catch (InterruptedException e) {
					logger.info ("********Election Timer was interrupted: " + new Date (System.currentTimeMillis ()));
				}
			}
		}
	}
}
