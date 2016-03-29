package gash.router.server.messages.wrk_messages.handlers;

import io.netty.channel.Channel;
import pipe.work.Work.*;


/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public interface IWrkMessageHandler {
	void handleMessage(WorkMessage workMessage, Channel channel);

	void setNextHandler(IWrkMessageHandler nextHandler);
}
