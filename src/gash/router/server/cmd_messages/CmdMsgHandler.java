package gash.router.server.cmd_messages;

import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import routing.Pipe.*;

/**
 * Created by codepenman on 3/27/16.
 */
public class CmdMsgHandler implements ICmdMessageHandler {

	private final CommandChannelHandler cmdChannelHandler;
	private ICmdMessageHandler nextHandler;

	public CmdMsgHandler(CommandChannelHandler cmdChannelHandler)   {
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

		cmdChannelHandler.getLogger ().info(cmdMessage.getMessage());
	}

	@Override
	public void nextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
