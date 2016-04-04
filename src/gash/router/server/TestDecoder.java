//package gash.router.server;
//
//import java.util.List;
//
//import com.google.protobuf.MessageLite;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.MessageToMessageDecoder;
//import io.netty.handler.codec.protobuf.ProtobufDecoder;
//import pipe.work.Work.WorkMessage;
//import routing.Pipe.CommandMessage;
//
//
//public class TestDecoder extends MessageToMessageDecoder<ByteBuf> {
//
//    private final MessageLite prototype;
//
//	ProtobufDecoder workDecoder;
//	ProtobufDecoder commandDecoder;
//	
//	public TestDecoder(MessageLite prototype) {
//		this.prototype = prototype;
//		workDecoder = new ProtobufDecoder(WorkMessage.getDefaultInstance());
//		commandDecoder = new ProtobufDecoder(CommandMessage.getDefaultInstance());
//	}
//	
//	@Override
//	protected void decode(ChannelHandlerContext ctx, ByteBuf msg,
//		List<Object> out) throws Exception {
//		if(proto)
//		workDecoder.de
//	}
//
//}
