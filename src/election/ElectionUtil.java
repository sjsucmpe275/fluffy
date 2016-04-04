package election;

import gash.router.server.ServerState;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.WorkMessage;

/**
 * @author saurabh
 *
 */
public class ElectionUtil {

	public WorkMessage createSizeIsMessage(int nodeId, int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(nodeId, destination);

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.SIZEIS);

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);
		return wb.build();
	}

	public WorkMessage createGetClusterSizeMessage(int nodeId,
		int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(nodeId, destination);

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.GETCLUSTERSIZE);

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	public WorkMessage createVoteRequest(ServerState state, int electionId) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(state.getConf().getNodeId(), -1);

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.VOTEREQUEST);
		leaderStatus.setElectionId(electionId);
		leaderStatus.setLeaderId(state.getConf().getNodeId());

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	public WorkMessage createLeaderIsMessage(ServerState state) {

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(state.getConf().getNodeId(), -1);

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.THELEADERIS);
		leaderStatus.setElectionId(state.getElectionId());
		leaderStatus.setLeaderId(state.getConf().getNodeId());
		leaderStatus.setState(LeaderState.LEADERALIVE);
		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);

		return wb.build();
	}

	public WorkMessage createWhoIsTheLeaderMessage(int nodeId,
		int destination) {
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		Header.Builder header = createHeader(nodeId, destination);

		LeaderStatus.Builder leaderStatus = LeaderStatus.newBuilder();
		leaderStatus.setAction(LeaderQuery.WHOISTHELEADER);

		wb.setHeader(header);
		wb.setLeader(leaderStatus);
		wb.setSecret(1);
		return wb.build();
	}

	/**
	 * @param nodeId
	 * @param destination
	 * @return
	 */
	private Header.Builder createHeader(int nodeId, int destination) {
		Header.Builder header = Common.Header.newBuilder();
		header.setNodeId(nodeId);
		header.setDestination(destination);
		header.setMaxHops(10);
		header.setTime(System.currentTimeMillis());
		return header;
	}

}
