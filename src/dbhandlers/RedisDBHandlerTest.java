package dbhandlers;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
	public void testGetDatabaseVendor() {
		assertEquals("RedisDB", dbHandler.getDatabaseVendor());
	}

	@Test
	public void testHasKey() {
		String key;
		try {
			key = dbHandler.store(serialize("foo"));
			assertTrue(dbHandler.hasKey(key));
			assertFalse(dbHandler.hasKey("foobar"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testGet() {
		String key="prasanna";
		Map<Integer, byte[]> mapGiven= new HashMap<Integer, byte[]>();
		String obtainedKey="";
		try {
			mapGiven.put(0, serialize("data1"));
			mapGiven.put(2, serialize("data2"));
			obtainedKey = dbHandler.put(key, 0, serialize("data1"));
			assertEquals(key, obtainedKey);
			obtainedKey = dbHandler.put(key, 2, serialize("data2"));
			assertEquals(key, obtainedKey);
			Map<Integer, byte[]> map= dbHandler.get(key);
			for(Integer i:map.keySet()){
				assertTrue(Arrays.equals(map.get(i), mapGiven.get(i)));
			}

			byte[] serializeValue=serialize("data3");
			key = dbHandler.store(serializeValue);
			Map<Integer, byte[]> mapObtained= dbHandler.get(key);
			byte[] obtainedVal= mapObtained.get(0);
			assertTrue(Arrays.equals(serializeValue, obtainedVal));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testPut() {
		String key="prasanna";
		Map<Integer, byte[]> mapGiven= new HashMap<Integer, byte[]>();
		String obtainedKey="";
		try {
			mapGiven.put(0, serialize("data1"));
			mapGiven.put(2, serialize("data2"));
			obtainedKey = dbHandler.put(key, 0, serialize("data1"));
			assertEquals(key, obtainedKey);
			obtainedKey="";
			obtainedKey = dbHandler.put(key, 2, serialize("data2"));
			assertEquals(key, obtainedKey);

			obtainedKey="";
			obtainedKey=dbHandler.put(null, 0, "bar".getBytes());
			assertNull(obtainedKey);

			obtainedKey="";
			obtainedKey=dbHandler.put(key, 0, null);
			assertNull(obtainedKey);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Test
	public void testRemove() {

		String key="prasanna";
		Map<Integer, byte[]> mapGiven= new HashMap<Integer, byte[]>();
		String obtainedKey="";
		try {
			mapGiven.put(0, serialize("data1"));
			obtainedKey = dbHandler.store("data1".getBytes());
			assertEquals(key, obtainedKey);
			Map<Integer, byte[]> map= new HashMap<Integer, byte[]>();
			map=dbHandler.remove(obtainedKey);
			for(Integer i: map.keySet()){
				assertTrue(Arrays.equals(mapGiven.get(i), map.get(i)));
			}

			assertFalse(dbHandler.hasKey(obtainedKey));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testStore() {
		byte[] serializedValue="foo".getBytes();
		String key = dbHandler.store(serializedValue);
		assertNotNull(key);

		key = dbHandler.store(null);
		assertNull(key);
	}

	@Test
	public void testUpdate() {
		byte[] serializedValue="foo".getBytes();
		String key = dbHandler.store(serializedValue);
		boolean b = dbHandler.update(key, 0, "foobar".getBytes());
		assertTrue(b);
	}

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
