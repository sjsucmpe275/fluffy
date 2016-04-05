package gash.router.server.edges;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;

public class AdaptorEdgeMonitor extends EdgeMonitor  {
	private static Logger logger = LoggerFactory.getLogger("Adaptor edge monitor");
	private static final boolean debug = false;
	
	
	
	public AdaptorEdgeMonitor(ServerState state) {
		super(state);

		
	}

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}
	




}
