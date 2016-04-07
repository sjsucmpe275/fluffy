/**
 * 
 */
package gash.router.server;

import routing.Pipe.CommandMessage;

import java.util.concurrent.LinkedBlockingQueue;

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
		System.out.println("To work server...");
		System.out.println(toWorkServer);
		return toWorkServer;
	}

}
