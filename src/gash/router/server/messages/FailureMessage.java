package gash.router.server.messages;

import com.google.protobuf.GeneratedMessage;
import pipe.common.Common;
import pipe.common.Common.Failure;
import pipe.work.Work.*;
import routing.Pipe.CommandMessage;

/**
 * @author: codepenman.
 * @date: 3/28/16
 */
public class FailureMessage {

	private final GeneratedMessage generatedMessage;
	private final Exception exception;
	private Common.Header.Builder header;
	private int nodeId = -1;
	private int destination = -1;

	public FailureMessage(GeneratedMessage generatedMessage, Exception exception) {
		this.generatedMessage = generatedMessage;
		this.exception = exception;
		header = Common.Header.newBuilder();
	}

	public CommandMessage getCommandMessage()  {
		CommandMessage cmdMessage = (CommandMessage) generatedMessage;

		if(nodeId == -1)    {
			nodeId = cmdMessage.getHeader ().getDestination ();
		}

		Failure.Builder eb = Failure.newBuilder();
		eb.setId(nodeId);
		eb.setRefId(cmdMessage.getHeader().getNodeId());
		eb.setMessage(exception.getMessage());

		CommandMessage.Builder cmdMsg = CommandMessage.newBuilder(cmdMessage);
		cmdMsg.setErr(eb);

		header.setNodeId(nodeId);
		header.setTime(System.currentTimeMillis());
		header.setDestination(destination);
		cmdMsg.setHeader(header);

		return cmdMsg.build ();
	}

	public WorkMessage getWorkMessage()  {
		WorkMessage wrkMessage = (WorkMessage) generatedMessage;

		if(nodeId == -1)    {
			nodeId = wrkMessage.getHeader ().getDestination ();
		}

		Failure.Builder eb = Failure.newBuilder();
		eb.setId(nodeId);
		eb.setRefId(wrkMessage.getHeader().getNodeId());
		eb.setMessage(exception.getMessage());

		WorkMessage.Builder workMsg = WorkMessage.newBuilder(wrkMessage);
		workMsg.setErr(eb);

		header.setNodeId(nodeId);
		header.setTime(System.currentTimeMillis());
		header.setDestination(destination);
		workMsg.setHeader(header);

		return workMsg.build ();
	}

	public void setNodeId(int nodeId)   {
		this.nodeId = nodeId;
	}

	public void setDestination(int destination) {
		this.destination = destination;
	}
}
