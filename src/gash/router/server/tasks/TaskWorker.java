/**
 * 
 */
package gash.router.server.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import dbhandlers.DatabaseFactory;
import dbhandlers.IDBHandler;
import gash.router.server.ServerState;
import pipe.common.Common;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
import storage.Storage;
import storage.Storage.Metadata;
import storage.Storage.Query;

/**
 * @author saurabh
 *
 */
public class TaskWorker extends Thread {

	private final Logger logger = LoggerFactory.getLogger(TaskWorker.class);

	private ServerState state;
	private IDBHandler dbHandler;

	private boolean forever;

	public TaskWorker(ServerState state) {
		this.state = state;
		this.forever = true;

		DatabaseFactory factory = new DatabaseFactory();
		try {
			this.dbHandler = factory
				.getDatabaseHandler(state.getConf().getDatabase());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		logger.info("Starting task worker : " + Thread.currentThread());
		while (forever) {
			if (state.getTasks().shouldSteal()) {
				startStealing();
			}
			Task task = state.getTasks().dequeue();
			CommandMessage msg = task.getTaskMessage();
			Query query = msg.getQuery();

			Common.Header.Builder hb = buildHeader();
			Storage.Response.Builder rb = Storage.Response.newBuilder();
			CommandMessage.Builder cb = CommandMessage.newBuilder(msg);
			String key = query.getKey();

			switch (query.getAction()) {

			case GET:
				rb.setAction(query.getAction());
				Map<Integer, byte[]> dataMap = dbHandler.get(key);
				if (dataMap.isEmpty()) {
					logger.info("Key ", key, " not present in the database");
					Common.Failure.Builder fb = Common.Failure.newBuilder();
					fb.setMessage("Key not present");
					fb.setId(101);

					rb.setSuccess(false);
					rb.setFailure(fb);

					cb.setHeader(hb);
					cb.setResponse(rb);
				} else {
					logger.info("Retrieved sequenceNumbers", dataMap);
					for (Integer sequenceNo : dataMap.keySet()) {
						rb.setSuccess(true);

						if (sequenceNo == 0) {

							try {
								rb.setMetaData(Metadata
									.parseFrom(dataMap.get(sequenceNo)));
							} catch (InvalidProtocolBufferException e) {
								e.printStackTrace();
							}
						} else {
							rb.setData(
								ByteString.copyFrom(dataMap.get(sequenceNo)));
						}
						rb.setKey(key);
						rb.setSequenceNo(sequenceNo);

						cb.setHeader(hb);
						cb.setResponse(rb);
					}
				}
				break;

			case DELETE:
				Map<Integer, byte[]> data = dbHandler.remove(key);
				if (data == null) {
					logger.info("Key ", key, " not present in the database");
					Common.Failure.Builder fb = Common.Failure.newBuilder();
					fb.setMessage("Key not present");
					fb.setId(101);

					rb.setSuccess(false);
					rb.setFailure(fb);

					cb.setHeader(hb);
					cb.setResponse(rb);
				} else {
					logger.info("Removed data: ", data);
					rb.setSuccess(true);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutputStream os;
					try {
						os = new ObjectOutputStream(bos);
						os.writeObject(data);
					} catch (IOException e) {
						e.printStackTrace();
					}

					rb.setData(ByteString.copyFrom(bos.toByteArray()));
					rb.setInfomessage("Action completed successfully!");
					rb.setKey(key);

					cb.setHeader(hb);
					cb.setResponse(rb);
				}
				break;

			case STORE:

				if (query.hasKey()) {

					if (query.hasMetadata()) {
						dbHandler.put(query.getKey(), 0,
							query.getMetadata().toByteArray());
					} else {
						key = dbHandler.put(query.getKey(),
							query.getSequenceNo(),
							query.getData().toByteArray());
					}
				} else {
					key = dbHandler.store(query.getData().toByteArray());

					// TODO temporary fix. Store without key can save only one
					// chunk of data.
					Metadata.Builder mb = Metadata.newBuilder();
					mb.setSeqSize(1);
					mb.setTime(System.currentTimeMillis());
					mb.setSize(query.getData().size());
					dbHandler.put(key, 0, mb.build().toByteArray());
				}

				System.out.println("Data saved at: " + key);

				rb.setAction(query.getAction());
				rb.setKey(key);
				rb.setSuccess(true);
				rb.setSequenceNo(query.getSequenceNo());
				rb.setInfomessage("Data stored successfully at key: " + key);

				cb.setHeader(hb);
				cb.setResponse(rb);
				break;

			case UPDATE:
				key = dbHandler.put(query.getKey(), query.getSequenceNo(),
					query.getData().toByteArray());

				logger.info("Data updated at key: ", key);

				rb.setAction(query.getAction());
				rb.setKey(key);
				rb.setSuccess(true);
				rb.setInfomessage("Data stored successfully at key: " + key);

				cb.setHeader(hb);
				cb.setResponse(rb);
				break;

			default:
				logger.info("Default case!");
				break;

			}

			// Send reply to the leader
			Task.Builder returnTask = Task.newBuilder();
			returnTask.setTaskMessage(cb);
			returnTask.setSeqId(task.getSeqId());
			returnTask.setSeriesId(task.getSeriesId());
			state.getEmon().broadcastMessage(wrapMessage(task));
		}
	}

	private void startStealing() {

	}

	public void shutdown() {
		forever = false;
	}

	private Common.Header.Builder buildHeader() {
		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(state.getLeaderId());
		return hb;
	}

	private WorkMessage wrapMessage(Task task) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(buildHeader());
		wb.setSecret(1);
		wb.setTask(task);
		return wb.build();
	}
}
