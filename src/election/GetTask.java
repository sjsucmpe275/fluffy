package election;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import gash.router.server.ServerState;
import pipe.work.Work.WorkMessage;

interface IGetTaskNotifier {
	void notifyGetTaskCompletion();
}

public class GetTask implements Callable<WorkMessage>{

	private ServerState state;
	private int destination;
	private IGetTaskNotifier notifier;
	private WorkMessage requestMsg;
	
	
	public GetTask(ServerState state, int destination, IGetTaskNotifier notifier, WorkMessage requestMsg) {
		this.destination = destination;
		this.notifier = notifier;
		this.requestMsg = requestMsg;
		this.state = state;
	}
	
	public void handleResponse(WorkMessage workMessage) {
		
	}

	@Override
	public WorkMessage call() throws Exception {
		return null;
	}

}
