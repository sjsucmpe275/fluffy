package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class TaskMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger;
	private IWrkMessageHandler nextHandler;

	public TaskMessageHandler(ServerState state, Logger logger) {
		this.state = state;
		this.logger = logger;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (workMessage.hasTask()) {
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
		Task t = workMessage.getTask();
		//
		state.getCurrentState().handleCmdQuery(workMessage, channel);
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
