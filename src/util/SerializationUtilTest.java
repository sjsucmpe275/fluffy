package util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.google.protobuf.ByteString;

@FixMethodOrder
public class SerializationUtilTest {

	private boolean keepFile = false;
	private List<ByteString> list;
	private SerializationUtil instance;

	@Before
	public void setup() {
		instance = new SerializationUtil();
		list = new LinkedList<>();
	}

	@Test
	public void testReadfile() {
		list = instance.readfile("src/util/dump.jpg", 0, 1024, -1);

		assertEquals(list.size(), 7601);
		assertEquals(list.get(0).size(), 1024);

	}

	@Test
	public void testWriteFile() {
		list = instance.readfile("src/util/dump.jpg", 0, 1024 * 1024, 1);
		list.addAll(instance.readfile("src/util/dump.jpg", 1, 1024 * 1024, 1));
		list.addAll(instance.readfile("src/util/dump.jpg", 2, 1024 * 1024, -1));

		System.out.println(list.size());
		for (ByteString data : list) {
			// System.out.println(data.size());
		}
		instance.writeFile("src/util/dump2.jpg", list);
		File file = new File("src/util/dump2.jpg");
		
		assertTrue(file.exists());
		assertTrue(file.isFile());
		assertEquals(8388608, file.length());
	}

	@After
	public void tearDown() {
		if (!keepFile) {
			File file = new File("src/util/dump2.jpg");
			file.delete();
		}
	}
}
