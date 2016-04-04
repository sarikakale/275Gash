package gash.router.client;

import java.util.concurrent.atomic.AtomicReference;

import ClientFacingMapping.Mapping.ClientFacingMessage;
import ClientFacingMapping.Mapping.MapAction;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import routing.Pipe.CommandMessage;


public  class RequestClientFacingServer {
	private  EventLoopGroup group;
	private  String host = "192.168.1.51";// ip of dns server
	private  int port = 4000; //port of the dns server
	private  ChannelFuture channel;
	protected static AtomicReference<RequestClientFacingServer> instance = new AtomicReference<RequestClientFacingServer>();

	public  RequestClientFacingServer() {
		super();
		System.out.println("in req client server");
		init();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//write();
		
		
	}
	public static RequestClientFacingServer initConnection() {
		instance.compareAndSet(null, new RequestClientFacingServer());
	//	CommandMessage msg = conn.outbound.take();
		return instance.get();
	}
	public  void init(){
		group = new NioEventLoopGroup();
		try{
		MappingInit si = new MappingInit( false);
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(si);
		b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		System.out.println("in init my");

		// Make the connection attempt.
		channel = b.connect(host, port).syncUninterruptibly();
		ClientFacingMessage.Builder cfm =  ClientFacingMessage.newBuilder();
		System.out.println("in init cfm");
		cfm.setMapaction(MapAction.WHICHSERVER);
		channel.channel().writeAndFlush(cfm.build());

		// want to monitor the connection to the server s.t. if we loose the
		// connection, we can try to re-establish it.
		ClientClosedListener ccl = new ClientClosedListener(this);
		channel.channel().closeFuture().addListener(ccl);

			
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public boolean write() {
//		if (channel == null)
//			throw new RuntimeException("missing channel");
		ClientFacingMessage.Builder cfm =  ClientFacingMessage.newBuilder();
		cfm.setMapaction(MapAction.WHICHSERVER);
		

		// TODO a queue is needed to prevent overloading of the socket
		// connection. For the demonstration, we don't need it
		ChannelFuture cf = connect().writeAndFlush(cfm.build());
		if (cf.isDone() && !cf.isSuccess()) {
			
			return false;
		}

		return true;
		
	}
	public static class ClientClosedListener implements ChannelFutureListener {
		RequestClientFacingServer rcfs;

		public ClientClosedListener(RequestClientFacingServer rcfs) {
			this.rcfs = rcfs;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			System.out.println("--> client lost connection to the server");
			System.out.flush();

			// @TODO if lost, try to re-establish the connection
		}
	}
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			init();
		}

		if (channel != null && channel.isSuccess() && channel.channel().isWritable())
			return channel.channel();
		else
			throw new RuntimeException("Not able to establish connection to server");
	}


}
