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
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisDBHandler implements IDBHandler {
	Jedis redis;
	static JedisPool jedisPool;

	/**
	 * Method to create a JedisPool and fetch a jedis connection from it.
	 * 
	 * @return Jedis connection instance
	 * @throws JedisConnectionException
	 */
	public synchronized Jedis getJedisConnection() throws JedisConnectionException {
		if (jedisPool == null) {
			JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
			jedisPoolConfig.setMaxTotal(10);
			jedisPoolConfig.setMaxIdle(5);
			jedisPoolConfig.setMinIdle(1);
			jedisPool = new JedisPool(jedisPoolConfig, "localhost");
		}
		return jedisPool.getResource();
	}

	/**
	 * Constructor to initialize jedis instance
	 */
	public RedisDBHandler() {
		redis = getJedisConnection();
	}

	/**
	 * Method to get the name of database vendor
	 * 
	 * @return Database vendor name
	 */
	@Override
	public String getDatabaseVendor() {
		return "RedisDB";
	}

	/**
	 * Method to serialize the given object
	 * 
	 * @param obj
	 *            - Object to be serialized
	 * @return serialized byte array object
	 * @throws IOException
	 */
	public byte[] serialize(Object obj) throws IOException {
		try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
			try (ObjectOutputStream o = new ObjectOutputStream(b)) {
				o.writeObject(obj);
			}
			return b.toByteArray();
		}
	}

	/**
	 * Method to deserialize the given byte array to object
	 * 
	 * @param bytes
	 *            - bytes to be deserialized
	 * @return deserialized object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
			try (ObjectInputStream o = new ObjectInputStream(b)) {
				return o.readObject();
			}
		}
	}

	/**
	 * Method to check if the key exists on the redis server
	 * 
	 * @param key
	 *            - key to be checked for existence
	 * @return boolean value true if the key exists, otherwise false
	 */
	@Override
	public boolean hasKey(String key) {
		boolean returnVal = false;
		try {
			if (redis.exists(serialize(key))) {
				returnVal = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnVal;
	}

	/**
	 * Method to put given key,field and value in the hash of the redis server
	 * 
	 * @param key
	 *            - Key as a string
	 * @param sequenceId
	 *            - Sequence Id for the data chunk
	 * @param value
	 *            - Data to store
	 * @return Key at which object is stored
	 */
	@Override
	public String put(String key, int sequenceId, byte[] value) {
		// TODO Auto-generated method stub
		if (key == null || value == null) {
			return null;
		}
		try {
			redis.hset(serialize(key), serialize(sequenceId), value);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return key;
	}

	/**
	 * Method to retrieve data stored at the key.
	 * 
	 * @param key
	 *            - Key as a string
	 * @return map of chunkId and Data as byte array.
	 */
	@Override
	public Map<Integer, byte[]> get(String key) {
		Map<Integer, byte[]> sequenceIdValue = new HashMap<Integer, byte[]>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
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

	/**
	 * Method to get all the sequence ids corresponding to the given key
	 * 
	 * @param key
	 *            - key for which sequence ids are to be fetched
	 * @return list of sequence ids
	 */
	@Override
	public List<Integer> getSequenceIds(String key) {
		List<Integer> listSequenceIds = new ArrayList<Integer>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					listSequenceIds.add((Integer) deserialize(bs));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return listSequenceIds;
	}

	/**
	 * Update chunk of data stored at sequenceId for the corresponding key
	 * 
	 * @param key
	 *            - key for which data chunk is to be stored
	 * @param sequenceId
	 *            - the sequence id of the key at which data chunk is updated
	 * @param value
	 *            - new value to be updated
	 * @return true if updated successfully, else false
	 */
	@Override
	public boolean update(String key, int sequenceId, byte[] value) {
		boolean isUpdated = false;
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				byte[] serializedSequenceId = serialize(sequenceId);
				if (redis.hexists(serializedKey, serializedSequenceId)) {
					redis.hset(serializedKey, serializedSequenceId, value);
					isUpdated = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isUpdated;
	}

	/**
	 * Method to store data in database with sequence id.
	 * 
	 * @param value
	 *            - data value to be stored in the database
	 * @return randomly generated unique key at which the given value is stored
	 */
	@Override
	public String store(byte[] value) {
		if (value == null) {
			return null;
		}
		String uuid = UUID.randomUUID().toString();
		String key = put(uuid, 0, value);
		return key;
	}

	/**
	 * Method to remove all data chunks at the key.
	 * 
	 * @param key
	 *            - key for which all the data chunks are to be removed
	 * @return - the map containing sequence ids and data chunk which are
	 *         deleted
	 */
	@Override
	public Map<Integer, byte[]> remove(String key) {
		// TODO Auto-generated method stub
		Map<Integer, byte[]> removedMap = new HashMap<Integer, byte[]>();
		try {
			if (hasKey(key)) {
				byte[] serializedKey = serialize(key);
				Set<byte[]> byteSequencIds = redis.hkeys(serializedKey);
				for (byte[] bs : byteSequencIds) {
					removedMap.put((Integer) deserialize(bs), redis.hget(serializedKey, bs));
					redis.hdel(serializedKey, bs);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return removedMap;
	}

	/**
	 * Overridden method to close the jedis connection
	 * 
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {

		if (redis != null) {
			redis.close();
		}
		super.finalize();
	}
}
