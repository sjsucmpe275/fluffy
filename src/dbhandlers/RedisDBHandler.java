/*
package dbhandlers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import redis.clients.jedis.Jedis;

public class RedisDBHandler implements IDBHandler {
	Jedis redis;

	public RedisDBHandler() {
		redis=new Jedis("localhost");
	}


	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
			try(ObjectInputStream o = new ObjectInputStream(b)){
				return o.readObject();
			}
		}
	}

	@Override
	public Object get(String key) {
		byte[] byteArray=null;
		Object returnObj=null;
		try {
			if(redis.exists(serialize(key))){
				byteArray = redis.get(serialize(key));
				returnObj = deserialize(byteArray);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return returnObj;
	}

	@Override
	public String getDatabaseVendor() {
		return "RedisDB";
	}

	@Override
	public boolean hasKey(String key) {
		boolean returnVal=false;
		try {
			if(redis.exists(serialize(key))){
				returnVal=true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnVal;
	}

	@Override
	public String put(String key, Object value) {
		// TODO Auto-generated method stub
		if (key == null || value == null) {
			return null;
		}
		try {
			redis.set(serialize(key), serialize(value));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}

	@Override
	public Object remove(String key) {
		Object returnObj=null;
		byte[] b=null;
		try {
			if(redis.exists(serialize(key))){
				b = redis.get(serialize(key));
				redis.del(serialize(key));
				returnObj=deserialize(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return returnObj;
	}

	public byte[] serialize(Object obj) throws IOException {
		try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
			try(ObjectOutputStream o = new ObjectOutputStream(b)){
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	@Override
	public String store(Object value) {
		if (value == null) {
			return null;
		}

		String uuid = UUID.randomUUID().toString();
		try {
			redis.set(serialize(uuid), serialize(value));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return uuid;

	}

	@Override
	public boolean update(String key, Object value) {
		boolean returnVal=false;
		try {
			if (redis.exists(serialize(key))){
				redis.set(serialize(key),serialize(value));
				returnVal=true;
			}else{
				returnVal=false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnVal;
	}


}
*/
