package election;

import gash.router.server.ServerState;
import gash.router.server.tasks.IReplicationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import storage.Storage;

import java.util.List;

/**
 * @author: codepenman.
 * @date: 4/8/16
 */
public class StoreTask extends AbstractTask implements Runnable {

	private static Logger logger = LoggerFactory.getLogger("Store Task");
	private IReplicationStrategy replicationStrategy;
	private List<Integer> availableNodes;

	public StoreTask(ServerState state, ITaskListener listener, WorkMessage requestMsg, IReplicationStrategy replicationStrategy, List<Integer> availableNodes) {
		super(state, listener, requestMsg);
		this.replicationStrategy = replicationStrategy;
		this.availableNodes = availableNodes;
		this.size = requestMsg.getTask ().getTaskMessage ().getQuery ().getMetadata ().getSeqSize ();
	}

	public void handleRequest(WorkMessage workMessage, List<Integer> availableNodes) {
		replicateData (workMessage, availableNodes);
	}

	@Override
	public void handleResponse(WorkMessage workMessage) {
		Storage.Response response = workMessage.getTask().getTaskMessage()
				.getResponse();

		if (!receivedMessages.contains(response.getSequenceNo())) {
			receivedMessages.add(response.getSequenceNo());
			blockingQueue.add(workMessage);
		}
	}

	@Override
	public void run() {
		replicateData (requestMsg, availableNodes);
		run0 ();
		cleanup();
	}

	private void replicateData(WorkMessage workMessage, List<Integer> availableNodes) {
		List<Integer> replicationNodes = replicationStrategy.getNodeIds(availableNodes);

		for (Integer destinationId : replicationNodes) {
			WorkMessage.Builder wb = WorkMessage.newBuilder (workMessage);
			Common.Header.Builder hb = Common.Header.newBuilder (workMessage.getHeader ());
			hb.setNodeId (state.getConf ().getNodeId ());
			hb.setDestination (destinationId);
			wb.setHeader (hb);
			wb.setSecret (1);

			if (destinationId != state.getConf ().getNodeId ()) {
				state.getEmon ().broadcastMessage (wb.build ());
			} else {
				state.getTasks ().addTask (workMessage.getTask ());
			}
		}
	}

	@Override
	protected void cleanup() {
		try {
			/*Here I goto sleep for some time to ensure I don't send redundant messages over the network.
			This Sleep is not required, if each node know its distance from Leader and update its max hops based on that..
			In the current solution this is the tradeoff I have*/
			logger.info("I received all the messages from client");
			Thread.sleep (size * 2);
		} catch (InterruptedException e) {
			e.printStackTrace ();
		}
		receivedMessages.clear();
		listener.notifyTaskCompletion (
				requestMsg.getTask().getTaskMessage().getQuery().getKey());
	}
}
