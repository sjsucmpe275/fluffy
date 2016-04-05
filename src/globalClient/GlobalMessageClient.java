package globalClient;

import com.google.protobuf.ByteString;

import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;
import storage.Storage.Action;
import storage.Storage.Metadata;
import storage.Storage.Query;

public class GlobalMessageClient {
	private long curID = 0;

	public GlobalMessageClient(String host, int port) {
		init(host, port);
	}

	private void init(String host, int port) {
		CommConnection.initConnection(host, port);
	}

	public void addListener(CommListener listener) {
		CommConnection.getInstance().addListener(listener);
	}

	public void ping() {
		// construct the message to send
		Header.Builder hb = buildHeader();
		
		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setPing(true);
		
		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(rb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void store(ByteString data) {
		Header.Builder hb = buildHeader();
		

		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);

		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setData(data);
		qb.setSequenceNo(1);
	
		cb.setQuery(qb);

		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void get(String key) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.GET);
		qb.setKey(key);
	
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void put(String key, int sequenceNo, ByteString data) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setKey(key);
		qb.setSequenceNo(sequenceNo);
		qb.setData(data);
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void putMetadata(String key, int seqSize, long fileLength) {
		Header.Builder hb = buildHeader();
	
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		cb.setHeader(hb);
	
		Query.Builder qb = Query.newBuilder();
		qb.setAction(Action.STORE);
		qb.setKey(key);
		qb.setSequenceNo(0);
		
		Metadata.Builder mb = Metadata.newBuilder();
		mb.setSeqSize(seqSize);
		mb.setSize(fileLength);
		mb.setTime(System.currentTimeMillis());
		
		qb.setMetadata(mb);
		cb.setQuery(qb);
	
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	private Header.Builder buildHeader() {
		Header.Builder hb = Header.newBuilder();
		hb.setDestination(-1);
		hb.setMaxHops(5);
		hb.setTime(System.currentTimeMillis());
		hb.setNodeId(-1);
		return hb;
	}

	public void release() {
		CommConnection.getInstance().release();
	}

	/**
	 * Since the service/server is asychronous we need a unique ID to associate
	 * our requests with the server's reply
	 * 
	 * @return
	 */
	private synchronized long nextId() {
		return ++curID;
	}
}
