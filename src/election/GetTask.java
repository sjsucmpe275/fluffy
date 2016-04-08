package election;

import gash.router.server.ServerState;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import storage.Storage;

import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

interface IGetTaskListener {
	void notifyGetTaskCompletion(String requestKey);
}

public class GetTask implements Runnable{

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
		receivedMessages = new TreeSet<> ();
		blockingQueue = new LinkedBlockingQueue<> ();
	}
	
	public void handleResponse(WorkMessage workMessage) {
		Storage.Response response = workMessage.getTask ().getTaskMessage ().getResponse ();
		if(response.getSequenceNo () == 0)  {
			size = response.getMetaData ().getSeqSize ();
		}
		receivedMessages.add (response.getSequenceNo ());
		blockingQueue.add (workMessage);
	}

	@Override
	public void run() {

		state.getTasks ().addTask (requestMsg.getTask ());
		state.getEmon ().broadcastMessage (requestMsg);

		while(receivedMessages.size () != size) {
			try {
				WorkMessage workMessage = blockingQueue.take ();

				if(requestMsg.getHeader ().getNodeId () == state.getConf ().getNodeId ())   {
					state.getQueues ().getFromWorkServer ().put (workMessage.getTask ().getTaskMessage ());
					continue;
				}

				WorkMessage.Builder wb = WorkMessage.newBuilder(workMessage);
				Common.Header.Builder hb = Common.Header
						.newBuilder(workMessage.getHeader());
				hb.setDestination(requestMsg.getHeader ().getNodeId ());
				wb.setHeader(hb);
				wb.setSecret(1);

				state.getEmon ().broadcastMessage (workMessage);
			} catch (InterruptedException e) {
				e.printStackTrace ();
			}
		}
		receivedMessages.clear ();
		listener.notifyGetTaskCompletion (requestMsg.getTask ().getTaskMessage ().getQuery ().getKey ());
	}

}
