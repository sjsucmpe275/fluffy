package globalClient;

import gash.router.client.CommConnection;

public class GlobalClientApp {
	private GlobalMessageClient gc;
	public GlobalClientApp(GlobalMessageClient g){
		this.gc=g;
		
	}
	
	private void ping(int N) {
		// test round-trip overhead (note overhead for initial connection)
		final int maxN = 10;
		long[] dt = new long[N];
		long st = System.currentTimeMillis(), ft = 0;
		for (int n = 0; n < N; n++) {
			gc.ping();
			ft = System.currentTimeMillis();
			dt[n] = ft - st;
			st = ft;
		}

		System.out.println("Round-trip ping times (msec)");
		for (int n = 0; n < N; n++)
			System.out.print(dt[n] + " ");
		System.out.println("");
	}
	
	public static void main(String[] args) {
		String host = "127.0.0.1";
		int port = 4297;

		try {
			GlobalMessageClient mc = new GlobalMessageClient(host, port);
			GlobalClientApp gapp = new GlobalClientApp(mc);

	
			gapp.ping(2);
			
			System.out.println("\n** exiting in 10 seconds. **");
			System.out.flush();
		   Thread.sleep(10 * 1000);
		} catch (Exception e) {
			System.out.println("In exception before relase");
			//e.printStackTrace();
		} finally {
			System.out.println("In finally before relase");
			CommConnection.getInstance().release();
			System.out.println("In finally after relase");
		}

	}

}
