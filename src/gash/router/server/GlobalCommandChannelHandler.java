/*package gash.router.server;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.client.CommInit;
import gash.router.container.RoutingConf;
import gash.router.server.messages.cmd_messages.handlers.ICmdMessageHandler;
import global.Global.GlobalCommandMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Header;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;


public class GlobalCommandChannelHandler extends SimpleChannelInboundHandler<GlobalCommandMessage>  {
	protected static Logger logger = LoggerFactory.getLogger("Global cmd");
	private RoutingConf conf;
	private ICmdMessageHandler cmdMessageHandler;
	private Worker worker;
	private EventLoopGroup group;
	private ChannelFuture channel;
	public GlobalCommandChannelHandler(RoutingConf conf) throws Exception {
		if (conf != null) {
			this.conf = conf;
		}
		init();
	}
	
	private void init() {
		group = new NioEventLoopGroup();
		try {
			CommInit si = new CommInit(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			
			// Make the connection attempt.
			
			channel = b.connect("localhost", conf.getWorkPort()).syncUninterruptibly();
			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());

		} catch (Throwable ex) {
			
			ex.printStackTrace();
		}
		
	}
	public void handleMessage(GlobalCommandMessage msg, Channel channel) {
		CommandMessage.Builder cmdMsg = CommandMessage.newBuilder();
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}
		if(msg.hasHeader()){
			cmdMsg.setHeader(msg.getHeader());
			logger.info("Received header");
		}
		if(msg.hasErr()){
			cmdMsg.setErr(msg.getErr());
			logger.info("Received global error");
		}
		if(msg.hasMessage()){
			cmdMsg.setMessage(msg.getMessage());
			logger.info("Received global message");
		}
		if(msg.hasPing()){
			cmdMsg.setPing(msg.getPing());
			logger.info("Received global ping");
		}
		if(msg.hasQuery()){
			cmdMsg.setQuery(msg.getQuery());
			logger.info("Received global query");
		}
		if(msg.hasResponse()){
			cmdMsg.setResponse(msg.getResponse());
			logger.info("Received global Response");
		}
		
		WorkMessage.Builder wrkMsg = WorkMessage.newBuilder();
		Task.Builder task = Task.newBuilder();
		Header.Builder header = Header.newBuilder();
		header.setDestination(-1);
		header.setNodeId(-1);
		header.setTime(System.currentTimeMillis());
		
		task.setTaskMessage(cmdMsg);
		task.setSeqId(cmdMsg.getQuery().getSequenceNo());
		task.setSeriesId(cmdMsg.getQuery().getKey().hashCode());
		wrkMsg.setTask(task);
		wrkMsg.setSecret(1);
		wrkMsg.setHeader(header);
		
		write(wrkMsg.build());
	}
	
	public boolean write(WorkMessage msg) {
		if (msg == null)
			return false;
		else if (channel == null)
			throw new RuntimeException("missing channel");

		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = connect().writeAndFlush(msg);
		if (cf.isDone() && !cf.isSuccess()) {
			logger.error("failed to send message to server");
			return false;
		}

		return true;

	}
	public Channel connect() {
		if (channel == null) {
			init();
		}

		if (channel != null && channel.isSuccess() && channel.channel().isWritable())
			return channel.channel();
		else
			throw new RuntimeException("Not able to establish connection ");
	}

	@Override
	protected void channelRead0(ChannelHandlerContext gctx, GlobalCommandMessage gmsg) throws Exception {
		handleMessage(gmsg,gctx.channel());
		
	}

}
*/