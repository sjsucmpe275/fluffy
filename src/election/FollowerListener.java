package Election;

/**
 * @author: codepenman.
 * @date: 3/31/16
 */
public interface FollowerListener {

	/*
	* followerId - This will be same as Node-Id
	* */
	void addFollower(int followerId);

	void removeFollower(int followerId);
}
