package deven.monitor.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.monitor.Monitor.ClusterMonitor;

public class MonitorHandler extends SimpleChannelInboundHandler<ClusterMonitor> {

	protected ConcurrentMap<String, MonitorListener> listeners = new ConcurrentHashMap<String, MonitorListener>();
	public MonitorHandler() {
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, ClusterMonitor arg1) throws Exception {
		// TODO Auto-generated method stub
		
		System.out.println("Listener for monitor messages...");
		
	}
	
	public void addListener(MonitorListener listener) {
		if (listener == null)
			return;

		listeners.putIfAbsent(listener.getListenerID(), listener);
	}

}