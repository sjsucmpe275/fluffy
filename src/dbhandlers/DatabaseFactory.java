package dbhandlers;

/**
 * @author: codepenman.
 * @date: 3/28/16
 */
public class DatabaseFactory {

	//Todo: Should return the respective handler based on the different databases we support
	public IDBHandler getDatabaseHandler(String dbName) throws Exception{

		if(dbName.equals ("redis")) {
			return null;
		}

		if(dbName.equals ("memcached")) {
			return null;
		}

		throw new Exception("Database is not defined, Supported databases are Redis and Memcached");
	}
}
