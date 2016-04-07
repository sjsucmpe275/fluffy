package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

/**
 * @author: codepenman.
 * @date: 3/27/16
 */
public class CmdMsgHandler implements ICmdMessageHandler {

	private ICmdMessageHandler nextHandler;

	public CmdMsgHandler(CommandChannelHandler commandChannelHandler)   {
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
		channel.writeAndFlush (cmdMessage);
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
