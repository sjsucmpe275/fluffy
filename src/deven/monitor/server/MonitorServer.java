package deven.monitor.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MonitorServer {
	protected static ServerBootstrap b;
	int port=5000;
	public MonitorServer(int port){
		this.port = port;
	}
	
	public void startServer() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			b = new ServerBootstrap();

			b.group(bossGroup, workerGroup);
			b.channel(NioServerSocketChannel.class);
			b.option(ChannelOption.SO_BACKLOG, 100);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

			boolean compressComm = false;
			b.childHandler(new MonitorInit(compressComm));

			// Start the server.
			System.out.println("Starting monitor server , listening on port = "
					+ this.port);
			ChannelFuture f = b.bind(this.port).syncUninterruptibly();

			System.out.println(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
					+ f.channel().isWritable() + ", act: " + f.channel().isActive());

			// block until the server socket is closed.
			f.channel().closeFuture().sync();

		} catch (Exception ex) {
			// on bind().sync()
			System.out.println("Failed to setup handler."+ ex.toString());
		} finally {
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
