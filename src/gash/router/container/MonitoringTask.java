package gash.router.container;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.MessageServer.JsonUtil;

public class MonitoringTask implements Subject {

	private ArrayList<Observer> observers = new ArrayList<Observer>();
	
	private static Logger logger = LoggerFactory.getLogger("task");

	

	@Override
	public void registerObserver(Observer observer) {
		logger.info("Added Observers ");
		observers.add(observer);

	}

	@Override
	public void removeObserver(Observer observer) {
		observers.remove(observer);

	}

	@Override
	public void notifyObservers(File file) {
		RoutingConf conf =init(file);
		logger.info("Notifying Observers ");
		
		for (Observer ob : observers) {
			ob.onFileChanged(conf);
		}

	}
	private RoutingConf init(File cfg) {
		RoutingConf conf = null ;
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
		return conf;
	}

	private boolean verifyConf(RoutingConf conf) {
		return (conf != null);
	}

	public void monitorFile(String dirPath) {
		logger.info("started monitoring ");
		FileWatcher f=new FileWatcher(new File(dirPath));
		f.start();
	}

	private class FileWatcher extends Thread {
		
		private AtomicBoolean stop = new AtomicBoolean(false);
		public File file;
		public FileWatcher(File file) {
			this.file = file;
		}

		@Override
		public void run() {
			try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
				Path path = file.toPath().getParent();
				path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
				while (!isStopped()) {
					WatchKey key;
					try {
						key = watcher.poll(200, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						return;
					}
					if (key == null) {
						Thread.yield();
						continue;
					}

					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();

						@SuppressWarnings("unchecked")
						WatchEvent<Path> ev = (WatchEvent<Path>) event;
						Path filename = ev.context();

						if (kind == StandardWatchEventKinds.OVERFLOW) {
							Thread.yield();
							continue;
						} else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
							logger.info("in event ");
							notifyObservers(file);
						}
						boolean valid = key.reset();
						if (!valid) {
							break;
						}
					}
					Thread.yield();
				}
			} catch (Throwable e) {
				e.printStackTrace ();
			}
		}

		public boolean isStopped() {
			return stop.get();
		}

		public void stopThread() {
			stop.set(true);
		}

	}
}