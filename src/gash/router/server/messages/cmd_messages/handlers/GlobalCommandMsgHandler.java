package gash.router.server.messages.cmd_messages.handlers;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class GlobalCommandMsgHandler implements ICmdMessageHandler  {

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
