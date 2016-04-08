/**
 * Copyright 2016 Gash.
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
package gash.router.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common.Failure;
import pipe.common.Common.Header;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;

public class PrintUtil {
	private static final Logger logger = LoggerFactory.getLogger("Print Util");
	private static final String gap = "   ";

	public static void printHeader(Header hdr) {
		logger.info("\n-------------------------------------------------------");
		logger.info("ID:   " + hdr.getNodeId());
		logger.info("Time: " + hdr.getTime());
		if (hdr.hasMaxHops())
			logger.info("Hops: " + hdr.getMaxHops());
		if (hdr.hasDestination())
			logger.info("Dest: " + hdr.getDestination());

	}

	public static void printCommand(CommandMessage msg) {
		PrintUtil.printHeader(msg.getHeader());

		System.out.print("\nCommand: ");
		if (msg.hasErr()) {
			logger.info("Failure");
			logger.info(PrintUtil.gap + "Code:    " + msg.getErr().getId());
			logger.info(PrintUtil.gap + "Ref ID:  " + msg.getErr().getRefId());
			logger.info(PrintUtil.gap + "Message: " + msg.getErr().getMessage());
		} else if (msg.hasPing())
			logger.info("Ping");
		else if (msg.hasMessage()) {
			logger.info("Message");
			logger.info(PrintUtil.gap + "Msg:  " + msg.getMessage());
		} else if (msg.hasQuery()) {
			logger.info("Query");
//			logger.info(msg.getQuery().getAction().name());
//			logger.info(msg.getQuery().getKey());
//			logger.info(msg.getQuery().getData());
			logger.info("--------------------");
		} else {
			logger.info("Unknown");
		}
	}

	public static void printWork(WorkMessage msg) {
//		PrintUtil.printHeader(msg.getHeader());

		System.out.print("\nWork: ");
		if (msg.hasErr())
			logger.info("Failure");
		else if (msg.hasPing())
			logger.info("Ping");
		else if(msg.hasBeat ())
			logger.info("Beat");
		else if(msg.hasLeader()) {
			logger.info("Election");
			logger.info(msg.toString ());
		}else if(msg.hasTask()){
			logger.info("Task");
		} 
		else
			logger.info("Unknown");

//		logger.info(PrintUtil.gap + "Secret:  " + msg.getSecret());
	}

	public static void printFailure(Failure f) {
		logger.info("ERROR: " + f.getId() + "." + f.getRefId() + " : " + f.getMessage());
	}
}
