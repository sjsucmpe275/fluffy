/**
 * 
 */
package gash.router.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Prasanna
 *
 */
public class RoundRobinStrategy implements IReplicationStrategy {

	private int index;
	private int size;
	
	public RoundRobinStrategy(int size) {
		this.size = size;
		index = 0;
	}
	
	/* (non-Javadoc)
	 * @see gash.router.server.tasks.IReplicationStrategy#getNodeIds(com.gemstone.gemfire.internal.concurrent.ConcurrentHashSet)
	 */
	@Override
	public List<Integer> getNodeIds(List<Integer> activeNodes) {
		List<Integer> output = new ArrayList();
		activeNodes = Collections.synchronizedList(activeNodes);
		int temp = 0;
		index++;
		while(output.size() < size && activeNodes.size() >= size) {
			output.add(activeNodes.get(activeNodes.size() % (index + temp)));
			temp++;
		}
		return output;
	}

}
