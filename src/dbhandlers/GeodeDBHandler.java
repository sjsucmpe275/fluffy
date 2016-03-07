/**
 * 
 */
package dbhandlers;

import java.util.Map;
import java.util.UUID;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;

/**
 * @author root
 *
 */
public class GeodeDBHandler implements IDBHandler {

	private Region<String, Object> region;
	private ClientCache cache;

	public GeodeDBHandler() {
	}

	public boolean init(String regionPath) {
		cache = new ClientCacheFactory().addPoolLocator("localhost", 10334).create();
		region = cache.<String, Object> createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY).create("region");
		return region != null;
	}

	public void close() {
		cache.close();
	}

	@Override
	public String put(String key, Object value) {
		if (key == null || value == null) {
			return null;
		}

		region.put(key, value);
		return key;
	}

	@Override
	public String store(Object value) {
		if (value == null) {
			return null;
		}

		String uuid = UUID.randomUUID().toString();
		region.put(uuid, value);
		return uuid;
	}

	@Override
	public Object get(String key) {
		return region.get(key);
	}

	@Override
	public boolean update(String key, Object value) {
		return (null != region.put(key, value));
	}

	@Override
	public Object remove(String key) {
		return region.remove(key);
	}

	@Override
	public boolean hasKey(String key) {
		return region.containsKey(key);
	}

	@Override
	public String getDatabaseVendor() {
		return "ApacheGeode";
	}
}
