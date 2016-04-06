/**
 * 
 */
package gash.router.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.messages.wrk_messages.handlers.BeatMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.ElectionMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.IWrkMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.StateMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.TaskMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.WrkFailureMessageHandler;
import gash.router.server.messages.wrk_messages.handlers.WrkPingMessageHandler;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

/**
 * @author saurabh
 *
 */
public class WorkServerQueueManager extends Thread {

	private QueueManager queues;
	private boolean forever = true;
	private ServerState state;
	private Logger logger = LoggerFactory.getLogger(WorkServerQueueManager.class);
	private IWrkMessageHandler wrkMessageHandler;
	
	public WorkServerQueueManager(QueueManager queues, ServerState state) {
		this.queues = queues;
		this.state = state;
		initializeMessageHandlers();
	}

	private void initializeMessageHandlers() {
		//Define Handlers
		IWrkMessageHandler beatMessageHandler = new BeatMessageHandler (state, logger);
		IWrkMessageHandler failureMessageHandler = new WrkFailureMessageHandler (state, logger);
		IWrkMessageHandler pingMessageHandler = new WrkPingMessageHandler (state, logger);
		IWrkMessageHandler stateMessageHandler = new StateMessageHandler (state, logger);
		IWrkMessageHandler taskMessageHandler = new TaskMessageHandler (state, logger);
		IWrkMessageHandler electionMessageHandler=new ElectionMessageHandler(state, logger);

		//Chain all the handlers
		beatMessageHandler.setNextHandler (failureMessageHandler);
		failureMessageHandler.setNextHandler (pingMessageHandler);
		pingMessageHandler.setNextHandler (stateMessageHandler);
		stateMessageHandler.setNextHandler (taskMessageHandler);
		taskMessageHandler.setNextHandler(electionMessageHandler);

		//Define the start of Chain
		wrkMessageHandler = beatMessageHandler;
	}
	
	@Override
	public void run() {

		while (forever) {
			CommandMessage msg;
			try {
				msg = queues.getToWorkServer().take();
				System.out.println("Fetched task for worker...");
				System.out.println(msg);
				WorkMessage wrkMsg = MessageAdapter.getWorkMessage(state.getConf(),
					msg);
				
				// May need to route this message first...
				wrkMessageHandler.handleMessage(wrkMsg, null);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
