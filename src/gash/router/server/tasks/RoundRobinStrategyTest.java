package gash.router.server.tasks;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class RoundRobinStrategyTest {
	List<Integer> activeNodes;
	IReplicationStrategy strategy;
	int nodesCount;
	int index;

	@Before
	public void setUp() throws Exception {
		index=1;
		nodesCount=5;
		activeNodes=new ArrayList<>();
		for(int i=1;i<=nodesCount;i++){
			this.activeNodes.add(i);	
		}
		strategy=new RoundRobinStrategy(2);
	}

	@Test
	public void testGetNodeIds() {
		List<Integer> output=new ArrayList<Integer>();
		
		for(int j=0;j<20;j++){
			int temp=0;
			output=strategy.getNodeIds(activeNodes);
			for(Integer follower: output){
				index=(index+temp)%activeNodes.size();
				temp++;
				assertEquals(activeNodes.get(index), follower);
				
			}	
		}
	}

}
