package gash.router.server.messages.cmd_messages.handlers;

import gash.router.container.RoutingConf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import routing.Pipe.CommandMessage;

/**
 * Created by codepenman on 3/27/16.
 */
public class CmdFailureMsgHandler implements ICmdMessageHandler {

	private final RoutingConf conf;
	private final Logger logger;
	private ICmdMessageHandler nextHandler;

	public CmdFailureMsgHandler(RoutingConf state, Logger logger)  {
		this.conf = state;
		this.logger = logger;
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if(cmdMessage.hasErr ())  {
			handle(cmdMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (cmdMessage, channel);
			}else   {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(CommandMessage cmdMessage, Channel channel) {

	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
