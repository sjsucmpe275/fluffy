package Election;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public interface INodeState {
	public void handleMessage(WorkMessage workMessage, Channel channel);
}
