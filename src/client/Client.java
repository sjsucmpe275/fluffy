/**
 * 
 */
package client;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.ByteString;

import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import routing.Pipe.CommandMessage;
import storage.Storage.Action;
import storage.Storage.Response;
import util.SerializationUtil;

/**
 * @author saurabh
 *
 */
public class Client implements CommListener {

	private String host = "127.0.0.1";
	private int port = 4568;
	private MessageClient mc;
	private String filepath;
	private List<Response> responseList;
	private int responseSize;
	private boolean fileOutput = false;

	public Client() throws InterruptedException {
		mc = new MessageClient(host, port);
		init();
	}

	private void init() {
		responseList = new LinkedList<>();
		mc.addListener(this);
	}

	private void handleGet(String key) {
		mc.get(key);
	}

	private void handleStore(String value) {
		mc.store(ByteString.copyFrom(value.getBytes()));
	}

	private void handlePut(String key) {

		SerializationUtil util = new SerializationUtil();
		List<ByteString> dataList = util.readfile(filepath, 0, 1024 * 1024, -1);
		File tempFile = new File(filepath);

		mc.putMetadata(key, dataList.size(), filepath.length());
		int sequenceNo = 1;
		for (ByteString data : dataList) {
			mc.put(key, sequenceNo++, data);
		}

	}

	private void handleDelete() {

	}

	private void handleGets(String key) {
		mc.get(key);
	}

	
	private void handleDelete(String key) {
		// Not implemented
		throw new RuntimeException("Not implemented!");
	}

	private void handlePuts(String key, String value) {
		mc.putMetadata(key, 1, value.getBytes().length);
		mc.put(key, 1, ByteString.copyFrom(value.getBytes()));
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

	public void handleCommand(String[] args) {

		if (args.length < 1) {
			System.out.println("Operation not specified!");
			return;
		}

		switch (args[0]) {
		case "GET":

			if (args.length < 3) {
				System.out.println("Not enough params.\n->Key\n->Output File Location");
				return;
			}
			fileOutput = true;
			String key = args[1];
			filepath = args[2];
			handleGet(key);
			break;

		case "GETS":

			if (args.length < 2) {
				System.out.println("Not enough params.\n->key");
				return;
			}
			key = args[1];
			fileOutput = false;
			handleGets(key);
			break;

		case "STORE":
			if (args.length < 2) {
				System.out.println("Not enough params. \n->string value");
				return;
			}
			String value = args[1];
			handleStore(value);
			break;

		case "PUT":
			if (args.length < 3) {
				System.out.println("Not enough params.\n->Key\n->Input File Location");
				return;
			}
			key = args[1];
			filepath = args[2];
			handlePut(key);
			break;

		case "PUTS":
			if (args.length < 3) {
				System.out.println("Not enough params.\n->Key\n->String value");
				return;
			}
			key = args[1];
			value = args[2];
			handlePuts(key, value);
			break;

		case "DELETE":
			if (args.length < 2) {
				System.out.println("Not enough params.\n->Key");
				return;
			}
			key = args[1];
			handleDelete(key);
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

		System.out.println("Client closing...");
		mc.release();
	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		Client c = new Client();
		c.handleCommand(args);
	}
}
