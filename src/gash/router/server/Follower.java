package gash.router.server;

/*
 *   @author: codepenman
 * @date: 28/03/2016
 * This class maintains the state of every follower of this leader...
 */
public class Follower {
	private int followerId; //None other than Node Id of the Follower
	private Boolean isAlive;

	Follower(int followerId, Boolean isAlive)   {
		this.followerId = followerId;
		this.isAlive = isAlive;
	}

	public int getFollowerId()  {
		return followerId;
	}

	public Boolean getIsAlive() {
		return isAlive;
	}

	public void setIsAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}
}
