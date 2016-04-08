package election;

import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import gash.router.server.ServerState;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
import storage.Storage;
import storage.Storage.Response;

interface IGetTaskListener {

	void notifyGetTaskCompletion(String requestKey);
}

public class GetTask implements Runnable {

	private ServerState state;
	private IGetTaskListener listener;
	private WorkMessage requestMsg;
	private int size = Integer.MAX_VALUE;
	private TreeSet<Integer> receivedMessages;
	private LinkedBlockingQueue<WorkMessage> blockingQueue;

	public GetTask(ServerState state, IGetTaskListener listener, WorkMessage requestMsg) {
		this.listener = listener;
		this.requestMsg = requestMsg;
		this.state = state;
		this.receivedMessages = new TreeSet<>();
		this.blockingQueue = new LinkedBlockingQueue<>();
	}

	public void handleResponse(WorkMessage workMessage) {
		Storage.Response response = workMessage.getTask().getTaskMessage()
			.getResponse();

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

		while (receivedMessages.size() != size + 1) {
			try {
				WorkMessage workMessage = blockingQueue.poll(20,
					TimeUnit.SECONDS);

				if (workMessage == null) {

					CommandMessage.Builder cb = CommandMessage.newBuilder();

					Header.Builder hb = Header
						.newBuilder(workMessage.getHeader());
					hb.setDestination(requestMsg.getHeader().getNodeId());

					Common.Failure.Builder fb = Common.Failure.newBuilder();
					fb.setMessage("Data not present");
					fb.setId(102);

					Response.Builder rb = Response.newBuilder();
					rb.setSuccess(false);
					rb.setFailure(fb);

					cb.setHeader(hb);
					cb.setResponse(rb);

					Task.Builder tb = Task.newBuilder();
					tb.setSeqId(-1);
					tb.setSeriesId(requestMsg.getTask().getTaskMessage()
						.getQuery().getKey().hashCode());
					tb.setTaskMessage(cb);

					WorkMessage.Builder wb = WorkMessage.newBuilder();

					wb.setHeader(hb);
					wb.setSecret(1);
					wb.setTask(tb);
					handleMessage(wb.build());
					cleanup();
					return;
				}

				handleMessage(workMessage);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		cleanup();
	}

	/**
	 * @param workMessage
	 * @throws InterruptedException
	 */
	private void handleMessage(WorkMessage workMessage)
		throws InterruptedException {
		if (requestMsg.getHeader().getNodeId() == state.getConf().getNodeId()) {
			state.getQueues().getFromWorkServer()
				.put(workMessage.getTask().getTaskMessage());
			return;
		}

		WorkMessage.Builder wb = WorkMessage.newBuilder(workMessage);
		Common.Header.Builder hb = Common.Header
			.newBuilder(workMessage.getHeader());
		hb.setDestination(requestMsg.getHeader().getNodeId());
		wb.setHeader(hb);
		wb.setSecret(1);

		state.getEmon().broadcastMessage(workMessage);
	}
	
	private void cleanup() {
		receivedMessages.clear();
		listener.notifyGetTaskCompletion(
			requestMsg.getTask().getTaskMessage().getQuery().getKey());
	}

}
