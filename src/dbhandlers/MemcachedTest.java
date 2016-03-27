//package dbhandlers;
//
//import static org.junit.Assert.*;
//
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
//
//import org.junit.Test;
//import net.spy.memcached.MemcachedClient;
//public class MemcachedTest {
//	MemCachedHandler connectMemcached=new MemCachedHandler();
//	MemcachedClient memcachedClient =connectMemcached.connect();
//	@Test
//	public void testConnection() {
//		assertNotNull(memcachedClient);
//	}
//	
//	@Test
//	public void testPut() {
//		connectMemcached.put("TestKey",10);
//		Object myObject=memcachedClient.get("someKey");
//		assertEquals(10, myObject);
//		
//	}
//	
//	@Test
//	public void testUpdate() throws InterruptedException, ExecutionException {
//		boolean status=connectMemcached.update("TestKey", 20);
//		Object myObject=memcachedClient.get("TestKey");
//		assertEquals(20, myObject);
//		
//	}
//	
//	@Test
//	public void testRemove() throws InterruptedException, ExecutionException {
//		connectMemcached.put("TestKey2",20);
//		Object status=connectMemcached.remove("TestKey2");
//		assertEquals(true,status);
//		
//	}
//	@Test
//	public void testHaskey()  {
//		boolean status=connectMemcached.hasKey("TestKey");
//		assertEquals(true,status);
//		
//	}
//	@Test
//	public void testIfHaskeyNull()  {
//		boolean status=connectMemcached.hasKey(null);
//		assertEquals(false,status);
//		
//	}
//	@Test
//	public void testGetDatabaseVendor() {
//		assertEquals("Memcached", connectMemcached.getDatabaseVendor());
//	}
//	@Test
//	public void testStore() {
//		String key=connectMemcached.store("TestValue");
//		assertNotNull(key);
//	}
//	@Test
//	public void testStoreForNull() {
//		String key=connectMemcached.store(null);
//		assertNull(key);
//	}
//
//
//}
