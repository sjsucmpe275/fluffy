/**
 * 
 */
package gash.router.server.tasks;

import java.util.List;

/**
 * @author Prasanna
 *
 */
public interface IReplicationStrategy {

	List<Integer> getNodeIds(List<Integer> activeNodes);

}
