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
		params[0] = "puts";
		params[1] = "abcde";
		params[2] = "Quick brown fox jump over the lazy dog.";

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
			threadPool.awaitTermination(10, TimeUnit.SECONDS);
			client.releaseClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
