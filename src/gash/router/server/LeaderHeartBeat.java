package gash.router.server;

import gash.router.server.wrk_messages.BeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.TimerTask;

/*
*   @author: codepenman
* @date: 28/03/2016
* */
public class LeaderHeartBeat extends TimerTask {

	protected static Logger logger = LoggerFactory.getLogger("Leader Heart Beat");

	private ServerState state;

	//Todo: Harish: Are there any other parameters that has to be passed to this constructor ?
	public LeaderHeartBeat(ServerState state)    {
		this.state = state;
	}

	@Override
	public void run() {
		BeatMessage beatMessage = new BeatMessage (state.getConf ().getNodeId ());
		HashMap<Integer, Follower> followers = state.getFollowers();

		followers.entrySet ()
				.stream ()
				.filter (follower -> !follower.getValue ().getIsAlive ())
				.forEach (follower -> {
			logger.info ("Follower " + follower.getValue ().getFollowerId () + " is down");
		});

		state.resetFollowersState();

		state.getEmon ().broadcastMessage (beatMessage.getMessage ());
	}
}
