package gash.router.server.cmd_messages;

import io.netty.channel.Channel;
import routing.Pipe.*;

/**
 * Created by codepenman on 3/27/16.
 */
public interface ICmdMessageHandler {
	void handleMessage(CommandMessage workMessage, Channel channel) throws Exception;

	void nextHandler(ICmdMessageHandler nextHandler);
}
