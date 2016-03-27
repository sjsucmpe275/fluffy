/**
 * 
 */
package client;

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
	private String file;
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
		mc.get("foo");
	}

	private void handleStore() {

	}

	private void handlePut(String key) {
		
		// for (ByteString data : input) {
		SerializationUtil util = new SerializationUtil();
		List<ByteString> dataList = util.readfile(file, 0, 1024*1024, -1);
			
		mc.store(ByteString.copyFrom("bar".getBytes()));
		// }
	}

	private void handleDelete() {

	}

	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		Client c = new Client();
		c.handleCommand(args);
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
			}

			String key = args[1];
			file = args[2];
			handleGet(key);
			break;
			
		case "GETS":
			
			if (args.length < 2) {
				System.out.println("Not enough params.\n->key");
			}
			key = args[1];
			fileOutput = false;
			handleGets(key);
			break;
			
		case "STORE":
			break;

		case "PUT":
			if (args.length < 3) {
				System.out.println("Not enough params.\n->Key\n->Output File Location");
			}

			key = args[1];
			file = args[2];
			handlePut(key);
			break;
			
		case "PUTS":
			break;

		case "DELETE":
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

	private void handleGets(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getListenerID() {
		return "client";
	}

	@Override
	public void onMessage(CommandMessage msg) {
		System.out.println(msg);


		if (msg.getResponse().getAction() == Action.GET) {
			responseSize = msg.getResponse().getSize();

			System.out.println("###" + responseSize);
			ByteString data = msg.getResponse().getData();
			String str = new String(data.toByteArray());
			System.out.println("Fetched data: " + str);
			responseList.add(msg.getResponse());
		}
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

			SerializationUtil util = new SerializationUtil();
			util.writeFile(file, list);
		}
	}
}
