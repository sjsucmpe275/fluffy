package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;


/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class StateMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(StateMessageHandler.class);
	private IWrkMessageHandler nextHandler;

	public StateMessageHandler(ServerState state) {
		this.state = state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {

		if (workMessage.hasState()) {
			handle(workMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(workMessage, channel);
			} else {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {
		WorkState s = workMessage.getState();
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
