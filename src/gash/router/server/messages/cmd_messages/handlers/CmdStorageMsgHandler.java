/**
 * 
 */
package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.QueueManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author saurabh
 *
 */
public class CmdStorageMsgHandler extends Thread implements ICmdMessageHandler {

	private final Logger logger = LoggerFactory.getLogger("Command Storage Message Handler");
	private QueueManager queues;
	private boolean forever = true;
	private ICmdMessageHandler nextHandler;
	private ConcurrentHashMap<String, SocketAddress> key2Address = new ConcurrentHashMap<>();
	private ConcurrentHashMap<SocketAddress, Channel> addr2Channel = new ConcurrentHashMap<>();

	public CmdStorageMsgHandler(QueueManager queues) {
		super();
		this.queues = queues;
		//start ();
	}

	@Override
	public void run() {
		logger.info("Started Command Storage Message Handler...");

		while (forever) {
			try {
				// Getting message from work server. This message should be
				// forwarded to the client.
				CommandMessage msg = queues.getFromWorkServer().take();

				//Update Address of the source from where I recieved Message Request...
				if(key2Address.containsKey (msg.getResponse ().getKey ()))  {
					SocketAddress addr = key2Address.get (msg.getResponse ().getKey ());
					if(addr2Channel.containsKey (addr)) {
						addr2Channel.get (addr).writeAndFlush (msg);
					}else   {
						//I assume if there should always be an entry in add2Channel map, if there is an entry in key2Add
						key2Address.remove (msg.getResponse ().getKey ());
					}
				}else   {
					logger.info("No Client is waiting for the response....");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if (cmdMessage.hasQuery()/* || cmdMessage.hasResponse ()*/) {
			handleTaskMessage(cmdMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(cmdMessage, channel);
			} else {
				logger.info("*****No Handler available*****");
			}
		}
	}

	private void handleTaskMessage(CommandMessage cmdMessage, Channel channel) {

		logger.info("Adding command message to work server:");

		if(!key2Address.containsKey (cmdMessage.getQuery ().getKey ()))  {
			key2Address.put (cmdMessage.getQuery ().getKey (), channel.remoteAddress ());
			addr2Channel.put (channel.remoteAddress (), channel);
			channel.closeFuture ().addListener (new ClientClosedListener());
		}
		try {
			queues.getToWorkServer ().put (cmdMessage);
		} catch (InterruptedException e) {
			e.printStackTrace ();
		}
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	public class ClientClosedListener implements ChannelFutureListener {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			logger.info("--> client lost connection to the server");
			logger.info(key2Address.toString ());
			logger.info(addr2Channel.toString ());

			addr2Channel.remove (future.channel ().remoteAddress ());
			key2Address.values ().removeIf (entry -> entry == future.channel ().remoteAddress ());
		}
	}
}