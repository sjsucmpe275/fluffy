package election;

import io.netty.channel.Channel;
import pipe.election.Election.LeaderStatus;
import pipe.work.Work.WorkMessage;

public interface INodeState {

	void beforeStateChange();

	void afterStateChange();

	void onNewOrHigherTerm();

	void onLeaderDiscovery();

	void onHigherTerm();

	void handleGetClusterSize(WorkMessage workMessage, Channel channel);

	void handleSizeIs(WorkMessage workMessage, Channel channel);

	void handleLeaderIs(WorkMessage workMessage, Channel channel);

	void handleVoteRequest(WorkMessage workMessage, Channel channel);

	void handleVoteResponse(WorkMessage workMessage, Channel channel);

	void handleWhoIsTheLeader(WorkMessage workMessage, Channel channel);
	
	void handleBeat(WorkMessage workMessage, Channel channel);

	default void handleMessage(WorkMessage workMessage, Channel channel) {
		LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			handleGetClusterSize(workMessage, channel);
			break;
		case SIZEIS:
			handleSizeIs(workMessage, channel);
			break;
		case THELEADERIS:
			handleWhoIsTheLeader(workMessage, channel);
			break;
		case VOTEREQUEST:
			handleVoteRequest(workMessage, channel);
			break;
		case VOTERESPONSE:
			handleVoteResponse(workMessage, channel);
			break;
		case WHOISTHELEADER:
			handleWhoIsTheLeader(workMessage, channel);
			break;
		default:
		}
	}
}