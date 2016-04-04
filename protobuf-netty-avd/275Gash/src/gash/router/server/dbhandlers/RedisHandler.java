package gash.router.server.dbhandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;



public class RedisHandler implements DbHandler {
	Jedis jedis;
	static JedisPool jedisPool;

	@Override
	public void addEntry(String key, String value) {
		// TODO Auto-generated method stub

	}

	public RedisHandler() {
		// TODO Auto-generated constructor stub
		this.jedis = new Jedis("localhost");

	}

	@Override
	public long encryptKey(String keyValue) {
		// TODO Auto-generated method stub

		Checksum checksum = new CRC32();

		// update the current checksum with the specified array of bytes

		checksum.update(keyValue.getBytes(), 0, keyValue.length());

		// get the current checksum value

		return checksum.getValue();

	}

	public Jedis jedisPool() {
		try {
			if (jedisPool == null) {

				JedisPoolConfig config = new JedisPoolConfig();
				
				jedisPool = new redis.clients.jedis.JedisPool("localhost");

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jedisPool.getResource();

	}

	@Override
	public void insertData(String key, int sequenceId, String value) {

		try {
			jedis.hset(/*Long.toString(encryptKey(key))*/key, Integer.toString(sequenceId), value);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Map<String, String> retrieveData(String key) {

		Map<String, String> data = new HashMap<String, String>();
		
		try {
			if (jedis.exists(key)) {

				//long encryptedKey = encryptKey(key);

				Set<String> sequenceIds = jedis.hkeys(/*Long.toString(encryptedKey)*/key);
				
				for (String b : sequenceIds) {

					data.put(b, jedis.hget(key, b));
					

				}

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return data;

	}

	@Override
	public Map<String, String> removeData(String key) {
		Map<String, String> data = new HashMap<String, String>();

		if (jedis.exists(key)) {

			long encryptedKey = encryptKey(key);

			Set<String> sequenceIds = jedis.hkeys(/*Long.toString(encryptedKey)*/key);

			for (String b : sequenceIds) {

				data.put(b, jedis.hget(/*Long.toString(encryptedKey)*/key, b));
				jedis.hdel(/*Long.toString(encryptedKey)*/key, b);
				System.out.println(data.size());
			}	

		}

		return data;
	}

}
