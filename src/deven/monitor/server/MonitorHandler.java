package deven.monitor.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.monitor.Monitor.ClusterMonitor;

public class MonitorHandler  extends SimpleChannelInboundHandler<ClusterMonitor>{

	static private int count = 0;
	public MonitorHandler(){
		
	}
	
	
	/*
	 * Do not assume how message are processed.
	 * More error handling conditions will be there for unwanted packets
	 * Follow the directions mentioned in dummyMessage created by client to create message 
	 * and send it to Monitor Server or else your message might get discarded
	 */
	protected void handleMessage(ClusterMonitor msg, Channel ch){
		if (msg == null) {
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}
		System.out.println("Count : "+count);
		count++;
		System.out.println("Tick : "+msg.getTick()+" Cluster ID :"+msg.getClusterId()+"  #Nodes : "+msg.getNumNodes()+"\nProcess ID's");
		for(int i=0;i<msg.getProcessIdCount();i++){
			System.out.print(msg.getProcessId(i)+" ");
		}
		System.out.println("\nNo of task enqueued");
		for(int i=0;i<msg.getEnqueuedCount();i++){
			System.out.print(msg.getEnqueued(i)+" ");
		}
		System.out.println("\nNo of task Processed");
		for(int i=0;i<msg.getProcessedCount();i++){
			System.out.print(msg.getProcessed(i));
		}
		System.out.println("\nNo of task Stolen");
		for(int i=0;i<msg.getStolenCount();i++){
			System.out.print(msg.getStolen(i)+" ");
		}
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ClusterMonitor msg) throws Exception {
		handleMessage(msg, ctx.channel());
		
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Unexpected exception from downstream."+cause.toString());
		ctx.close();
	}

}
