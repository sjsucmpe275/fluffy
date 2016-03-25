/**
 * 
 */
package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.ByteString;

/**
 * @author saurabh
 *
 */
public class SerializationUtil {

	/**
	 * Method to read file in chunks of ByteString.
	 * 
	 * @param file - file to read
	 * @param from - chunk offset to read from
	 * @param size - size of the chunk
	 * @param N - number of chunks to read. If -1, complete file is read.
	 * @return List of ByteString which contains requested chunks.
	 *
	 * TODO: Throw exceptions
	 * TODO: Trim the last chunk
	 */
	public List<ByteString> readfile(String file, long from, int size, int N) {

		List<ByteString> output = new LinkedList<>();
		BufferedInputStream bis = null;
		try {
			
			// buffer to read file
			byte[] data = new byte[size];
			bis = new BufferedInputStream(new FileInputStream(file));
			
			// Skip file to the chunk to read from
			bis.skip(from * size);
			int index = 0;
			
			// Read file
			while (bis.read(data) > 0 && (N == -1 || index++ < N)) {
				output.add(ByteString.copyFrom(data));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return output;
	}

	/**
	 * 
	 * @param file - output file location
	 * @param input - list of ByteString chunks to write into the file
	 * 
	 * TODO: Throw exceptions
	 */
	public void writeFile(String file, List<ByteString> input) {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
			for (ByteString data : input) {
				bos.write(data.toByteArray());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
