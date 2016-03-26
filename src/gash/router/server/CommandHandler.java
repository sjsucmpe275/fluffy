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

import gash.router.container.RoutingConf;
import io.netty.channel.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import dbhandlers.IDBHandler;
import dbhandlers.RedisDBHandler;
import pipe.common.Common;
import pipe.common.Common.Failure;
import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;
import storage.Storage.Query;
import storage.Storage.Response;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {
	protected static Logger logger = LoggerFactory.getLogger("cmd");
	protected RoutingConf conf;
	protected IDBHandler dbhandler;

	public CommandHandler(RoutingConf conf) {
		if (conf != null) {
			this.conf = conf;
		}
		dbhandler = new RedisDBHandler();
	}

	/**
	 * override this method to provide processing behavior. This implementation
	 * mimics the routing we see in annotating classes to support a RESTful-like
	 * behavior (e.g., jax-rs).
	 * 
	 * @param msg
	 */
	public void handleMessage(CommandMessage msg, Channel channel) {
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

		PrintUtil.printCommand(msg);

		try {
			// TODO How can you implement this without if-else statements?
			if (msg.hasPing()) {
				logger.info("ping from " + msg.getHeader().getNodeId());
				// construct the message to send
				Common.Header.Builder hb = Common.Header.newBuilder();
				hb.setNodeId(888);
				hb.setTime(System.currentTimeMillis());
				hb.setDestination(-1);

				CommandMessage.Builder rb = CommandMessage.newBuilder();
				rb.setHeader(hb);
				rb.setPing(true);

				ChannelFuture cf = channel.writeAndFlush(rb.build());
				if (!cf.isSuccess()) {
					System.out.println("Reasion for failure : " + cf);
				}
			} else if (msg.hasMessage()) {
				logger.info(msg.getMessage());
			} else if (msg.hasQuery()) {
				
				Query query = msg.getQuery();
				Header.Builder hb = buildHeader();
				Response.Builder rb = Response.newBuilder();
				CommandMessage.Builder cb = CommandMessage.newBuilder();
				String key = query.getKey();
				
				switch (query.getAction()) {
				case GET:
					rb.setAction(query.getAction());
					Object data = dbhandler.get(key);
					if (data == null) {
						
						Failure.Builder fb = Failure.newBuilder();
						fb.setMessage("Key not present");
						fb.setId(101);
						
						rb.setSuccess(false);
						rb.setFailure(fb);

						cb.setHeader(hb);
						cb.setResponse(rb);
						channel.writeAndFlush(cb.build());
					} else {
						rb.setSuccess(true);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ObjectOutputStream os = new ObjectOutputStream(bos);
						
						os.writeObject(data);
						rb.setData(ByteString.copyFrom(bos.toByteArray()));
						rb.setInfomessage("Action completed successfully!");
						rb.setKey(key);
						
						cb.setHeader(hb);
						cb.setResponse(rb);
						channel.writeAndFlush(cb.build());
						
					}
					break;

				case DELETE:
					data = dbhandler.remove(key);
					if (data == null) {
						Failure.Builder fb = Failure.newBuilder();
						fb.setMessage("Key not present");
						fb.setId(101);
						
						rb.setSuccess(false);
						rb.setFailure(fb);

						cb.setHeader(hb);
						cb.setResponse(rb);
						channel.writeAndFlush(cb.build());
					}  else {
						rb.setSuccess(true);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ObjectOutputStream os = new ObjectOutputStream(bos);
						
						os.writeObject(data);
						rb.setData(ByteString.copyFrom(bos.toByteArray()));
						rb.setInfomessage("Action completed successfully!");
						rb.setKey(key);
						
						cb.setHeader(hb);
						cb.setResponse(rb);
						channel.writeAndFlush(cb.build());
					}
					break;

				case STORE:
					
					if (query.hasKey()) {
						key = dbhandler.put(query.getKey(), query.getData().toByteArray());
					} else {
						key = dbhandler.store(query.getData().toByteArray());
					}

					rb.setAction(query.getAction());
					rb.setKey(key);
					rb.setSuccess(true);
					rb.setInfomessage("Data stored successfully at key: " + key);
					
					cb.setHeader(hb);
					cb.setResponse(rb);
					channel.writeAndFlush(cb.build());
					break;

				case UPDATE:
					key = dbhandler.put(query.getKey(), query.getData().toByteArray());
					rb.setAction(query.getAction());
					rb.setKey(key);
					rb.setSuccess(true);
					rb.setInfomessage("Data stored successfully at key: " + key);
					
					cb.setHeader(hb);
					cb.setResponse(rb);
					channel.writeAndFlush(cb.build());
					break;

				default:
					logger.info("Default case!");
					break;
				}
			}

		} catch (Exception e) {
			// TODO add logging
			logger.error("Got an exception in command", e);
			Failure.Builder eb = Failure.newBuilder();
			eb.setId(conf.getNodeId());
			eb.setRefId(msg.getHeader().getNodeId());
			eb.setMessage(e.getMessage());
			CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
			rb.setErr(eb);
			rb.setHeader(buildHeader());
			channel.write(rb.build());
		}

		System.out.flush();
	}

	private Header.Builder buildHeader() {
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(conf.getNodeId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);
		return hb;
	}

	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}
}