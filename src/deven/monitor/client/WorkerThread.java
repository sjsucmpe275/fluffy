package deven.monitor.client;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.monitor.Monitor.ClusterMonitor;

public class WorkerThread extends Thread {
	private MonitorClient mcc;
	private ServerState state;
	private long tick;
	private String monitorHost;
	private int port;
	private boolean forever = true;

	public WorkerThread(String monitorHost, int monitorPort, ServerState state) {
		tick = 0;
		this.monitorHost = monitorHost;
		this.port = monitorPort;
		this.state = state;
	}

	@Override
	public void run() {
		while (forever) {
			this.mcc = new MonitorClient(monitorHost, port);
			Channel ch = null;
			try {
				ch = mcc.connect();
			} catch (Exception e) {
				System.out.println("Client is not available! Try again later...");
			}
			if (ch == null || !ch.isOpen() || !ch.isActive()) {
				System.out.println("connection missing, no outbound communication");
			}
			ClusterMonitor.Builder cm = ClusterMonitor.newBuilder();
			cm.setClusterId(1); //TODO add secret in conf file and update this code to state.getConf().getSecret()
			cm.setNumNodes(4); // TODO take this value from the leader
			cm.addProcessId(state.getConf().getNodeId());
			cm.addEnqueued(state.getTasks().numEnqueued());
			cm.addProcessed(state.getTasks().numProcessed());
			cm.addStolen(state.getTasks().numBalanced());
			cm.setTick(tick++);
			cm.build();

			if (ch != null && ch.isActive() && ch.isWritable()) {
				mcc.write(cm.build());
			} else {
				System.out.println("Channel not writable");
			}
			try {
				sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void shutdown() {
		forever = false;
	}

}