package gash.router.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;

/**
 * @author saurabh
 *
 */
public class Router {
	private final Logger logger = LoggerFactory.getLogger("Router");

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
			logger.info(
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
				broadcast(msg);

				// If destination is not -1 that means this message is only for
				// broadcasting. So return null.
				if (msg.getHeader().getDestination() != -1) {
					logger.info("Destination is not -1...");
					return null;
				}
			} else {
				logger.info("MAX HOPS is Zero! Dropping message...");
				return null;
			}
		}
		logger.info("<<<<<<<<<<<<Returning Message>>>>>>>>>>>>>>>>>>>>>>>>" + msg);
		return msg;
	}

	private void broadcast(WorkMessage msg) {
		logger.info("Forwarding message...");
		WorkMessage.Builder wb = WorkMessage.newBuilder(msg);
		Header.Builder hb = Header.newBuilder(msg.getHeader());
		hb.setMaxHops(hb.getMaxHops() - 1);
		wb.setHeader(hb);
		state.getEmon().broadcastMessage(wb.build());
	}
}