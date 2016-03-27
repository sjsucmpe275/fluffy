//package dbhandlers;
//
//import org.iq80.leveldb.DB;
//import org.iq80.leveldb.Options;
//
//import java.io.*;
//import java.sql.Timestamp;
//
//import static org.fusesource.leveldbjni.JniDBFactory.bytes;
//import static org.fusesource.leveldbjni.JniDBFactory.factory;
//
///**
// * Created by codepenman on 3/6/16.
// */
//public class LevelDBHandler implements IDBHandler {
//
//	private LevelDBHandler(){}
//
//	private static LevelDBHandler levelDBHandler = null;
//	private DB db = null;
//
//	public static LevelDBHandler instance() {
//		if(levelDBHandler == null)  {
//			levelDBHandler = new LevelDBHandler ();
//		}
//		return levelDBHandler;
//	}
//
//	private void initializeConnection() {
//		Options options = new Options ();
//		options.createIfMissing (true);
//
//		try {
//			db = factory.open (new File ("leveldb"), options);
//		} catch (IOException ioException) {
//			ioException.printStackTrace ();
//			if(db != null)  {
//				try {
//					db.close();
//				} catch (IOException e) {
//					e.printStackTrace ();
//				}
//			}
//		}
//	}
//
//	@Override
//	public String put(String key, Object value) {
//		initializeConnection ();
//		try{
//			db.put (bytes(key), getBytes (value));
//		} catch (IOException ioException) {
//			ioException.printStackTrace ();
//		} finally {
//			closeConnection();
//		}
//		return key;
//	}
//
//	private byte[] getBytes(Object value) throws IOException {
//		try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()){
//			try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)){
//				objectOutputStream.writeObject(value);
//			}
//			return byteArrayOutputStream.toByteArray ();
//		}
//	}
//
//	private Object getObject(byte[] value) throws IOException, ClassNotFoundException {
//		try(ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(value)){
//			try(ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayOutputStream)){
//				return objectInputStream.readObject ();
//			}
//		}
//	}
//
//	private void closeConnection() {
//		try {
//			db.close ();
//		} catch (IOException ioException) {
//			ioException.printStackTrace ();
//		}
//	}
//
//	@Override
//	public String store(Object value) {
//		String key = generateKey();
//		put (key, value);
//		return key;
//	}
//
//	private String generateKey() {
//		Timestamp timestamp = new Timestamp (System.currentTimeMillis ());
//		return timestamp.toString ();
//	}
//
//	@Override
//	public Object get(String key) {
//		initializeConnection ();
//		Object value = null;
//		try {
//			byte[] bytes = db.get (bytes (key));
//			if(bytes != null)   {
//				value = getObject (bytes);
//			}
//		} catch (IOException | ClassNotFoundException exception) {
//			exception.printStackTrace ();
//		}finally {
//			closeConnection ();
//		}
//		return value;
//	}
//
//	@Override
//	public boolean update(String key, Object value) {
//		initializeConnection ();
//		try {
//			db.put (bytes (key), getBytes (value));
//		} catch (IOException ioException) {
//			ioException.printStackTrace ();
//		}finally {
//			closeConnection ();
//		}
//		return false;
//	}
//
//	@Override
//	public Object remove(String key) {
//		initializeConnection ();
//		Object value = db.get (bytes(key));
//		db.delete (bytes(key));
//		closeConnection ();
//		return value;
//	}
//
//	@Override
//	public boolean hasKey(String key) {
//		initializeConnection ();
//		Object value = db.get (bytes(key));
//		closeConnection ();
//		return value != null;
//	}
//
//	@Override
//	public String getDatabaseVendor() {
//		return "LevelDB";
//	}
//}
