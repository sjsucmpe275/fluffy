/**
 * Copyright 2012 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.container;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Routing information for the server - internal use only
 * 
 * @author gash
 * 
 */
@XmlRootElement(name = "conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class RoutingConf  {
	private AtomicInteger nodeId;
	private AtomicInteger commandPort;
	private AtomicInteger workPort;
	private AtomicInteger adaptorPort;
	private AtomicBoolean internalNode;
	private AtomicInteger heartbeatDt;
	private AtomicReference<String> database;
	private AtomicInteger electionTimeout;
	private AtomicInteger maxHops;
	private AtomicInteger secret;

	public RoutingConf(){
		nodeId=new AtomicInteger();
		internalNode=new AtomicBoolean(true);
		heartbeatDt=new AtomicInteger(2000);
		workPort=new AtomicInteger();
		commandPort=new AtomicInteger();
		database= new AtomicReference<> ("redis");
		electionTimeout=new AtomicInteger();
		maxHops = new AtomicInteger ();
		adaptorPort = new AtomicInteger(0);
		secret = new AtomicInteger(0);
	}

	//private List<RoutingEntry> routing;
	public List<RoutingEntry> routing = Collections.synchronizedList(new ArrayList<RoutingEntry>());
	public List<RoutingEntry> adaptorRouting = Collections.synchronizedList(new ArrayList<RoutingEntry>());
	public HashMap<String, Integer> asHashMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		if (routing != null) {
			for (RoutingEntry entry : routing) {
				map.put(entry.host, entry.port);
			}
		}
		return map;
	}
	public HashMap<String, Integer> asAdaptorHashMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		if (adaptorRouting != null) {
			for (RoutingEntry entry : routing) {
				map.put(entry.host, entry.port);
			}
		}
		return map;
	}

	public void addEntry(RoutingEntry entry) {
		if (entry == null)
			return;

		if (routing == null)
			routing = new ArrayList<RoutingEntry>();

		routing.add(entry);
	}
	public void addAdaptorEntry(RoutingEntry entry) {
		if (entry == null)
			return;

		if (adaptorRouting == null)
			adaptorRouting = new ArrayList<RoutingEntry>();

		adaptorRouting.add(entry);
	}

	public int getNodeId() {
		return nodeId.get();
	}
	public int getAdaptorPort() {
		return adaptorPort.get();
	}
	public void setAdaptorPort(int adaptorPort) {
		this.adaptorPort.getAndSet(adaptorPort);
	}
	public void setNodeId(int nodeId) {
		this.nodeId.getAndSet(nodeId);
	}

	public int getCommandPort() {
		return commandPort.get();
	}

	public void setCommandPort(int commandPort) {
		this.commandPort.getAndSet(commandPort) ;
	}

	public int getWorkPort() {
		return workPort.get();
	}

	public void setWorkPort(int workPort) {
		this.workPort.getAndSet(workPort) ;
	}

	public boolean isInternalNode() {
		return internalNode.get();
	}

	public void setInternalNode(boolean internalNode) {
		this.internalNode.getAndSet(internalNode);
	}

	public int getHeartbeatDt() {
		return heartbeatDt.get();
	}

	public void setHeartbeatDt(int heartbeatDt) {
		this.heartbeatDt.getAndSet(heartbeatDt);
	}

	public List<RoutingEntry> getRouting() {
		return routing;
	}

	public List<RoutingEntry> getAdaptorRouting() {
		return adaptorRouting;
	}

	public void setRouting(List<RoutingEntry> conf) {
		this.routing = conf;
	}

	public void setAdaptorRouting(List<RoutingEntry> conf) {
		this.adaptorRouting = conf;
	}
	public String getDatabase() {
		return database.get();
	}

	public void setDatabase(String database) {
		this.database.getAndSet(database);
	}

	public int getElectionTimeout() {
		return electionTimeout.get();
	}

	public void setElectionTimeout(int electionTimeout) {
		this.electionTimeout.getAndSet(electionTimeout);
	}

	public int getSecret() {
		return secret.get();
	}
	
	public void setSecret(int secret) {
		this.secret.set(secret);
	}

	public int getMaxHops() {
		return maxHops.get ();
	}

	public void setMaxHops(int maxHops) {
		this.maxHops.getAndSet (maxHops);
	}

	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.PROPERTY)
	public static final class RoutingEntry {
		private String host;
		private int port;
		private int id;

		public RoutingEntry() {
		}

		public RoutingEntry(int id, String host, int port) {
			this.id = id;
			this.host = host;
			this.port = port;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}
}
