/**
 * 
 */
package gash.router.server;

import java.util.concurrent.LinkedBlockingQueue;

import routing.Pipe.CommandMessage;

/**
 * @author saurabh
 *
 */
public class QueueManager {

	private LinkedBlockingQueue<CommandMessage> toWorkServer;
	private LinkedBlockingQueue<CommandMessage> fromWorkServer;

	public QueueManager(int size) {
		this.toWorkServer = new LinkedBlockingQueue<>(size);
		this.fromWorkServer = new LinkedBlockingQueue<>(size);
	}

	public LinkedBlockingQueue<CommandMessage> getFromWorkServer() {
		return fromWorkServer;
	}

	public LinkedBlockingQueue<CommandMessage> getToWorkServer() {
		return toWorkServer;
	}

}
