/**
 * 
 */
package gash.router.server;

import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;

/**
 * @author saurabh
 *
 */
public class Router {

	private final ServerState state;

	public Router(ServerState state) {
		this.state = state;
	}

	/**
	 * Checks if message is coming back to this node.
	 * @param msg
	 * @return
	 */
	public WorkMessage route(WorkMessage msg) {
		
		// Message is for current node.
		if (msg.getHeader().getDestination() == state.getConf().getNodeId()) {
			return msg;
		}
		
		if (msg.getHeader().getNodeId() == state.getConf().getNodeId()) {
			System.out.println(
				"Same message received by source! Dropping message...");
			return null;
		}

		return route0(msg);
	}

	public WorkMessage publish(WorkMessage msg) {
		return route0(msg);
	}

	/**
	 * Just publishes messages without dropping boomrang messages.
	 * @param msg
	 * @return
	 */
	private WorkMessage route0(WorkMessage msg) {

		if (msg.getHeader().getDestination() != state.getConf().getNodeId()) {

			if (msg.getHeader().getMaxHops() > 0) {
				WorkMessage.Builder wb = WorkMessage.newBuilder(msg);
				Header.Builder hb = Header.newBuilder(msg.getHeader());
				hb.setDestination(state.getLeaderId());
				hb.setNodeId(state.getConf().getNodeId());
				wb.setHeader(hb);
				msg = wb.build();
				broadcast(msg);
				
				// If destination is not -1 that means this message is only for 
				// broadcasting. So return null.
				if (msg.getHeader().getDestination() != -1) {
					return null;
				}
				
			} else {
				System.out.println("MAX HOPS is Zero! Dropping message...");
				return null;
			}
			/*
			 * } else { if (msg.getHeader().getMaxHops() > 0) { broadcast(msg);
			 * return null; } else { System.out.println(
			 * "MAX HOPS is Zero! Dropping message..."); return null; } }
			 */
		}
		return msg;
	}

	private void broadcast(WorkMessage msg) {
		System.out.println("Forwarding message...");
		WorkMessage.Builder wb = WorkMessage.newBuilder(msg);
		Header.Builder hb = Header.newBuilder(msg.getHeader());
		hb.setMaxHops(hb.getMaxHops() - 1);
		wb.setHeader(hb);
		state.getEmon().broadcastMessage(wb.build());
	}

}
