package gash.router.server.messages.cmd_messages.handlers;

import gash.router.server.CommandChannelHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import pipe.common.Common;
import routing.Pipe.CommandMessage;

/**
 * Created by codepenman on 3/27/16.
 */
public class CmdPingMsgHandler implements ICmdMessageHandler {

	private final CommandChannelHandler cmdChannelHandler;
	private ICmdMessageHandler nextHandler;

	public CmdPingMsgHandler(CommandChannelHandler cmdChannelHandler) {
		this.cmdChannelHandler = cmdChannelHandler;
	}

	@Override
	public void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception{
		if(cmdMessage.hasPing ())  {
			handle(cmdMessage, channel);
		}else   {
			if(nextHandler != null) {
				nextHandler.handleMessage (cmdMessage, channel);
			}else   {
				System.out.println("*****No Handler available*****");
			}
		}
	}

	private void handle(CommandMessage cmdMessage, Channel channel) {

		cmdChannelHandler.getLogger ().info("ping from " + cmdMessage.getHeader().getNodeId());
		// construct the message to send
		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(888);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setPing(true);

		ChannelFuture cf = channel.writeAndFlush (rb.build());
		if(!cf.isSuccess ())    {
			System.out.println("Reasion for failure : " + cf);
		}
	}

	@Override
	public void setNextHandler(ICmdMessageHandler nextHandler) {
		this.nextHandler = nextHandler;
	}
}
