package gash.router.server.messages.cmd_messages.handlers;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

public class GlobalCommandMsgHandler implements ICmdMessageHandler  {

	private final Logger logger = LoggerFactory.getLogger("Global Command Message Handler");
	private ICmdMessageHandler nextHandler;
	
	public GlobalCommandMsgHandler()   {
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

	}
	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
		
	}
}
