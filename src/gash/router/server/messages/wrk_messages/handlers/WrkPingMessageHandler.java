package gash.router.server.messages.wrk_messages.handlers;

import gash.router.server.WorkChannelHandler;
import io.netty.channel.Channel;
import pipe.work.Work;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class WrkPingMessageHandler implements IWrkMessageHandler {

	private final WorkChannelHandler workChannelHandler;
	private IWrkMessageHandler nextHandler;

	public WrkPingMessageHandler(WorkChannelHandler workChannelHandler) {
		this.workChannelHandler = workChannelHandler;
	}

	@Override
	public void handleMessage(Work.WorkMessage workMessage, Channel channel) {
		if(! workMessage.hasBeat () && nextHandler != null)  {
			nextHandler.handleMessage (workMessage, channel);
			return;
		}

		if(nextHandler == null) {
			System.out.println("*****No Handler available*****");
			return;
		}

		workChannelHandler.getLogger ().info("ping from " + workMessage.getHeader().getNodeId());
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
