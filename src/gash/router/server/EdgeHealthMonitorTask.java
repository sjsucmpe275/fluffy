package gash.router.server;

import gash.router.server.edges.EdgeMonitor;

import java.util.TimerTask;

/*
*   @author: codepenman
* @date: 28/03/2016
* */

public class EdgeHealthMonitorTask extends TimerTask {

	private final boolean debug = false;
	private final EdgeMonitor edgeMonitor;

	public EdgeHealthMonitorTask(EdgeMonitor edgeMonitor) {
		this.edgeMonitor = edgeMonitor;
	}

	@Override
	public void run() {

		if (debug) {
			edgeMonitor.getLogger().info("I am awake");
			edgeMonitor.getLogger().info("Checking inbound edges health");
		}

		long currentTime = System.currentTimeMillis();

		/*
		 * If I don't receive Heart Beat Message from my neighbours, then I will
		 * assume neighbour instances are in failure state and remove the node
		 * from InBound edges list
		 */

		/* While checking the health of inbound edges I check if node is present in outbound edge because
		my assumption is if a heart beat is received from node which is present in outbound edge then I consider
		it as reply. This assumption will cause inbound heartbeat not updated in cyclic loop but channel is still active.
		This is the only use case, and remaining all other use cases are handled.
	    */
		edgeMonitor.getInboundEdges().getEdgesMap().values().stream()
				.filter(ei -> ((currentTime - ei.getLastHeartbeat()) > (edgeMonitor.getDelayTime()) &&
						!edgeMonitor.getOutboundEdges ().hasNode (ei.getRef ())))
				.forEach(ei -> {
					long hb = currentTime - ei.getLastHeartbeat();
					if (debug)
						edgeMonitor.getLogger().info("Last heat beat received before: " + hb);
					ei.setChannel (null);
					ei.setActive (false);
				});

		if (debug)
			edgeMonitor.getLogger().info("Checking outbound edges health");
		/*
		 * If I don't receive reply back from my neighbours for the heart beat
		 * message's I sent I assume my neighbour instances are down and I
		 * remove them. i Multiply dt with 2 because here I need to consider
		 * Round Trip Time. In Simple terms: I send Heart Beat to my neighbour -
		 * Neighbour send Heart Beat to me
		 */
		edgeMonitor.getOutboundEdges().getEdgesMap().values().stream()
				.filter(ei -> (currentTime - ei.getLastHeartbeat()) > (edgeMonitor.getDelayTime() * 2)).forEach(ei -> {
					long hb = currentTime - ei.getLastHeartbeat();
					if (debug)
						edgeMonitor.getLogger().info("Last heat beat reply received before: " + hb);
					ei.setChannel(null);
					ei.setActive(false);
				});
	}
}
