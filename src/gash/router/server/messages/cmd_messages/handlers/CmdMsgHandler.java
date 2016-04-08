package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

/**
 * @author: codepenman.
 * @date: 3/27/16
 */
public class CmdMsgHandler implements ICmdMessageHandler {

	private final Logger logger = LoggerFactory.getLogger("Command Message Handler");
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
				logger.info("*****No Handler available*****");
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
