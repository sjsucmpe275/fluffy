/**
 * 
 */
package gash.router.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import routing.Pipe.CommandMessage;

/**
 * @author saurabh
 *
 */
public class TaskChannelHandler extends SimpleChannelInboundHandler<CommandMessage>{

	private ServerState state;
	
	public TaskChannelHandler(ServerState state) {

		if (state != null) {
			this.state = state;
		}
	}
	
	public void handleMessage(Channel channel, CommandMessage msg) {
		// TODO Handle tasks
		System.out.println("####################################");
		System.out.println("In task channel handler....");
		System.out.println(msg);
		channel.writeAndFlush(msg);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg)
		throws Exception {
		handleMessage(ctx.channel(), msg);
	}

}
