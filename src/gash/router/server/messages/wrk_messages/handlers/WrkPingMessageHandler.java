package gash.router.server.messages.wrk_messages.handlers;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class WrkPingMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger;
	private IWrkMessageHandler nextHandler;

	public WrkPingMessageHandler(ServerState state, Logger logger) {
		this.state = state;
		this.logger = logger;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if(workMessage.hasPing ())  {
			handle(workMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (workMessage, channel);
			}else   {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {

		logger.info("ping from " + workMessage.getHeader().getNodeId());
		//Todo: I commented this code to avoid infinite loop. Will update later
				/*boolean p = msg.getPing();
				WorkMessage.Builder rb = WorkMessage.newBuilder();
				rb.setPing(true);
				channel.write(rb.build());*/
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
