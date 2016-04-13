package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class WrkFailureMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(WrkFailureMessageHandler.class);;
	private IWrkMessageHandler nextHandler;

	public WrkFailureMessageHandler(ServerState state) {
		this.state = state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (workMessage.hasErr()) {
			handle(workMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(workMessage, channel);
			} else {
				logger.info("*****No Handler available*****");
			}
		}
	}

	private void handle(WorkMessage workMessage, Channel channel) {
		Common.Failure err = workMessage.getErr();
		logger.error("failure from " + workMessage.getHeader().getNodeId());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
