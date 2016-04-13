package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class WrkPingMessageHandler implements IWrkMessageHandler {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(WrkPingMessageHandler.class);
	private IWrkMessageHandler nextHandler;

	public WrkPingMessageHandler(ServerState state) {
		this.state = state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (workMessage.hasPing()) {
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

		logger.info("Ping from " + workMessage.getHeader().getNodeId());

		WorkMessage.Builder rb = WorkMessage.newBuilder();
		rb.setPing(true);
		channel.write(rb.build());
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
