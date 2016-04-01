package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import routing.Pipe.CommandMessage;

/**
 * Created by codepenman on 3/27/16.
 */
public class CmdMsgHandler implements ICmdMessageHandler {

	private final ServerState state;
	private final Logger logger;
	private ICmdMessageHandler nextHandler;

	public CmdMsgHandler(ServerState state, Logger logger)   {
		this.state = state;
		this.logger = logger;
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if(cmdMessage.hasMessage ())  {
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
