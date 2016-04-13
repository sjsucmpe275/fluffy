package gash.router.server.messages.cmd_messages.handlers;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

/**
 * Created by codepenman on 3/27/16.
 */
public interface ICmdMessageHandler {

	void handleMessage(CommandMessage cmdMessage, Channel channel) throws Exception;

	void setNextHandler(ICmdMessageHandler nextHandler);
}
