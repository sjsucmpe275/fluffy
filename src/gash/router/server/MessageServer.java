package gash.router.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gash.router.server.messages.cmd_messages.handlers.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deven.monitor.client.WorkerThread;
import gash.router.container.MonitoringTask;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.messages.wrk_messages.handlers.TaskMessageHandler;
import gash.router.server.tasks.NoOpBalancer;
import gash.router.server.tasks.TaskList;
import gash.router.server.tasks.TaskWorker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MessageServer {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();

	// public static final String sPort = "port";
	// public static final String sPoolSize = "pool.size";

	protected static RoutingConf conf;
	protected boolean background = false;
	public static MonitoringTask monitor = new MonitoringTask();
	public static File confFile;
	
	private QueueManager queues = new QueueManager(100);
	
	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public MessageServer(File cfg) {
		confFile = cfg;
		init(cfg);
	}

	public void release() {
	}

	public void startServer() {
		StartWorkCommunication comm = new StartWorkCommunication(conf, queues);
		/*Adaptor Communication is for Inter Cluster Communication - it was not fully done*/
//		StartAdaptorCommunication adapComm = new StartAdaptorCommunication(conf);
		logger.info("Work starting");

		// We always start the worker in the background
		Thread cthread = new Thread(comm);
		cthread.start();
		
//		Thread cthreadAdaptor = new Thread(adapComm);
//		cthreadAdaptor.start();


		if (!conf.isInternalNode()) {
			StartCommandCommunication comm2 = new StartCommandCommunication(conf, queues);
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
			System.out.println (conf.getNodeId());
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
		QueueManager queues;
		CmdStorageMsgHandler cmdMessageHandler;
		
		public StartCommandCommunication(RoutingConf conf, QueueManager queues) {
			this.conf = conf;
			this.queues = queues;
			cmdMessageHandler = new CmdStorageMsgHandler (queues);//createCommandMsgHandlersChainAndGetStart();
			cmdMessageHandler.start();
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
				b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new CommandChannelInitializer (conf, compressComm, cmdMessageHandler));

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
		QueueManager queues;
		
		public StartWorkCommunication(RoutingConf conf, QueueManager queues) {
			
			if (conf == null)
				throw new RuntimeException("missing conf");
			
			this.queues = queues;
			
			//final Path path = FileSystems.getDefault().getPath();
			logger.info("in message server");
			state = new ServerState(conf);
			state.setQueues(queues);
			monitor.registerObserver(state);
			monitor.monitorFile(confFile.getPath());

			TaskList tasks = new TaskList(new NoOpBalancer());
			state.setTasks(tasks);

			EdgeMonitor emon = new EdgeMonitor(state, queues);
			monitor.registerObserver(emon);
			Thread t = new Thread(emon);
			t.start();

			TaskMessageHandler command2workerThread = new TaskMessageHandler(state);
			t = new Thread(command2workerThread);
			t.start();
			
			int workerCount = 4;
			ExecutorService executors = Executors.newFixedThreadPool(workerCount);
			for(int i = 0; i < workerCount; i++) {
				TaskWorker taskWorker = new TaskWorker(state);
				executors.execute(taskWorker);
			}
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
	/*private static class StartAdaptorCommunication implements Runnable {
		ServerState state;
		WorkerThread monitorThread;
		
		public StartAdaptorCommunication(RoutingConf conf) {
			if (conf == null)
				throw new RuntimeException("missing conf");
			
			//final Path path = FileSystems.getDefault().getPath();
			state = new ServerState(conf);
			
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
				bootstrap.put(state.getConf().getAdaptorPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);

				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				b.childHandler(new GlobalCommandChannelInitializer(conf, false));

				// Start the server.
				logger.info("Starting work server (" + state.getConf().getNodeId() + "), listening on port = "
						+ state.getConf().getAdaptorPort());
				ChannelFuture f = b.bind(state.getConf().getAdaptorPort()).syncUninterruptibly();

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
	}*/
	
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