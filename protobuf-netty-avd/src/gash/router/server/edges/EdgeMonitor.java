/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server.edges;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import gash.router.client.CommInit;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.ClusterInfo;
import gash.router.server.LeaderElectionThread;
import gash.router.server.ServerState;
import gash.router.server.CommonUtils;
import gash.router.server.WorkHandler;
import gash.router.server.WorkInit;
import gash.router.server.queuemanagement.QueueManagementWorker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.Task;
import pipe.work.Work.TaskAction;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import routing.Pipe;
import routing.Pipe.Chunk;
import routing.Pipe.CommandMessage;
import routing.Pipe.ReadResponse;

public class EdgeMonitor implements EdgeListener, Runnable {
	protected static Logger logger = LoggerFactory.getLogger("edge monitor");

	private EdgeList outboundEdges;
	private EdgeList inboundEdges;
	public static long DT = 2000;
	private ServerState state;
	private boolean forever = true;
	public LeaderState ls = LeaderState.LEADERUNKNOWN;
	public int destId;
	final private int RETRY_ATTEMPTS = 3;
	private LeaderElectionThread leaderElec;
	private int enqueuedTask;
	private int processedTask;
	private QueueManagementWorker queueManagementWorker = new QueueManagementWorker(state);
	private Map<Integer, Object> consistentMap;
	private String var_host = "192.168.1.";
	private int var_port = 5000;
	private EventLoopGroup group;
	private ChannelFuture channelfuture;
	private Channel channel;
	private String IP;

	InetAddress[] localaddr;
	private String var_host_broadcast = "192.168.1.255";

	// TODO parmj : may not need this..
	public boolean oneTime = false; // to execute once only

	public EdgeList getOutBoundEdges() {
		return outboundEdges;
	}

	public synchronized void setLeaderState(LeaderState lstate) {
		this.ls = lstate;
	}

	public synchronized LeaderState getLeaderState() {
		return ls;
	}

	public LeaderElectionThread geLeaderElectionThread() {
		return leaderElec;
	}

	public synchronized void setDestId(int s_destId) {
		this.destId = s_destId;
	}

	public synchronized int getDestId() {
		return destId;
	}

