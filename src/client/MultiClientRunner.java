/**
 * 
 */
package client;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author saurabh
 *
 */
public class MultiClientRunner {

	public static void main(String[] args) throws InterruptedException {
		String[] params = new String[3];
		params[0] = "get";
		params[1] = "abcdeasdasd";
		params[2] = "src/util/dump2.jpg";

		Client client = new Client();
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 100; i++) {
			threadPool.execute(new Runnable() {

				@Override
				public void run() {
					try {
						client.handleCommand(params);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
			});
		}
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(100, TimeUnit.SECONDS);
			client.releaseClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
