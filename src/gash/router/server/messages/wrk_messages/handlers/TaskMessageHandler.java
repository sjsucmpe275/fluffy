package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.MessageAdapter;
import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

/**
 * @author: codepenman
 * @date: 28/03/2016
 */
public class TaskMessageHandler implements IWrkMessageHandler, Runnable {

	private final ServerState state;
	private final Logger logger = LoggerFactory.getLogger(TaskMessageHandler.class);
	private IWrkMessageHandler nextHandler;
	private boolean forever = true;

	public TaskMessageHandler(ServerState state) {
		this.state = state;
	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		if (workMessage.hasTask()) {
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

		CommandMessage msg = workMessage.getTask().getTaskMessage();
		if (msg.hasQuery()) {
			state.getCurrentState().handleCmdQuery(workMessage, channel);
		} else if (msg.hasResponse()) {
			state.getCurrentState ().handleCmdResponse (workMessage, channel);
		} else if (msg.hasErr()) {
				state.getCurrentState().handleCmdError(workMessage, channel);
		} else {
			logger.info("This should never reach...");
		}
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	@Override
	public void run() {
		logger.info("Starting work server queue manager thread...");
		while (forever) {
			CommandMessage msg;

			try {
				msg = state.getQueues().getToWorkServer().take();
				WorkMessage wrkMsg = MessageAdapter.getWorkMessageToLeader(state, msg);

				if (state.getLeaderId() == state.getConf().getNodeId()) {
					handleMessage(wrkMsg, null);
				}
				state.getEmon().broadcastMessage(wrkMsg);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
