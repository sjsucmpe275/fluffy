package election;

import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.election.Election;
import pipe.work.Work.WorkMessage;

import java.util.ArrayList;

public class Leader implements INodeState, FollowerListener{

	private ServerState state;
	private ArrayList<Integer> activeNodes;
	
	public Leader(ServerState state) {
		this.state = state;
		activeNodes = new ArrayList<> ();
	}

	@Override
	public void beforeStateChange() {

	}

	@Override
	public void afterStateChange() {

	}

	@Override
	public void onNewOrHigherTerm() {

	}

	@Override
	public void onLeaderDiscovery() {

	}

	@Override
	public void onHigherTerm() {

	}

	@Override
	public void handleMessage(WorkMessage workMessage, Channel channel) {
		Election.LeaderStatus leaderStatus = workMessage.getLeader();
		switch (leaderStatus.getAction()) {
		case GETCLUSTERSIZE:
			break;
		case SIZEIS:
			break;
		case THELEADERIS:
			break;
		case VOTEREQUEST:
			break;
		case VOTERESPONSE:
			break;
		case WHOISTHELEADER:
			break;
		default:

		}
	}

	@Override
	public void addFollower(int followerId) {
		activeNodes.add (followerId);
	}

	@Override
	public void removeFollower(int followerId) {
		activeNodes.remove (followerId);
	}
}
