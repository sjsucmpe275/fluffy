/**
 * 
 */
package gash.router.server.tasks;

/**
 * @author saurabh
 *
 */
public class BasicRebalancer implements Rebalancer {

	private TaskList taskList;

	public BasicRebalancer(TaskList taskList) {
		this.taskList = taskList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gash.router.server.tasks.Rebalancer#allow()
	 */
	@Override
	public boolean allow() {
		return calcLoad() > 0.5;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gash.router.server.tasks.Rebalancer#calcLoad()
	 */
	@Override
	public float calcLoad() {
		return taskList.numEnqueued() / taskList.getMaxQueueSize();
	}

}
