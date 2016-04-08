/**
 * 
 */
package client;

import com.google.protobuf.ByteString;
import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import routing.Pipe.CommandMessage;
import storage.Storage.Action;
import storage.Storage.Response;
import util.SerializationUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author saurabh
 *
 */
public class Client implements CommListener {

	private static final int M = 1024 * 1024;

	private String host = "localhost";
	private int port = 4568;
	private MessageClient mc;
	private String filepath;
	private List<Response> responseList;
	private int responseSize;
	private boolean fileOutput = false;

	public Client() throws InterruptedException {
		mc = new MessageClient(host, port);
		responseList = new LinkedList<>();
		mc.addListener(this);
	}

	@Override
	public String getListenerID() {
		return "client";
	}

	@Override
	public void onMessage(CommandMessage msg) {

		if (!fileOutput) {
			System.out.println(msg);
		}

		if (msg.getResponse().getAction() == Action.GET) {

			if (msg.getResponse().hasMetaData()) {
				responseSize = msg.getResponse().getMetaData().getSeqSize();
			} else {
				ByteString data = msg.getResponse().getData();
				String str = new String(data.toByteArray());
				
				if (!fileOutput) {
					System.out.println("Fetched data: " + str);
				}
				responseList.add(msg.getResponse());

				if (responseList.size() == responseSize) {
					Collections.sort(responseList, new Comparator<Response>() {

						@Override
						public int compare(Response o1, Response o2) {
							return o1.getSequenceNo() - o2.getSequenceNo();
						}
					});

					List<ByteString> list = new LinkedList<>();
					for (Response response : responseList) {
						list.add(response.getData());
					}

					if (fileOutput) {
						SerializationUtil util = new SerializationUtil();
						util.writeFile(filepath, list);
					}
				}
			}
		}
	}

	public void handleCommand(String[] args) throws FileNotFoundException {
		if (args.length < 1) {
			System.out.println("Operation not specified!");
			return;
		}

		System.out.println(Thread.currentThread() + ": Handling " + args[0] );
		switch (args[0].toUpperCase()) {
		case "GET":

			if (args.length < 3) {
				System.out.println(
					"Not enough params.\n->Key\n->Output File Location");
				return;
			}
			fileOutput = true;
			String key = args[1];
			filepath = args[2];
			mc.get(key);
			break;

		case "GETS":

			if (args.length < 2) {
				System.out.println("Not enough params.\n->key");
				return;
			}
			key = args[1];
			fileOutput = false;
			mc.get(key);
			break;

		case "STORE":
			if (args.length < 2) {
				System.out.println("Not enough params. \n->string value");
				return;
			}
			String value = args[1];
			mc.store(ByteString.copyFrom(value.getBytes()));
			break;

		case "PUT":
			if (args.length < 3) {
				System.out.println(
					"Not enough params.\n->Key\n->Input File Location");
				return;
			}
			key = args[1];
			filepath = args[2];
			SerializationUtil util = new SerializationUtil();
			File tempFile = new File(filepath);
			if (!tempFile.exists()) {
				throw new FileNotFoundException(filepath);
			}
			long fileSize = tempFile.length();
			mc.putMetadata(key, (int) (Math.ceil(1.0 * fileSize / M)),
				fileSize);
			for (int i = 0; i < 1 + (fileSize / M / 10); i++) {
				List<ByteString> dataList = util.readfile(filepath, 0, M, 10);

				int sequenceNo = 1;
				for (ByteString data : dataList) {
					mc.put(key, sequenceNo++, data);
				}
			}
			break;

		case "PUTS":
			if (args.length < 3) {
				System.out.println("Not enough params.\n->Key\n->String value");
				return;
			}
			key = args[1];
			value = args[2];
			mc.putMetadata(key, 1, value.getBytes().length);
			mc.put(key, 1, ByteString.copyFrom(value.getBytes()));
			break;

		case "DELETE":
			if (args.length < 2) {
				System.out.println("Not enough params.\n->Key");
				return;
			}
			key = args[1];
			System.out.println("Delete not implemented");
			break;

		default:
			System.out.println("Operation not supported. Use one of the :");
			System.out.println("GET");
			System.out.println("GETS");
			System.out.println("STORE");
			System.out.println("PUT");
			System.out.println("PUTS");
			System.out.println("DELETE");
			break;
		}

		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
	}
	
	public void releaseClient() {
		System.out.println("Client closing...");
		mc.release();
	}
	

	/**
	 * @param args
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args)
		throws InterruptedException, FileNotFoundException {
		Client c = new Client();
		c.handleCommand(args);
		c.releaseClient();
	}
}
