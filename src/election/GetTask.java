package election;

import gash.router.server.ServerState;
import pipe.work.Work.WorkMessage;
import storage.Storage;

public class GetTask extends AbstractTask implements Runnable {

	public GetTask(ServerState state, ITaskListener listener, WorkMessage requestMsg) {
		super(state, listener, requestMsg);
	}

	@Override
	public void handleResponse(WorkMessage workMessage) {
		Storage.Response response = workMessage.getTask().getTaskMessage().getResponse();

		if (response.getSequenceNo() == 0) {
			size = response.getMetaData().getSeqSize();
		}

		if (!receivedMessages.contains(response.getSequenceNo())) {
			receivedMessages.add(response.getSequenceNo());
			blockingQueue.add(workMessage);
		}
	}

	@Override
	public void run() {

		state.getTasks().addTask(requestMsg.getTask());
		state.getEmon().broadcastMessage(requestMsg);

		run0();
		cleanup();
	}

	@Override
	protected void cleanup() {
		receivedMessages.clear();
		listener.notifyTaskCompletion(
			requestMsg.getTask().getTaskMessage().getQuery().getKey());
	}

}
