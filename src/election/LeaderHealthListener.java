package election;

/**
 * @author: codepenman.
 * @date: 4/1/16
 */
public interface LeaderHealthListener {
	/*
	* implement this method if you are interested in the notification on leader bad health*/
	void onLeaderBadHealth();
}
