package gash.router.server.messages.cmd_messages.handlers;

import dbhandlers.DatabaseFactory;
import dbhandlers.IDBHandler;
import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

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

		try {
			System.out.println("Adding command message to work server:");
			System.out.println(cmdMessage);
			cmdChannelHandler.getQueues().getToWorkServer().put(cmdMessage);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		WorkMessage.Builder wb = WorkMessage.newBuilder();
//		Header.Builder header = createHeader(
//			cmdChannelHandler.getConf().getNodeId(), -1);
//
//		Task.Builder t = Task.newBuilder();
//		t.setSeqId(cmdMessage.getQuery().getSequenceNo());
//		t.setSeriesId(cmdMessage.getQuery().getKey().hashCode());
//		t.setTaskMessage(cmdMessage);
//
//		wb.setHeader(header);
//		wb.setSecret(1);
//		wb.setTask(t);
//
//		channel.writeAndFlush(wb.build());

	}

	private Header.Builder createHeader(int nodeId, int destination) {
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(nodeId);
		header.setDestination(destination);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());
		return header;
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
