package gash.router.server;

import gash.router.container.RoutingConf;
import gash.router.server.edges.AdaptorEdgeMonitor;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import gash.router.container.MonitoringTask;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deven.monitor.client.WorkerThread;
import election.NodeStateEnum;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;

import javax.management.monitor.Monitor;

public class MessageServer {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();

	// public static final String sPort = "port";
	// public static final String sPoolSize = "pool.size";

	protected RoutingConf conf;
	protected boolean background = false;
	public static MonitoringTask monitor = new MonitoringTask();
	public static File confFile;
	
	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public MessageServer(File cfg) {
		this.confFile = cfg;
		init(cfg);
	}

	public MessageServer(RoutingConf conf) {
		this.conf = conf;
		
	}

	public void release() {
	}

	public void startServer() {
		StartWorkCommunication comm = new StartWorkCommunication(conf);
		StartAdaptorCommunication adapComm = new StartAdaptorCommunication(conf);
		logger.info("Work starting");

		// We always start the worker in the background
		Thread cthread = new Thread(comm);
		cthread.start();
		
		Thread cthreadAdaptor = new Thread(adapComm);
		cthreadAdaptor.start();


		if (!conf.isInternalNode()) {
			StartCommandCommunication comm2 = new StartCommandCommunication(conf);
			logger.info("Command starting");

			if (background) {
				Thread cthread2 = new Thread(comm2);
				cthread2.start();
			} else
				comm2.run();
		}
	}

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		logger.info("Server shutdown");
		System.exit(0);
	}

	private void init(File cfg) {
		if (!cfg.exists())
			throw new RuntimeException(cfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), RoutingConf.class);
			System.out.println(conf.getNodeId());
			if (!verifyConf(conf))
				throw new RuntimeException("verification of configuration failed");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean verifyConf(RoutingConf conf) {
		return (conf != null);
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartCommandCommunication implements Runnable {
		RoutingConf conf;

		public StartCommandCommunication(RoutingConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getCommandPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new CommandChannelInitializer (conf, compressComm));

				// Start the server.
				logger.info("Starting command server (" + conf.getNodeId() + "), listening on port = "
						+ conf.getCommandPort());
				ChannelFuture f = b.bind(conf.getCommandPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	/**
	 * initialize netty communication
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartWorkCommunication implements Runnable {
		ServerState state;
		WorkerThread monitorThread;
		
		public StartWorkCommunication(RoutingConf conf) {
			if (conf == null)
				throw new RuntimeException("missing conf");
			
			//final Path path = FileSystems.getDefault().getPath();
			monitor.monitorFile(confFile.getPath());
			logger.info("in message server");
			state = new ServerState(conf);
			monitor.registerObserver(state);
			
			TaskList tasks = new TaskList(new NoOpBalancer());
			state.setTasks(tasks);

			EdgeMonitor emon = new EdgeMonitor(state);
			monitor.registerObserver(emon);
			Thread t = new Thread(emon);
			t.start();
			
			monitorThread = new WorkerThread("localhost", 5000, state);
			monitorThread.start();
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(state.getConf().getWorkPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				b.childHandler(new WorkChannelInitializer (state, false));

				// Start the server.
				logger.info("Starting work server (" + state.getConf().getNodeId() + "), listening on port = "
						+ state.getConf().getWorkPort());
				ChannelFuture f = b.bind(state.getConf().getWorkPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();

				// shutdown monitor
				EdgeMonitor emon = state.getEmon();
				if (emon != null)
					emon.shutdown();
				
				if (monitorThread != null && monitorThread.isAlive()) {
					monitorThread.shutdown();
				}
			}
		}
	}
	private static class StartAdaptorCommunication implements Runnable {
		ServerState state;
		WorkerThread monitorThread;
		
		public StartAdaptorCommunication(RoutingConf conf) {
			if (conf == null)
				throw new RuntimeException("missing conf");
			
			//final Path path = FileSystems.getDefault().getPath();
			
			TaskList tasks = new TaskList(new NoOpBalancer());
			state.setTasks(tasks);

			AdaptorEdgeMonitor emon = new AdaptorEdgeMonitor(state);
			
			Thread t = new Thread(emon);
			t.start();
			
			
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(state.getConf().getWorkPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				b.childHandler(new WorkChannelInitializer (state, false));

				// Start the server.
				logger.info("Starting work server (" + state.getConf().getNodeId() + "), listening on port = "
						+ state.getConf().getWorkPort());
				ChannelFuture f = b.bind(state.getConf().getWorkPort()).syncUninterruptibly();

				logger.info(f.channel().localAddress() + " -> open: " + f.channel().isOpen() + ", write: "
						+ f.channel().isWritable() + ", act: " + f.channel().isActive());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();

				// shutdown monitor
				EdgeMonitor emon = state.getEmon();
				if (emon != null)
					emon.shutdown();
				
				if (monitorThread != null && monitorThread.isAlive()) {
					monitorThread.shutdown();
				}
			}
		}
	}
	/**
	 * help with processing the configuration information
	 * 
	 * @author gash
	 *
	 */
	public static class JsonUtil {
		private static JsonUtil instance;

		public static void init(File cfg) {

		}

		public static JsonUtil getInstance() {
			if (instance == null)
				throw new RuntimeException("Server has not been initialized");

			return instance;
		}

		public static String encode(Object data) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.writeValueAsString(data);
			} catch (Exception ex) {
				return null;
			}
		}

		public static <T> T decode(String data, Class<T> theClass) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				return mapper.readValue(data.getBytes(), theClass);
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}
		}
	}
}