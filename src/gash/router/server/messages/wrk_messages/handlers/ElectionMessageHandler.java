package gash.router.server.messages.wrk_messages.handlers;

import gash.router.server.WorkChannelHandler;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class ElectionMessageHandler implements IWrkMessageHandler {

	private final WorkChannelHandler workHandler;
	private IWrkMessageHandler nextHandler;

	public ElectionMessageHandler(WorkChannelHandler workChannelHandler) {
		this.workHandler = workChannelHandler;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (!workMessage.hasBeat() && nextHandler != null) {
			nextHandler.handleMessage(workMessage, null);
			return;
		}
		if (nextHandler == null) {
			System.out.println("*****No Handler available*****");
			return;
		}
		System.out.println("Election Message Received forwarding to state");
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {

		this.nextHandler = nextHandler;
	}
}
