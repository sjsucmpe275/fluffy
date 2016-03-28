/*
package dbhandlers;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class RedisDBHandlerTest {
	private RedisDBHandler dbHandler;
	
	@Before
	public void setUp() throws Exception {
		dbHandler = new RedisDBHandler();
	}

	@Test
	public void testGet() {
		String key="prasanna";

		try {
			key = dbHandler.put(key, 0, serialize("data1"));

			key = dbHandler.put(key, 2, serialize("data2"));
			Map<Integer, byte[]> mapGiven= new HashMap<Integer, byte[]>();
			mapGiven.put(0, serialize("data1"));
			mapGiven.put(2, serialize("data2"));
			Map<Integer, byte[]> map= dbHandler.get(key);

			assertEquals(map, mapGiven);

			byte[] serializeValue=serialize("data3");
			key = dbHandler.store(serializeValue);
			Map<Integer, byte[]> mapObtained= dbHandler.get(key);
			byte[] obtainedVal= mapObtained.get(0);
			
			assertEquals(serializeValue, obtainedVal);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testGetDatabaseVendor() {
		assertEquals("RedisDB", dbHandler.getDatabaseVendor());
	}

//	@Test
//	public void testHasKey() {
//		String key = dbHandler.store("foo");
//		assertTrue(dbHandler.hasKey(key));
//
//		assertFalse(dbHandler.hasKey("foobar"));
//	}
//
//	@Test
//	public void testPut() {
//		String key = dbHandler.put("foo", "bar");
//		assertEquals(key, "foo");
//
//		key = dbHandler.put(null, "bar");
//		assertNull(key);
//
//		key = dbHandler.put("foo", null);
//		assertNull(key);
//	}
//
//	@Test
//	public void testRemove() {
//		String key = dbHandler.store("foo");
//		assertEquals("foo", dbHandler.remove(key));
//	}
//
//	@Test
//	public void testStore() {
//		String key = dbHandler.store("foo");
//		assertNotNull(key);
//
//		key = dbHandler.store(null);
//		assertNull(key);
//	}
//
//	@Test
//	public void testUpdate() {
//		String key = dbHandler.store("bar");
//		boolean b = dbHandler.update(key, "foo");
//		assertTrue(b);
//	}
	
	public byte[] serialize(Object obj) throws IOException {
		try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
			try(ObjectOutputStream o = new ObjectOutputStream(b)){
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
			try(ObjectInputStream o = new ObjectInputStream(b)){
				return o.readObject();
			}
		}
	}

}
*/
