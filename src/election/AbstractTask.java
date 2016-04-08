package election;

import gash.router.server.ServerState;
import pipe.common.Common;
import pipe.work.Work;
import pipe.work.Work.WorkMessage;
import routing.Pipe;
import storage.Storage;

import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author: codepenman.
 * @date: 4/8/16
 */
public abstract class AbstractTask {

	protected ServerState state;
	protected ITaskListener listener;
	protected WorkMessage requestMsg;
	protected int size = Integer.MAX_VALUE;
	protected final Object lock = new Object ();
	protected TreeSet<Integer> receivedMessages;
	protected LinkedBlockingQueue<WorkMessage> blockingQueue;

	public AbstractTask(ServerState state, ITaskListener listener, WorkMessage requestMsg)   {
		this.listener = listener;
		this.requestMsg = requestMsg;
		this.state = state;
		this.receivedMessages = new TreeSet<> ();
		this.blockingQueue = new LinkedBlockingQueue<>();
	}

	abstract void handleResponse(WorkMessage workMessage);

	/**
	 * @param workMessage
	 * @throws InterruptedException
	 */
	protected void handleMessage(WorkMessage workMessage)
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


	protected WorkMessage.Builder constructFailureMessage(WorkMessage requestMsg) {
		Pipe.CommandMessage.Builder cb = Pipe.CommandMessage.newBuilder();

		Common.Header.Builder hb = Common.Header
				.newBuilder(requestMsg.getHeader());
		hb.setDestination(requestMsg.getHeader().getNodeId());

		Common.Failure.Builder fb = Common.Failure.newBuilder();
		fb.setMessage("Data not present");
		fb.setId(102);

		Storage.Response.Builder rb = Storage.Response.newBuilder();
		rb.setSuccess(false);
		rb.setFailure(fb);

		cb.setHeader(hb);
		cb.setResponse(rb);

		Work.Task.Builder tb = Work.Task.newBuilder();
		tb.setSeqId(-1);
		tb.setSeriesId(requestMsg.getTask().getTaskMessage()
				.getQuery().getKey().hashCode());
		tb.setTaskMessage(cb);

		WorkMessage.Builder wb = WorkMessage.newBuilder();

		wb.setHeader(hb);
		wb.setSecret(1);
		wb.setTask(tb);
		return wb;
	}
	
	protected void run0()    {
		while (receivedMessages.size() != size + 1) {
			try {
				WorkMessage workMessage = blockingQueue.poll (20, TimeUnit.SECONDS);

				if (workMessage == null) {
					WorkMessage.Builder wb = constructFailureMessage (requestMsg);
					handleMessage(wb.build());
					cleanup();
					return;
				}

				handleMessage(workMessage);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract void cleanup();
}
