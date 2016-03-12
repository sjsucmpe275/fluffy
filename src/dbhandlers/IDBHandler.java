/**
 * 
 */
package dbhandlers;

/**
 * @author Saurabh
 *
 */
public interface IDBHandler {

	/**
	 * Method to store object in database.
	 * 
	 * @param key - Key as a string. We can consider this as object as well.
	 * @param value - Object to store
	 * @return Key at which object is stored
	 */
	public String put(String key, Object value);

	/**
	 * Method to store object. Key should be calculated implicitly.
	 * 
	 * @param value - Object to store
	 * @return Key at which object is stored
	 */
	public String store(Object value);

	/**
	 * Method to retrieve the object stored at the key.
	 * 
	 * @param key
	 * @return Object if present; otherwise null.
	 */
	public Object get(String key);

	/**
	 * Method to update/overwrite previously stored object.
	 * 
	 * @param key
	 * @param value
	 * @return boolean to indicate if object is updated successfully. We can
	 *         return key here instead.
	 */
	public boolean update(String key, Object value);

	/**
	 * Method to remove object at the key.
	 * 
	 * @param key
	 * @return removed object. If key not present return null.
	 */
	public Object remove(String key);

	/**
	 * Method to check if the key is present in the database.
	 * 
	 * @param key
	 * @return true if key is present; otherwise false.
	 */
	public boolean hasKey(String key);

	/**
	 * Utility method to know which database is used at the back end.
	 * 
	 * @return
	 */
	public String getDatabaseVendor();

}
