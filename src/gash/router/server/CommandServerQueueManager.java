/**
 * 
 */
package gash.router.server;

import routing.Pipe.CommandMessage;

/**
 * @author saurabh
 *
 */
public class CommandServerQueueManager extends Thread {

	private QueueManager queues;
	private boolean forever = true;
	private CommandChannelHandler channelHandler;

	public CommandServerQueueManager(CommandChannelHandler channelHandler,
		QueueManager queues) {
		this.channelHandler = channelHandler;
		this.queues = queues;

	}

	@Override
	public void run() {
		while (forever) {
			try {
				
				// Getting message from work server. This message should be 
				// forwarded to the client.
				CommandMessage msg = queues.getFromWorkServer().take();

				// Expecting only command messages which needs to be sent to the
				// client. So channel is null.
				channelHandler.handleMessage(msg, null);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}