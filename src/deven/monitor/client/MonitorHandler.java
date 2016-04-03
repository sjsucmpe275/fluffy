/*
 * copyright 2016, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package deven.monitor.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.monitor.Monitor.ClusterMonitor;

public class MonitorHandler extends SimpleChannelInboundHandler<ClusterMonitor> {

	protected ConcurrentMap<String, MonitorListener> listeners = new ConcurrentHashMap<String, MonitorListener>();
	public MonitorHandler() {
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, ClusterMonitor arg1) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public void addListener(MonitorListener listener) {
		if (listener == null)
			return;

		listeners.putIfAbsent(listener.getListenerID(), listener);
	}

}