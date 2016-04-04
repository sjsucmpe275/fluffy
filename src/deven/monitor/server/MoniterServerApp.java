package deven.monitor.server;

public class MoniterServerApp {
		public static void main(String[] args) {
			int port = 5000;

			try {
				MonitorServer svr = new MonitorServer(port);
				svr.startServer();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("server closing");
			}
	}

}
