package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class ElectionMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(ElectionMessageHandler.class);
	private IWrkMessageHandler nextHandler;

	public ElectionMessageHandler(ServerState state) {
		this.state = state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if(workMessage.hasLeader ())    {
			handle(workMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (workMessage, channel);
			}else   {
				logger.info ("*******No handler available********");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {
		logger.info ("Election Message Received forwarding to state");
		synchronized (state)    {
			state.getCurrentState().handleMessage(workMessage, channel);
		}
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
