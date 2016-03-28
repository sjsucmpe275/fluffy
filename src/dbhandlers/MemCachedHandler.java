//package dbhandlers;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
//import net.spy.memcached.AddrUtil;
//import net.spy.memcached.MemcachedClient;
//public class MemCachedHandler implements IDBHandler{
//	MemcachedClient memcachedClient = null;
//	public MemcachedClient connect(){
//		int port=8000;
//		try {
//			memcachedClient = new MemcachedClient(new InetSocketAddress("127.0.0.1", 8000));
//	        System.out.println("Connection to server sucessful.");
//		} catch (IOException e) {
//			System.out.println( e.getMessage() );
//		}
//		return memcachedClient;
//	}
//	
//	@Override
//	public String put(String key, Object value) {
//		if (key == null || value == null) {
//			return null;
//		}
//		memcachedClient.set(key, 0, value);
//		return key;
//	}
//
//	@Override
//	public String store(Object value) {
//		if (value == null) {
//			return null;
//		}
//
//		String uuid = UUID.randomUUID().toString();
//		memcachedClient.set(uuid, 0, value);
//		return uuid;
//	}
//
//	@Override
//	public Object get(String key) {
//		return memcachedClient.get(key);
//	}
//
//	@Override
//	public boolean update(String key, Object value) {
//		Future fo = memcachedClient.replace(key, 0, value);
//		Object status=false;
//		try {
//			status = fo.get();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}
//			return (boolean) status;
//	}
//
//	@Override
//	public Object remove(String key) {
//		Future fo = memcachedClient.delete(key);
//		Object status=false;
//		try {
//			 status= fo.get();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			e.printStackTrace();
//		}
//		return status;
//	}
//
//	@Override
//	public boolean hasKey(String key) {
//		if(key==null)
//			return false;
//		Object myObject=memcachedClient.get(key);
//		if (myObject==null)
//				return false;
//		else
//			return true;
//	}
//
//	@Override
//	public String getDatabaseVendor() {
//		return "Memcached";
//	}
//
//}
