/**
 * 
 */
package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.QueueManager;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author saurabh
 *
 */
public class CmdStorageMsgHandler extends Thread implements ICmdMessageHandler {

	private QueueManager queues;
	private boolean forever = true;
	private ICmdMessageHandler nextHandler;
	private ConcurrentHashMap<String, Channel> key2Channel = new ConcurrentHashMap<>();

	public CmdStorageMsgHandler(QueueManager queues) {
		super();
		this.queues = queues;
		start ();
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
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if (cmdMessage.hasQuery() || cmdMessage.hasResponse ()) {
			handleTaskMessage(cmdMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(cmdMessage, channel);
			} else {
				System.out.println("*****No Handler available*****");
			}
		}
	}


	private void handleTaskMessage(CommandMessage cmdMessage, Channel channel) {

		System.out.println("Adding command message to work server:");
		System.out.println(cmdMessage);


		if(cmdMessage.hasQuery ())  {
				key2Channel.put (cmdMessage.getQuery ().getKey (), channel);
			try {
				queues.getToWorkServer ().put (cmdMessage);
			} catch (InterruptedException e) {
				e.printStackTrace ();
			}
		}
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

}