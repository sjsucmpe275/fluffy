package gash.router.server.messages.wrk_messages.handlers;

import gash.router.server.ServerState;
import gash.router.server.WorkChannelHandler;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class ElectionMessageHandler implements IWrkMessageHandler {

	private WorkChannelHandler workHandler;
	private IWrkMessageHandler nextHandler;

	public ElectionMessageHandler(WorkChannelHandler workChannelHandler) {

		this.workHandler = workChannelHandler;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (!workMessage.hasLeader() && nextHandler != null) {
			nextHandler.handleMessage(workMessage, channel);
			return;
		}
		if (nextHandler == null) {
			System.out.println("*****No Handler available*****");
			//return;
		}
		System.out.println("Election Message Received forwarding to state");
		workHandler.getServerState().getCurrentState().handleMessage(workMessage, channel);

	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {

		this.nextHandler = nextHandler;
	}
}
