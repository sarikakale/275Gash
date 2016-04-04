package gash.router.server;


import java.util.HashMap;

import ClientFacingMapping.Mapping.ClientFacingMessage;
import ClientFacingMapping.Mapping.MapAction;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class CallClientFacingServer {
	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
	private static int DNSSERVERPORT = 4000; //port of the command prompt
	private static Channel channel;
	private static EventLoopGroup group; 
	private static ChannelFuture future;
	
	public static void UpdateLeaderDNSEntry(String host) {
		try {
			
			group = new NioEventLoopGroup();
			//WorkInit si = new WorkInit(null, false);
			MappingServerInit mi = new MappingServerInit(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(mi);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			System.out.println("Channel  ---- " + host);

			// Make the connection attempt.b.bind

			future = b.connect(CommonUtils.DNS_HOST, CommonUtils.DNS_PORT).syncUninterruptibly();
			channel = future.channel();
			ClientFacingMessage.Builder cfm =  ClientFacingMessage.newBuilder();
			cfm.setMapaction(MapAction.SERVERIS);
			cfm.setIp(host);
			cfm.setPort(CommonUtils.WORK_PORT);
			channel.writeAndFlush(cfm.build()); 
			

			
	}catch(Exception e){
		
	}
	}
}
