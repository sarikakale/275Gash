package gash.router.server;

import java.util.Map;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.Action;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.election.Election.LeaderStatus.NodeState;
import pipe.work.Work.WorkMessage;

public class LeaderElectionThread implements Runnable{
	Map<Integer, EdgeInfo> map;
	final public static int minMumberOfNodes = 2;
	ClusterInfo clusterInfo;
	public LeaderElectionThread(ServerState state){
		this.clusterInfo = state.getClusterInfo();
		
	}

	public void onLeaderElectionMsgRecieved(WorkMessage msg, ServerState state) {
		EdgeMonitor emon = state.getEmon();
		System.out.println(RoutingConf.getTimeStamp()+"LEADER MESSAGE RECIEVED from --->" + msg.getHeader().getNodeId());
		LeaderStatus ls = msg.getLeader();
		emon.setDestId(ls.getDestId());
		System.out.println(RoutingConf.getTimeStamp()+"Will set "+state.getConf().getNodeId()+" to"+ls.getState()+"{}{}{}{}{}setting now"+ls.getDestId());
		emon.setLeaderState(ls.getState());
		System.out.println("The leader action recieved:"+ls.getAction()+"Its destination is:"+ ls.getDestId());

		//leader has been made and i wasn't leader.
		if(ls.getState().equals(LeaderState.LEADERALIVE) && ls.getQuery().equals(LeaderQuery.THELEADERIS) && 
				state.getClusterInfo().getLeaderID() < 0){
			announceLeadership(msg, state);
			return;
		}
		else if(ls.getState().equals(LeaderState.LEADERALIVE) && ls.getQuery().equals(LeaderQuery.THELEADERIS) && 
				state.getClusterInfo().getLeaderID() > 0){
			leaderSelected(msg, state, emon);
			return;
		}
		
		//when a node gets the vote.
		else if(state.getConf().getNodeId()== ls.getDestId() && ls.getAction().equals(LeaderStatus.Action.SENDVOTE)){   
			voteRecieved(msg, state, emon, ls);
		}
		else if(ls.getAction().equals(LeaderStatus.Action.REQUESTVOTE) && clusterInfo.getLeaderID()<0){
			voteRequested(state, emon, ls);
		}
		else if(ls.getAction().equals(LeaderStatus.Action.REQUESTVOTE) && clusterInfo.getLeaderID()>0){
			System.out.println("Dude,already set leader..dnt request.");
		}
	}

	private void announceLeadership(WorkMessage msg, ServerState state) {
		int leaderId = msg.getLeader().getLeaderId();
		System.out.println("hurray..................Leader is alive"+leaderId);
		clusterInfo.setLeaderID(leaderId);
		CallClientFacingServer.UpdateLeaderDNSEntry(CommonUtils.getHostInfoById(state, leaderId));
		if(leaderId == state.getConf().getNodeId()){
			state.setLeader(true);
		}
		return;
	}
	public void onLeaderDead(){
		
	}

	private void voteRequested(ServerState state, EdgeMonitor emon, LeaderStatus ls) {
		emon.setLeaderState(LeaderState.LEADERELECTING);
		System.out.println("ls.getDestId() was"+ls.getDestId());
		
			System.out.println(RoutingConf.getTimeStamp()+"OOPS.SOMEONE ELSE REQUESTED MY VOTE, i will become follower then");
			state.nodeState = NodeState.FOLLOWER_STATE;
			WorkMessage wm = CommonUtils.createLeaderElectionMessage(state, state.getConf().getNodeId(),LeaderQuery.WHOISTHELEADER,
			Action.SENDVOTE,NodeState.FOLLOWER_STATE, LeaderState.LEADERELECTING, ls.getOriginNodeId());
			System.out.println("voting now..");

			for (EdgeInfo ei : emon.getOutBoundEdges().getEdgesMap().values()) {
				//send only to destination channel
				System.out.println("LET: writing to channel of "+ei.getRef()+" channel is"+ei.getChannel());
				if (ei.getRef() == ls.getOriginNodeId()) {
					if(ei.getChannel()!=null){
						System.out.println("LET: writing to channel of "+ei.getRef()+" channel is"+ei.getChannel());
						ei.getChannel().writeAndFlush(wm);
					}
					
				}
			}
	}

