package gash.router.server;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class Worker extends Thread{
	private CommandChannelHandler ref;
	private boolean forever = true;
	public Worker(CommandChannelHandler c){
		this.ref=c;
		if (ref.outbound == null)
			throw new RuntimeException("connection worker detected null queue");
	}
	@Override
	public void run() {
		System.out.println("--> starting worker thread");
		System.out.flush();
		Channel ch = null;
		
		try {
			ch = ref.connect();
		} catch(Exception e) {
			System.out.println("Client is not available! Try again later...");
			System.exit(0);;
		}
		
		if (ch == null || !ch.isOpen() || !ch.isActive()) {
			CommandChannelHandler.logger.error("connection missing, no outbound communication");
			return;
		}

		while (true) {
			if (!forever && ref.outbound.size() == 0)
				break;

			try {
				// block until a message is enqueued AND the outgoing
				// channel is active
				CommandMessage msg = ref.outbound.take();
				if (ch.isWritable()) {
					if (!ref.write(msg)) {
						ref.outbound.putFirst(msg);
					}

					System.out.flush();
				} else {
					System.out.println("--> channel not writable- tossing out msg!");

					// conn.outbound.putFirst(msg);
				}

				System.out.flush();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
				break;
			} catch (Exception e) {
				CommandChannelHandler.logger.error("Unexpected communcation failure", e);
				break;
			}
		}

		if (!forever) {
			CommandChannelHandler.logger.info("connection queue closing");
		}
	}
}