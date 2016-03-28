/*
package dbhandlers;

import static org.junit.Assert.*;

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
		String key = dbHandler.put("foo", "bar");
		Object obj = dbHandler.get(key);
		if (obj instanceof String) {
			assertEquals((String) obj, "bar");
		} else {
			fail("Saved object is not String!");
		}

		key = dbHandler.store("bar");
		obj = dbHandler.get(key);
		if (obj instanceof String) {
			assertEquals((String) obj, "bar");
		} else {
			fail("Saved object is not String!");
		}
	}

	@Test
	public void testGetDatabaseVendor() {
		assertEquals("RedisDB", dbHandler.getDatabaseVendor());
	}

	@Test
	public void testHasKey() {
		String key = dbHandler.store("foo");
		assertTrue(dbHandler.hasKey(key));

		assertFalse(dbHandler.hasKey("foobar"));
	}

	@Test
	public void testPut() {
		String key = dbHandler.put("foo", "bar");
		assertEquals(key, "foo");

		key = dbHandler.put(null, "bar");
		assertNull(key);

		key = dbHandler.put("foo", null);
		assertNull(key);
	}

	@Test
	public void testRemove() {
		String key = dbHandler.store("foo");
		assertEquals("foo", dbHandler.remove(key));
	}

	@Test
	public void testStore() {
		String key = dbHandler.store("foo");
		assertNotNull(key);

		key = dbHandler.store(null);
		assertNull(key);
	}

	@Test
	public void testUpdate() {
		String key = dbHandler.store("bar");
		boolean b = dbHandler.update(key, "foo");
		assertTrue(b);
	}

}
*/
