package gash.router.server.messages.wrk_messages.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.MessageAdapter;
import gash.router.server.Router;
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
	private Router router;
	
	public TaskMessageHandler(ServerState state) {
		this.state = state;
		this.router = new Router(state);
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

		CommandMessage msg = workMessage.getTask().getTaskMessage();
		System.out.println("Handling task message...");
		System.out.println(workMessage);
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		if (msg.hasQuery()) {
			state.getCurrentState().handleCmdQuery(workMessage, channel);
		} else if (msg.hasResponse()) {
			state.getCurrentState().handleCmdResponse(workMessage, channel);
		} else if (msg.hasErr()) {
			state.getCurrentState().handleCmdError(workMessage, channel);
		} else {
			System.out.println("This should never reach...");
		}
	}

	@Override
	public void setNextHandler(IWrkMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	@Override
	public void run() {
		System.out.println("Starting work server queue manager thread...");
		while (forever) {
			CommandMessage msg;
			
			try {
				msg = state.getQueues().getToWorkServer().take();
				System.out.println("$$$$$$$$$$$$$Fetched task for worker...");
				System.out.println(msg);
				WorkMessage wrkMsg = MessageAdapter
					.getWorkMessageToLeader(state, msg);

				if (state.getLeaderId() == state.getConf().getNodeId()) {
					handleMessage(wrkMsg, null);
					return;
				} 
				wrkMsg = router.publish(wrkMsg);
				
				if (wrkMsg != null) {
					handleMessage(wrkMsg, null);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
