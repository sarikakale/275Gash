package gash.router.dns.server;

import java.util.HashMap;

import ClientFacingMapping.Mapping.ClientFacingMessage;
import ClientFacingMapping.Mapping.MapAction;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import pipe.work.Work.WorkMessage;


public class IPRequestHandler  extends SimpleChannelInboundHandler<ClientFacingMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ClientFacingMessage msg) throws Exception {
		// TODO Auto-generated method stub
		handleMessage(msg, ctx.channel());
		
	}
	public void handleMessage(ClientFacingMessage msg, Channel channel) {
		try {
			if (msg.getMapaction().equals(MapAction.WHICHSERVER)){
				ClientFacingMessage.Builder cfm =  ClientFacingMessage.newBuilder();
				cfm.setMapaction(MapAction.SERVERIS);
				cfm.setIp(IPList.getHostIP());
				cfm.setPort(IPList.getHostport());
				channel.writeAndFlush(cfm.build());	
				System.out.println("Inside which");
			}		
			else if(msg.getMapaction().equals(MapAction.SERVERIS)){
				System.out.println("HEllo" + msg.getIp());
				IPList.setHostIP(msg.getIp());
			}
	}catch(Exception e){
		
	}	

	}
}