	private void voteRecieved(WorkMessage msg, ServerState state, EdgeMonitor emon, LeaderStatus ls) {
		System.out.println(" :"+ls.getTerm());
		//increment the vote for the particular term.
		int vote = state.getTermVotesMap(ls.getTerm());
		vote++;
		System.out.println(RoutingConf.getTimeStamp()+"Recieved vote!my vote count is***************************************** "+vote);
		state.setTermVotesMap(ls.getTerm(), vote);
		emon.setLeaderState(LeaderState.LEADERELECTING);
		//if voted by more than half of the nodes, announce the leader.
		if(vote >= EdgeList.getTotalNodes()/2){
			leaderSelected(msg, state, emon);
		}
	}

	private void leaderSelected(WorkMessage msg, ServerState state, EdgeMonitor emon) {
		
		System.out.println("yea, leader selected guys!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+state.getConf().getNodeId());
		emon.setLeaderState(LeaderState.LEADERALIVE);
		state.setLeader(true);
		state.setPrevTerm(state.getTerm());
		state.setTerm(state.getTerm()+1);
		int nodeId = state.getConf().getNodeId();
		clusterInfo.setLeaderID(nodeId);
		CallClientFacingServer.UpdateLeaderDNSEntry(CommonUtils.getHostInfoById(state, nodeId));

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(1111111);
		LeaderStatus.Builder lb = LeaderStatus.newBuilder();
		lb.setState(LeaderState.LEADERALIVE);

		lb.setQuery(LeaderQuery.THELEADERIS);
		lb.setOriginNodeId(nodeId);
		lb.setLeaderId(nodeId);
		lb.setMsgId(nodeId);
		lb.setTerm(state.getTerm());
		lb.setPrevTerm(state.getPrevTerm());
		
		for (EdgeInfo ei : emon.getOutBoundEdges().getEdgesMap().values()) {
			//send only to destination channel
			System.out.println("LET: writing to channel of "+ei.getRef()+" channel is"+ei.getChannel());
			//announce to all OTHER NODES
			if (ei.getRef()!= nodeId) {
				System.out.println("LET: Announcing about Elected Leader %%%%%%%%%%%"+ei.getRef()+" channel is"+ei.getChannel());
				wb.setLeader(lb);
				if(ei.getChannel() != null)
				ei.getChannel().writeAndFlush(wb.build());
				
				}	
		}
		
	}
	
		public void triggerLeaderElec(EdgeInfo ei, ServerState state, int destId) {
		WorkMessage wm;
		Long startTime;
		if(state.getClusterInfo().getLeaderID() >0)
			return;
		startTime = System.currentTimeMillis();
		while(true){
			if(state.getEmon().getLeaderState().equals(LeaderState.LEADERALIVE)){
				System.out.println("NOT UNKNOWN/DEAD Leader State...."+state.getEmon().getLeaderState());
				break;
			}
			
			if(System.currentTimeMillis() - startTime >=EdgeMonitor.DT){
				state.nodeState = NodeState.CANDIDATE_STATE;
				System.out.println(RoutingConf.getTimeStamp()+"I am candidate!!!!!!!!!!!!");
				break;
			}
			
		}
		//since candidate, requesting vote.
		if(state.nodeState.equals(NodeState.CANDIDATE_STATE)){
			System.out.println(RoutingConf.getTimeStamp()+"PLease vote for me...:"+state.getConf().getNodeId());
			wm = CommonUtils.createLeaderElectionMessage(state, state.getConf().getNodeId(),LeaderQuery.WHOISTHELEADER,
				Action.REQUESTVOTE,NodeState.CANDIDATE_STATE, LeaderState.LEADERELECTING, state.getConf().getNodeId());
			if(ei.getChannel() != null)
				ei.getChannel().writeAndFlush(wm);
			
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
	
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%Leader election started..1");
	}	

}
