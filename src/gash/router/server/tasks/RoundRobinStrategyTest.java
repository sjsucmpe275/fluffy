package gash.router.server.tasks;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class RoundRobinStrategyTest {

	private List<Integer> activeNodes;
	private IReplicationStrategy strategy;
	private int nodesCount;
	private int index;

	@Before
	public void setUp() throws Exception {
		this.index = 1;
		this.nodesCount = 5;
		this.activeNodes = new ArrayList<>();
		for (int i = 1; i <= nodesCount; i++) {
			this.activeNodes.add(i);
		}
		this.strategy = new RoundRobinStrategy(2);
	}

	@Test
	public void testGetNodeIds() {
		List<Integer> output = new ArrayList<Integer>();

		for (int j = 0; j < 20; j++) {
			int temp = 0;
			output = strategy.getNodeIds(activeNodes);
			for (Integer follower : output) {
				index = (index + temp) % activeNodes.size();
				temp++;
				assertEquals(activeNodes.get(index), follower);

			}
		}
	}
}
