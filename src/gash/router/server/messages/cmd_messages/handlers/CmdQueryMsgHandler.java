package gash.router.server.messages.cmd_messages.handlers;

import com.google.protobuf.ByteString;
import dbhandlers.DatabaseFactory;
import dbhandlers.IDBHandler;
import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import pipe.common.Common;
import routing.Pipe.CommandMessage;
import storage.Storage;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

/**
 * @author: codepenman.
 * @date: 3/28/16
 */
public class CmdQueryMsgHandler implements ICmdMessageHandler {

	private final CommandChannelHandler cmdChannelHandler;
	private ICmdMessageHandler nextHandler;
	private final IDBHandler dbHandler;

	public CmdQueryMsgHandler(CommandChannelHandler cmdChannelHandler) throws Exception {
		this.cmdChannelHandler = cmdChannelHandler;
		dbHandler = new DatabaseFactory ().getDatabaseHandler (cmdChannelHandler.getConf ().getDatabase ());
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception {
		if(cmdMessage.hasQuery ())  {
			handle(cmdMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (cmdMessage, channel);
			}else   {
				System.out.println("*****No Handler available*****");
			}
		}
}

	private void handle(CommandMessage cmdMessage, Channel channel) throws Exception {

		Storage.Query query = cmdMessage.getQuery();
		Common.Header.Builder hb = buildHeader();
		Storage.Response.Builder rb = Storage.Response.newBuilder();
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		String key = query.getKey();

		switch (query.getAction()) {
			case GET:
				rb.setAction(query.getAction());
				Object data = dbHandler.get(key);
				if (data == null) {

					Common.Failure.Builder fb = Common.Failure.newBuilder();
					fb.setMessage("Key not present");
					fb.setId(101);

					rb.setSuccess(false);
					rb.setFailure(fb);

					cb.setHeader(hb);
					cb.setResponse(rb);
					channel.writeAndFlush(cb.build());
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
					channel.writeAndFlush(cb.build());

				}
				break;

			case DELETE:
				data = dbHandler.remove(key);
				if (data == null) {
					Common.Failure.Builder fb = Common.Failure.newBuilder();
					fb.setMessage("Key not present");
					fb.setId(101);

					rb.setSuccess(false);
					rb.setFailure(fb);

					cb.setHeader(hb);
					cb.setResponse(rb);
					channel.writeAndFlush(cb.build());
				}  else {
					rb.setSuccess(true);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(bos);

					os.writeObject(data);
					rb.setData(ByteString.copyFrom(bos.toByteArray()));
					rb.setInfomessage("Action completed successfully!");
					rb.setKey(key);

					cb.setHeader(hb);
					cb.setResponse(rb);
					channel.writeAndFlush(cb.build());
				}
				break;

			case STORE:

				if (query.hasKey()) {
					key = dbHandler.put(query.getKey(), 0, query.getData().toByteArray());
				} else {
					key = dbHandler.store(query.getData().toByteArray());
				}

				rb.setAction(query.getAction());
				rb.setKey(key);
				rb.setSuccess(true);
				rb.setInfomessage("Data stored successfully at key: " + key);

				cb.setHeader(hb);
				cb.setResponse(rb);
				channel.writeAndFlush(cb.build());
				break;

			case UPDATE:
				key = dbHandler.put(query.getKey(),0, query.getData().toByteArray());
				rb.setAction(query.getAction());
				rb.setKey(key);
				rb.setSuccess(true);
				rb.setInfomessage("Data stored successfully at key: " + key);

				cb.setHeader(hb);
				cb.setResponse(rb);
				channel.writeAndFlush(cb.build());
				break;

			default:
				cmdChannelHandler.getLogger ().info("Default case!");
				break;
		}
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	private Common.Header.Builder buildHeader() {
		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(cmdChannelHandler.getConf ().getNodeId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);
		return hb;
	}
}
