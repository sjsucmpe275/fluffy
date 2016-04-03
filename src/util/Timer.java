package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/*
* Gets a request to start election timer.
* Know about the owner(person who starts it) of timer.
* Owner should be able to cancel the task.
* Owner should be notified once the timer is out
* If owner cancel the task, timer should not notify the owner or should be interrupted
* */
public class Timer {

	private static final Logger logger = LoggerFactory.getLogger ("Timer");
	private static final boolean debug = true;
	private long timeout;
	private TimeoutListener listener;
	private TimerThread timerThread;
	//private AtomicBoolean stop;
	private Object lock;

	public Timer(TimeoutListener listener, long timeout) {
		this.listener = listener;
		this.timeout = timeout;
		this.lock = new Object();
		//stop = new AtomicBoolean (false);
		timerThread = new TimerThread ();
	}

	public void startTimer()   {
		if(debug)
			logger.info ("********Request to start timer: " + new Date (System.currentTimeMillis ()));
		timerThread.start ();
	}

	public void resetTimer(TimeoutListener listener, long timeout)    {
		this.timeout = timeout;
		this.listener = listener;
		timerThread.interrupt ();
		//stop.getAndSet (false);
		timerThread.start ();
	}

	public void cancel()    {
		if(debug)
			logger.info ("********Request to cancel timer: " + new Date (System.currentTimeMillis ()));
		//stop.getAndSet (true);
		timerThread.interrupt ();
	}

	private class TimerThread extends Thread{

		@Override
		public void run(){
			try {
				if(debug)
					logger.info ("********Timer started: " + new Date (System.currentTimeMillis ()));

				synchronized (this) {
					wait(timeout);
				}

				if(debug)
					logger.info ("********Timed out: " + new Date (System.currentTimeMillis ()));

				listener.notifyTimeout ();

			} catch (InterruptedException e) {
				logger.info ("********Timer was interrupted: " + new Date (System.currentTimeMillis ()));
			}
		}
	}
}
