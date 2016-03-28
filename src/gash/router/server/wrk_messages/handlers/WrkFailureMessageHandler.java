package gash.router.server.wrk_messages.handlers;

import gash.router.server.WorkChannelHandler;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.work.Work.*;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class WrkFailureMessageHandler implements IWrkMessageHandler{

	private final WorkChannelHandler workChannelHandler;
	private IWrkMessageHandler nextHandler;

	public WrkFailureMessageHandler(WorkChannelHandler workChannelHandler)   {
		this.workChannelHandler = workChannelHandler;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if(! workMessage.hasBeat () && nextHandler != null)  {
			nextHandler.handleMessage (workMessage, null);
			return;
		}

		if(nextHandler == null) {
			System.out.println("*****No Handler available*****");
			return;
		}

		Common.Failure err = workMessage.getErr();
		workChannelHandler.getLogger().error("failure from " + workMessage.getHeader().getNodeId());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
