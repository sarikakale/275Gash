package gash.router.client;

import ClientFacingMapping.Mapping.ClientFacingMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientFacingServerHandler  extends SimpleChannelInboundHandler<ClientFacingMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ClientFacingMessage msg) throws Exception {

		handleMessage(msg, ctx.channel());

	}
	public void handleMessage(ClientFacingMessage msg, Channel channel) {
		try {
			IPList.setHostIP(msg.getIp());
			System.out.println("HostIP "+msg.getIp() + msg.getPort());
			IPList.setHostPort(msg.getPort());
			IPList.setRead(true);
		}catch(Exception e){

		}

	}
}

