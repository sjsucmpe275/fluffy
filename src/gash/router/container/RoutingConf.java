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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Routing information for the server - internal use only
 * 
 * @author gash
 * 
 */
@XmlRootElement(name = "conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class RoutingConf {
	private int nodeId;
	private int commandPort;
	private int workPort;
	private boolean internalNode = true;
	private int heartbeatDt = 2000;
	private List<RoutingEntry> routing;

	public HashMap<String, Integer> asHashMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		if (routing != null) {
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

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getCommandPort() {
		return commandPort;
	}

	public void setCommandPort(int commandPort) {
		this.commandPort = commandPort;
	}

	public int getWorkPort() {
		return workPort;
	}

	public void setWorkPort(int workPort) {
		this.workPort = workPort;
	}

	public boolean isInternalNode() {
		return internalNode;
	}

	public void setInternalNode(boolean internalNode) {
		this.internalNode = internalNode;
	}

	public int getHeartbeatDt() {
		return heartbeatDt;
	}

	public void setHeartbeatDt(int heartbeatDt) {
		this.heartbeatDt = heartbeatDt;
	}

	public List<RoutingEntry> getRouting() {
		return routing;
	}

	public void setRouting(List<RoutingEntry> conf) {
		this.routing = conf;
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
