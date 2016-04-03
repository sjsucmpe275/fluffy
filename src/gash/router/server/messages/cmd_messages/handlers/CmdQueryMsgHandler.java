package gash.router.server.messages.cmd_messages.handlers;

import com.google.protobuf.ByteString;
import dbhandlers.DatabaseFactory;
import dbhandlers.IDBHandler;
import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.*;
import storage.Storage;
import storage.Storage.Metadata;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * @author: codepenman.
 * @date: 3/28/16
 */
public class CmdQueryMsgHandler implements ICmdMessageHandler {

	private final CommandChannelHandler cmdChannelHandler;
	private ICmdMessageHandler nextHandler;
	private final IDBHandler dbHandler;

	public CmdQueryMsgHandler(CommandChannelHandler cmdChannelHandler)
		throws Exception {
		this.cmdChannelHandler = cmdChannelHandler;
		dbHandler = new DatabaseFactory()
			.getDatabaseHandler(cmdChannelHandler.getConf().getDatabase());
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel)
		throws Exception {
		if (cmdMessage.hasQuery()) {
			handleTaskMessage(cmdMessage, channel);
		} else {
			if (nextHandler != null) {
				nextHandler.handleMessage(cmdMessage, channel);
			} else {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handleTaskMessage(CommandMessage cmdMessage, Channel channel) {

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(
			cmdChannelHandler.getConf().getNodeId(), -1);

		Task.Builder t = Task.newBuilder();
		t.setSeqId(cmdMessage.getQuery().getSequenceNo());
		t.setSeriesId(cmdMessage.getQuery().getKey().hashCode());
		t.setTaskMessage(cmdMessage);

		wb.setHeader(header);
		wb.setSecret(1);
		wb.setTask(t);

		channel.writeAndFlush(wb.build());

	}

	private Header.Builder createHeader(int nodeId, int destination) {
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(nodeId);
		header.setDestination(destination);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());
		return header;
	}

	private void handle(CommandMessage cmdMessage, Channel channel)
		throws Exception {

		Storage.Query query = cmdMessage.getQuery();
		Common.Header.Builder hb = buildHeader();
		Storage.Response.Builder rb = Storage.Response.newBuilder();
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		String key = query.getKey();

		switch (query.getAction()) {
		case GET:
			rb.setAction(query.getAction());
			Map<Integer, byte[]> dataMap = dbHandler.get(key);
			if (dataMap.isEmpty()) {

				Common.Failure.Builder fb = Common.Failure.newBuilder();
				fb.setMessage("Key not present");
				fb.setId(101);

				rb.setSuccess(false);
				rb.setFailure(fb);

				cb.setHeader(hb);
				cb.setResponse(rb);
				// enqueue instead of writing
				// channel.writeAndFlush(cb.build());
				cmdChannelHandler.enqueue(cb.build());
			} else {
				System.out.println(dataMap);
				for (Integer sequenceNo : dataMap.keySet()) {
					rb.setSuccess(true);
					if (sequenceNo == 0) {
						rb.setMetaData(
							Metadata.parseFrom(dataMap.get(sequenceNo)));
					} else {
						rb.setData(
							ByteString.copyFrom(dataMap.get(sequenceNo)));
					}
					rb.setKey(key);
					rb.setSequenceNo(sequenceNo);

					cb.setHeader(hb);
					cb.setResponse(rb);
					// enqueue instead of writing
					// channel.write(cb.build());
					cmdChannelHandler.enqueue(cb.build());
				}
				channel.flush();
			}
			break;

		case DELETE:
			Object data = dbHandler.remove(key);
			if (data == null) {
				Common.Failure.Builder fb = Common.Failure.newBuilder();
				fb.setMessage("Key not present");
				fb.setId(101);

				rb.setSuccess(false);
				rb.setFailure(fb);

				cb.setHeader(hb);
				cb.setResponse(rb);
				// enqueue instead of writing
				// channel.writeAndFlush(cb.build());
				cmdChannelHandler.enqueue(cb.build());
			} else {
				rb.setSuccess(true);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(bos);

				os.writeObject(data);
				rb.setData(ByteString.copyFrom(bos.toByteArray()));
				rb.setInfomessage("Action completed successfully!");
				rb.setKey(key);

				cb.setHeader(hb);
				cb.setResponse(rb);
				// enqueue instead of writing
				// channel.writeAndFlush(cb.build());
				cmdChannelHandler.enqueue(cb.build());
			}
			break;

		case STORE:

			if (query.hasKey()) {

				if (query.hasMetadata()) {
					dbHandler.put(query.getKey(), 0,
						query.getMetadata().toByteArray());
				} else {
					key = dbHandler.put(query.getKey(), query.getSequenceNo(),
						query.getData().toByteArray());
				}
			} else {
				key = dbHandler.store(query.getData().toByteArray());

				// TODO temporary fix
				Metadata.Builder mb = Metadata.newBuilder();
				mb.setSeqSize(1);
				mb.setTime(System.currentTimeMillis());
				mb.setSize(query.getData().size());
				dbHandler.put(key, 0, mb.build().toByteArray());
			}

			rb.setAction(query.getAction());
			rb.setKey(key);
			rb.setSuccess(true);
			rb.setSequenceNo(query.getSequenceNo());
			rb.setInfomessage("Data stored successfully at key: " + key);

			cb.setHeader(hb);
			cb.setResponse(rb);
			// enqueue instead of writing
			// channel.writeAndFlush(cb.build());
			cmdChannelHandler.enqueue(cb.build());
			break;

		case UPDATE:
			key = dbHandler.put(query.getKey(), query.getSequenceNo(),
				query.getData().toByteArray());
			rb.setAction(query.getAction());
			rb.setKey(key);
			rb.setSuccess(true);
			rb.setInfomessage("Data stored successfully at key: " + key);

			cb.setHeader(hb);
			cb.setResponse(rb);
			// enqueue instead of writing
			// channel.writeAndFlush(cb.build());
			cmdChannelHandler.enqueue(cb.build());
			break;

		default:
			cmdChannelHandler.getLogger().info("Default case!");
			break;
		}
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	private Common.Header.Builder buildHeader() {
		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(cmdChannelHandler.getConf().getNodeId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);
		return hb;
	}
}
