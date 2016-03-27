//package dbhandlers;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import static org.junit.Assert.assertEquals;
//
//public class LevelDBHandlerTest {
//
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	@Test
//	public void testPut() {
//		log("****PUT*****");
//		LevelDBHandler.instance ().put ("Name", "Harish");
//		System.out.println(LevelDBHandler.instance ().get ("Name"));
//	}
//
//	@Test
//	public void testStore() {
//		log("****Store****");
//		String key = LevelDBHandler.instance ().store ("Time Saved");
//		assertEquals("Time Saved", LevelDBHandler.instance ().get (key));
//		System.out.println(key);
//		System.out.println(LevelDBHandler.instance ().get (key));
//	}
//
//	@Test
//	public void testUpdate() {
//		log("****Update****");
//		LevelDBHandler.instance ().put ("Name", "CodePenMan");
//		System.out.println(LevelDBHandler.instance ().get ("Name"));
//	}
//
//	@Test
//	public void testRemove() {
//		log("****Remove****");
//		System.out.println(LevelDBHandler.instance ().get ("Name"));
//		LevelDBHandler.instance ().remove ("Name");
//		System.out.println(LevelDBHandler.instance ().get ("Name"));
//	}
//
//	@Test
//	public void testHasKey() {
//		log("****Has Key****");
//		System.out.println(LevelDBHandler.instance ().hasKey ("Name"));
//	}
//
//	@Test
//	public void testGetDatabaseVendor() {
//		System.out.println(LevelDBHandler.instance ().getDatabaseVendor ());
//	}
//
//	private void log(String description)  {
//		System.out.println(description);
//	}
//}
