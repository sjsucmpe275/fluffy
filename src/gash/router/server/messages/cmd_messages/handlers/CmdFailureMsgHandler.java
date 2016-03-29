package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

/**
 * Created by codepenman on 3/27/16.
 */
public class CmdFailureMsgHandler implements ICmdMessageHandler {

	private final CommandChannelHandler cmdChannelHandler;
	private ICmdMessageHandler nextHandler;

	public CmdFailureMsgHandler(CommandChannelHandler cmdChannelHandler)  {
		this.cmdChannelHandler = cmdChannelHandler;
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if(! cmdMessage.hasErr () && nextHandler != null)  {
			nextHandler.handleMessage (cmdMessage, channel);
			return;
		}

		if(nextHandler == null) {
			System.out.println("*****No Handler available*****");
			return;
		}


	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
