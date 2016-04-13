/**
 * 
 */
package gash.router.server;

import gash.router.container.RoutingConf;
import pipe.common.Common.Header;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

/**
 * @author saurabh
 *
 */
public class MessageAdapter {

	public static CommandMessage getCommandMessage(WorkMessage msg) throws Exception {

		if (!msg.hasTask() || !msg.getTask().hasTaskMessage()) {
			throw new Exception("Work message does not have task");
		}

		return msg.getTask().getTaskMessage();
	}

	public static WorkMessage getWorkMessageToLeader(ServerState state,
		CommandMessage msg) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(getGenericHeader(state.getConf(), state.getLeaderId()));

		Task.Builder tb = Task.newBuilder();
		if (msg.hasQuery()) {
			tb.setSeqId(msg.getQuery().getSequenceNo());
			tb.setSeriesId(msg.getQuery().getKey().hashCode());
		} else if (msg.hasResponse()) {
			tb.setSeqId(msg.getResponse().getSequenceNo());
			tb.setSeriesId(msg.getResponse().getKey().hashCode());
		}
		tb.setTaskMessage(msg);

		wb.setTask(tb.build());
		wb.setSecret(state.getConf().getSecret());
		return wb.build();
	}

	public static Header getGenericHeader(RoutingConf conf, int destination) {
		Header.Builder hb = Header.newBuilder();
		hb.setDestination(destination);
		hb.setMaxHops(conf.getMaxHops());
		hb.setNodeId(conf.getNodeId());
		hb.setTime(System.currentTimeMillis());
		return hb.build();
	}

}