	private Bootstrap discoverNode(ServerState state, int i) {
		Bootstrap b = new Bootstrap();
		if (i == (state.getConf().getNodeId()))
			return b;
		b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel arg0) throws Exception {
						// TODO Auto-generated method stub
					}
				}).connect(CommonUtils.SUBNET + i, 5000).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (!future.isSuccess()) {
							return;
						}
						try {
							Channel c = future.channel();
							System.out.println("channel formed");
							InetSocketAddress addr = (InetSocketAddress) c.remoteAddress();
							System.out.println("inet socket");
							String address = addr.getAddress().getHostAddress();
							EdgeInfo ei = outboundEdges.createIfNew(Integer.parseInt(address.split("\\.")[3]), address,
									addr.getPort());

							if (ei.getChannel() == null) {
								ei.setChannel(c);
								ei.setActive(true);
								System.out.println("Successfully connected to " + addr.getAddress().getHostAddress());
							}
						} catch (RuntimeException e) {
							System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj");
							e.printStackTrace();
						}
					}
				});
		return b;
	}

	public EdgeMonitor(ServerState state) {

		if (state == null)
			throw new RuntimeException("state is null");
		this.outboundEdges = new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);
		this.consistentMap = new HashMap<>();
		leaderElec = new LeaderElectionThread(state);

		if (state.getConf().getRouting() != null) {
			for (RoutingEntry e : state.getConf().getRouting()) {
				outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
			}
		}
		// //populate inboundEdge entry too
		// createInboundIfNew(e.getId(), e.getHost(), e.getPort());
		// }
		// }
		/*
		 * for (int i = 1; i <= 100; i++) { Bootstrap b = discoverNode(state,
		 * i); }
		 */

		// ArrayList<RoutingEntry> route = new ArrayList<RoutingEntry>();
		//
		// RoutingEntry e1 = new RoutingEntry();
		// boolean flag_routing_exists = false;
		//
		// for (RoutingEntry e : state.getConf().getRouting()) {
		//
		// flag_routing_exists = true;
		//
		// outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
		//
		// }
		//
		// if (!flag_routing_exists) {
		//
		// // System.out.println("Inside no routing ");
		//
		// for (int i = 49; i < 100; i++) {
		//
		// if (i == 7)
		// continue;
		//
		// outboundEdges.addNode(i, var_host + i, var_port);
		//
		// e1.setHost(var_host + i);
		//
		// e1.setId(i);
		//
		// e1.setPort(var_port);
		//
		// route.add(e1);
		//
		// }
		//
		// for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {
		//
		// ei.newChannel();
		//
		// }
		//
		// state.getConf().setRouting(route);

		// }

		LeaderElectionThread ldrElecThread = new LeaderElectionThread(state);
		// cannot go below 2 sec
		if (state.getConf().getHeartbeatDt() > this.DT)
			this.DT = state.getConf().getHeartbeatDt();

	}

	public void createInboundIfNew(int ref, String host, int port) {
		inboundEdges.createIfNew(ref, host, port);
	}

	
	
	public void createOutboundIfNew(int ref, Channel channel) {
		//state.getConf().updateRouting(Utils.SUBNET+ref, Utils.PORT, ref);  
		System.out.println("In outbound "+channel); 
		outboundEdges.createIfNew(ref, CommonUtils.SUBNET+ref, CommonUtils.PORT);
		outboundEdges.getNode(ref).setChannel(channel);	
		}
	// private WorkMessage createLeaderElectionMessage(int nodeId, LeaderQuery
	// query, Action action, NodeState nState, LeaderState ls, int destId) {
	//
	// System.out.println("EM : Sending Elec msg to....."+destId);
	// Header.Builder hb = Header.newBuilder();
	// hb.setNodeId(state.getConf().getNodeId());
	// hb.setDestination(-1);
	// hb.setTime(System.currentTimeMillis());
	//
	// WorkMessage.Builder wb = WorkMessage.newBuilder();
	// wb.setHeader(hb);
	// wb.setSecret(1111111);
	// LeaderStatus.Builder lb = LeaderStatus.newBuilder();
	// lb.setState(ls);
	// lb.setNodeState(nState);
	//
	// lb.setQuery(query);
	// lb.setAction(action);
	// lb.setMsgId(nodeId);
	// lb.setTerm(1);
	// lb.setPrevTerm(0);
	// lb.setDestId(destId);
	// wb.setLeader(lb);
	// return wb.build();
	//
	// }

	private WorkMessage createHB(EdgeInfo ei) {
		WorkState.Builder sb = WorkState.newBuilder();
		sb.setEnqueued(getEnqueuedTasks());
		sb.setProcessed(getEnqueuedTasks());
		sb.setCpuLoad(queueManagementWorker.calcCPULoad());

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);
		bb.setCapacity(1024);
		bb.setTotalNodes(outboundEdges.getTotalNodes());

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setBeat(bb);
		wb.setSecret(1111111);
		return wb.build();

	}

	public Map<Integer, Object> getConsistentMap() {
		return consistentMap;
	}

	public void setConsistentMap(Map<Integer, Object> consistentMap) {
		this.consistentMap = consistentMap;
	}

	public void shutdown() {
		forever = false;
	}

	@Override
	public void run() {

		ClusterInfo clusterInfo = state.getClusterInfo();
		TimerTask timerTask = new MonitorHBTask();
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 6 * 1000);
		while (forever) {
			logger.info("WITHIN EDGE MONITOR +++++" + this.outboundEdges.getEdgesMap().size());
			try {
				for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {

					if (ei.getChannel() == null) {
						ei.setChannel(newChannel(ei.getHost(), ei.getPort(), true));
						ei.setActive(true);
						this.outboundEdges.getEdgesMap().put(ei.getRef(), ei);
						state.setEmon(this);
						sendHBToLeader(clusterInfo, ei);
					} else if (ei.isActive() && ei.getChannel() != null) {
						sendHBToLeader(clusterInfo, ei);
						if (outboundEdges.getTotalNodes() >= LeaderElectionThread.minMumberOfNodes
								&& getLeaderState().equals(LeaderStatus.LeaderState.LEADERUNKNOWN)
								&& state.getClusterInfo().getLeaderID() < 0) {
							leaderElec.triggerLeaderElec(ei, state, destId);
						} else if (outboundEdges.getTotalNodes() >= LeaderElectionThread.minMumberOfNodes
								&& getLeaderState().equals(LeaderStatus.LeaderState.LEADERDEAD)) {
							// leaderElec.triggerLeaderDeadRaft(ei, state,
							// destId);
						}
					}
					/*
					 * else logger.info(""); // Thread.sleep(200);
					 */ }
				Thread.sleep(DT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void monitorHB() {
		// if slave node, monitor the leader
		if (!state.isLeader) {
			if (state.getClusterInfo().getLeaderID() > -1
					&& WorkHandler.ctr[state.getClusterInfo().getLeaderID()] > 0) {
				System.out.println(WorkHandler.ctr[state.getClusterInfo().getLeaderID()]
						+ "cool....got heartbeat from leader>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
						+ state.getClusterInfo().getLeaderID());
				WorkHandler.ctr[state.getClusterInfo().getLeaderID()]--;
			} else if (outboundEdges.getTotalNodes() > LeaderElectionThread.minMumberOfNodes
					&& !state.getEmon().getLeaderState().equals(LeaderState.LEADERELECTING)) {
				state.getClusterInfo().setLeaderID(-1);
				state.getEmon().setLeaderState(LeaderState.LEADERDEAD);
				state.setPrevTerm(state.getTerm());
				state.setTerm(state.getTerm() + 1);
				System.out.println(
						"panic attack..destId.panic attack .panic attack .panic attack.panic attack.panic attack");
				for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {

					if (outboundEdges.getTotalNodes() >= LeaderElectionThread.minMumberOfNodes) {
						leaderElec.triggerLeaderElec(ei, state, destId);
					}
				}

			}
		}
		// if master nodes, monitor all the nodes and update ClusterInfo
		else {
			int nodesAliveCount = 0;
			for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {
				int id = ei.getRef();
				if (id != state.getConf().getNodeId()) {
					if (id > -1 && WorkHandler.ctr[id] > 0) {
						System.out.println(WorkHandler.ctr[id]
								+ ":cool....got heartbeat from slaves>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + id);
						WorkHandler.ctr[id]--;
						nodesAliveCount++;
						state.getClusterInfo().setAliveNodeInfo(id);
					} else {
						System.out.println("time out bro of slaves..........................>>>>>>>>>>>>>>>>>>>>>>>>>");
						state.getClusterInfo().getAliveNodeInfo().remove(id);
					}
				}
			}
			state.getClusterInfo().setNodesAliveCount(nodesAliveCount);
		}
	}

	private void sendHBToLeader(ClusterInfo clusterInfo, EdgeInfo ei) {
		System.out.println("sending HB..........................................................." + state.isLeader());
		// if (state.isLeader()) {
		WorkMessage wm = createHB(ei);
		Channel chan = ei.getChannel();
		if (ei.getChannel() != null) {
			System.out.println("to child nodes..&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&7");
			chan.writeAndFlush(wm);
		} else
			System.out.println(
					"to child nodes..&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& nulllllllllllllllllll");

		// } else
		if (ei.getRef() == clusterInfo.getLeaderID()) {
			System.out.println("will try to send HB to leader" + clusterInfo.getLeaderID());
			WorkMessage wm1 = createHB(ei);
			ei.getChannel().writeAndFlush(wm1);
		}

		// return;
	}

	public Channel newChannel(String host, int port, boolean addingNode) {
		ChannelFuture channel;
		Channel chan = null;
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			System.out.println("Creating new channel for " + host);
			WorkInit si = new WorkInit(state, false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();
			chan = channel.channel();
			System.out.println("LOGGERRRRR" + chan);
			/*
			 * setChannel(chan); setActive(chan.isActive());
			 */
			// want to monitor the connection to the server s.t. if we loose the
			// connection, we can try to re-establish it.
			// ClientClosedListener ccl = new ClientClosedListener(this);
			// channel.channel().closeFuture();
			if (chan.isOpen() && addingNode) {
				outboundEdges.setTotalNodes(outboundEdges.getTotalNodes() + 1);
				state.getClusterInfo().setNumberOfNodes(outboundEdges.getTotalNodes() + 1);
				System.out.println(outboundEdges.getTotalNodes() + "$$$$$$$$$$$$$$");
			}

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());
			return chan;

		} catch (Throwable ex) {
			logger.error("failed to initialize the client connection");
		}
		return chan;
	}

	public Channel newChannel(String host, int port) {
		ChannelFuture channel;
		Channel chan = null;
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			System.out.println("Creating new channel for " + host);
			CommInit si = new CommInit(false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(host, port).syncUninterruptibly();
			chan = channel.channel();
			System.out.println("LOGGERRRRR" + chan);
			/*
			 * setChannel(chan); setActive(chan.isActive());
			 */
			// want to monitor the connection to the server s.t. if we loose the
			// connection, we can try to re-establish it.
			// ClientClosedListener ccl = new ClientClosedListener(this);
			// channel.channel().closeFuture();

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());
			return chan;

		} catch (Throwable ex) {
			logger.error("failed to initialize the client connection");
		}
		return chan;
	}

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// TODO check connection
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// TODO ?
	}

	public class MonitorHBTask extends TimerTask {
		@Override
		public void run() {

			monitorHB();
		}
	}

	public int getEnqueuedTasks() {
		enqueuedTask = state.getTasks().numEnqueued();
		return enqueuedTask;
	}

	public int getprocessesTask() {
		processedTask = state.getTasks().numProcessed();
		return processedTask;
	}

	public boolean isQueueLessThanThreshold() {

		if (state.getTasks().numEnqueued() == 0 || state.getTasks().numEnqueued() <= 5) {
			return true;
		} else
			return false;
	}

	public WorkMessage createTaskMessage(CommandMessage cm) {

		Task.Builder tm = Task.newBuilder();
		// Task t = state.getTasks().peek();
		tm.setSeqId(1236);
		tm.setSeriesId(1234578);
		tm.setMessage(cm);

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setTask(tm);
		wb.setSecret(123);
		return wb.build();

	}

	public void queueManagement(ServerState s, EdgeInfo ei) {
		QueueManagementWorker queueManagement = new QueueManagementWorker(state);
		Thread thread = new Thread(queueManagement);
		thread.setDaemon(true);
		thread.start();
		// queueManagement.queueManagement(s);
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
		}

	}

	/*
	 * 0 * public WorkMessage requestToSendTasks() {
	 * 
	 * return state.getEmon().createTaskMessage(); }
	 */

	public synchronized void sendTaskMessages(WorkMessage wm) {
		logger.error("Send  Message$$$$$$$$$$$$$" + state.getEmon().getOutBoundEdges().getEdgesMap().size());
		for (EdgeInfo ei : this.outboundEdges.getEdgesMap().values()) {
			try {

				if (ei.getChannel() == null) {
					try {
						ei.setChannel(newChannel(ei.getHost(), ei.getPort(), false));
						ei.setActive(true);

					} catch (Exception e) { // TODO Auto-generated catch block
						logger.info("trying to connect to node " + ei.getRef());
					}

				}

				System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII"
						+ state.getEmon().getOutBoundEdges().getEdgesMap().size());
				if (ei.isActive() && ei.getChannel() != null) {
					System.out.println(ei.getHost());
					System.out.println("HIIIIIIIIIIIIBYYYEYYYYIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
					ei.getChannel().writeAndFlush(wm);

				} else {
					// TODO create a client to the node

					logger.info("trying to connect to node " + ei.getRef());

				}

				// Thread.sleep(dt);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	protected class RetryCount {
		Map<EdgeInfo, Integer> map = new HashMap<EdgeInfo, Integer>();

		public void reduceAttempts(EdgeInfo ei) {
			System.out.println("reduceAttempts reduceAttempts reduceAttempts");
			if (map.get(ei) != null) {
				int curAttemptRemaining = map.get(ei);
				curAttemptRemaining--;
				map.put(ei, curAttemptRemaining);
			} else
				map.put(ei, RETRY_ATTEMPTS);
		}

		public void resetAttempts(EdgeInfo ei) {
			System.out.println("resetAttempts resetAttempts resetAttempts " + ei.getRef());
			map.put(ei, RETRY_ATTEMPTS);
		}

		public boolean isNumOfAttemptsZero(EdgeInfo ei) {
			System.out.println("isNumOfAttemptsZero  isNumOfAttemptsZero  isNumOfAttemptsZero");
			if (map.get(ei) == 0)
				return true;
			else
				return false;
		}

	}

	public CommandMessage createReadMessage(CommandMessage msg, Entry<String, String> entry, int size) {
		CommandMessage.Builder cmsg = CommandMessage.newBuilder(msg);
		Header.Builder header = Header.newBuilder();
		header.setNodeId(msg.getHeader().getNodeId());
		header.setDestination(msg.getHeader().getNodeId());
		header.setTime(System.currentTimeMillis());
		cmsg.setHeader(header);

		cmsg.setAction(Pipe.Action.READ);
		cmsg.setFilename(msg.getFilename());
		ReadResponse.Builder rd = ReadResponse.newBuilder();
		rd.setReadData(ByteString.copyFrom(entry.getValue().getBytes()));
		cmsg.setReadResponse(rd);
		Chunk.Builder chunk = Chunk.newBuilder();
		chunk.setChunkId(Integer.parseInt(entry.getKey()));
		chunk.setChunkSize(entry.getValue().length());
		chunk.setNumberOfChunks(size);
		cmsg.setChunk(chunk);
		return cmsg.build();
	}

	public WorkMessage createTaskMessage(CommandMessage cm, Entry<String, String> entry, int size) {
		CommandMessage cmsg = createReadMessage(cm, entry, size);

		Task.Builder tm = Task.newBuilder();
		// Task t = state.getTasks().peek();
		tm.setSeqId(1236);
		tm.setSeriesId(1234578);
		tm.setMessage(cmsg);
		tm.setTaskAction(TaskAction.RESPONSE);
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setTask(tm);
		wb.setSecret(123);
		return wb.build();

	}
}
