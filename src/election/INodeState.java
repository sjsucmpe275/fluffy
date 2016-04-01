package Election;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public interface INodeState {

	void beforeStateChange();

	void afterStateChange();

	void onNewOrHigherTerm();

	void onLeaderDiscovery();

	void onHigherTerm();

	void handleMessage(WorkMessage workMessage, Channel channel);

}
