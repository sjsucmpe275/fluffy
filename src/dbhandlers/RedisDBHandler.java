package dbhandlers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import redis.clients.jedis.Jedis;

public class RedisDBHandler implements IDBHandler {
	Jedis redis;

	public RedisDBHandler() {
		redis=new Jedis("localhost");
	}

	@Override
	public String getDatabaseVendor() {
		return "RedisDB";
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
	public String put(String key, int sequenceId, byte[] value) {
		// TODO Auto-generated method stub
		if(key==null || value==null){
			return null;
		}
		try {
			redis.hset(serialize(key), serialize(sequenceId), serialize(value));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}

	@Override
	public Map<Integer, byte[]> get(String key) {
		Map<Integer, byte[]> sequenceIdValue=new HashMap<Integer,byte[]>();
		try {
			if(hasKey(key)){
				byte[] serializedKey=serialize(key);
				Set<byte[]> byteSequencIds= redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					sequenceIdValue.put((Integer) deserialize(bs), redis.hget(serializedKey, bs));
				}	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return sequenceIdValue;
	}

	@Override
	public List<Integer> getSequenceIds(String key) {
		List<Integer> listSequenceIds=new ArrayList<Integer>();
		try {
			if(hasKey(key)){
				byte[] serializedKey=serialize(key);
				Set<byte[]> byteSequencIds= redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					listSequenceIds.add((Integer)deserialize(bs));
				}	
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return listSequenceIds;
	}

	@Override
	public boolean update(String key, int sequenceId, byte[] value) {
		boolean isUpdated=false;
		try {
			if(hasKey(key)){
				byte[] serializedKey=serialize(key);
				byte[] serializedSequenceId=serialize(sequenceId);
				if(redis.hexists(serializedKey, serializedSequenceId)){
					redis.hset(serializedKey, serializedSequenceId, value);
					isUpdated=true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isUpdated;
	}

	@Override
	public String store(byte[] value) {
		if(value==null){
			return null;
		}
		String uuid=UUID.randomUUID().toString();
		String key= put(uuid, 0, value);
		return key;
	}


	@Override
	public Map<Integer, byte[]> remove(String key) {
		// TODO Auto-generated method stub
		Map<Integer,byte[]> removedMap=new HashMap<Integer, byte[]>();
		try{
			if(hasKey(key)){
				byte[] serializedKey=serialize(key);
				Set<byte[]> byteSequencIds= redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					removedMap.put((Integer) deserialize(bs), redis.hget(serializedKey, bs) );
					redis.hdel(serializedKey, bs);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return removedMap;
	}
}
