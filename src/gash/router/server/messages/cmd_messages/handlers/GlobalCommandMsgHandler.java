package gash.router.server.messages.cmd_messages.handlers;

import org.slf4j.Logger;

import gash.router.container.RoutingConf;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class GlobalCommandMsgHandler implements ICmdMessageHandler  {
	private final RoutingConf conf;
	private final Logger logger;
	private ICmdMessageHandler nextHandler;
	
	public GlobalCommandMsgHandler(RoutingConf conf, Logger logger)   {
		this.conf = conf;
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
