/**
 * 
 */
package dbhandlers;

import java.util.List;
import java.util.Map;

/**
 * @author Saurabh
 *
 */
public interface IDBHandler {

	/**
	 * Method to store data in database with sequence id. 
	 * 
	 * Two tables should be maintained. One to store key to sequence id pair.
	 * Another to store data against key+sequenceId. 
	 * 
	 * @param key - Key as a string
	 * @param sequenceId - Sequence Id for the data chunk
	 * @param value - Data to store
	 * @return Key at which object is stored
	 */
	public String put(String key, int sequenceId, byte[] value);
	
	/**
	 * Method to store object. Key should be calculated implicitly.<br>
	 *
	 * Note: We should not store more than one chunk of data using store. <br>
	 * <b>Should maintain entries in both tables with sequenceId as 0.</b>
	 * 
	 * @param value - Object to store
	 * @return Key at which object is stored
	 */
	public String store(byte[] value);


	/**
	 * Method to retrieve data stored at the key.
	 * DBHandlers should fetch all the chunks for the data with the key.
	 * <pre>Process should be as follows: 
	 * 1. Fetch all the sequence IDs from table1.
	 * 2. Fetch all data chunks from the table2.
	 * 3. Return map of chunkId and Data as byte array.
	 * </pre>
	 * @param key
	 * @return
	 */
	public Map<Integer, byte[]> get(String key);
	
	/**
	 * Update chunk of data stored at sequenceId.
	 * 
	 * @param key
	 * @param sequenceId
	 * @param value
	 * @return
	 */
	public boolean update(String key, int sequenceId, byte[] value);
	

	/**
	 * Method to remove all data chunks at the key.
	 * 
	 * @param key - key to remove
	 * @return Map of chunk id and data
	 */
	public Map<Integer, byte[]> remove(String key);
	
	/**
	 * Method to check if the key is present in the database.
	 * 
	 * @param key
	 * @return true if key is present; otherwise false.
	 */
	public boolean hasKey(String key);

	/**
	 * Get all sequenceIds present at the node.
	 * 
	 * @param key - Key 
	 * @return List of sequenceIds present for the key.
	 */
	public List<Integer> getSequenceIds(String key);
	
	/**
	 * Utility method to know which database is used at the back end.
	 * 
	 * @return
	 */
	public String getDatabaseVendor();
}