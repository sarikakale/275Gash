package gash.router.server;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.edges.EdgeInfo;
import pipe.common.Common.Header;
import pipe.election.Election.LeaderStatus;
import pipe.election.Election.LeaderStatus.Action;
import pipe.election.Election.LeaderStatus.LeaderQuery;
import pipe.election.Election.LeaderStatus.LeaderState;
import pipe.election.Election.LeaderStatus.NodeState;
import pipe.work.Work.WorkMessage;

public class CommonUtils {
	protected static Logger logger = LoggerFactory.getLogger("utils");
	final static int RETRY_HB = 3;
	public  static boolean FOUNDNODES = false;
	public final static String SUBNET = "192.168.1.";
	public final static int PORT = 5000;
	public static final String DNS_HOST = "192.168.1.51";// ip of dns server
	public final static int DNS_PORT = 4000; //port   of the dns server
	public final static int WORK_PORT = 3000; //listeneing on work port

	public static RoutingConf conf ;
	public static String getHostInfoById(ServerState state, int nodeId){
		Map<Integer, EdgeInfo> map = state.getEmon().getOutBoundEdges().getEdgesMap();
		
		for(EdgeInfo ei : map.values()){
			if(ei.getRef() == nodeId){
				System.out.println(nodeId + "Hostttt .......");
				return ei.getHost();
			}
		}
		logger.error("getHostInfoById: failed to return the host from id");
		return null;
	}
	
	
	public static WorkMessage createLeaderElectionMessage(ServerState state, int nodeId, LeaderQuery query, 
			Action action, NodeState nState, LeaderState ls, int destId) {
		logger.info("Utils: sending leader election to..1 : "+destId);
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(nodeId);
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(1111111);
		LeaderStatus.Builder lb = LeaderStatus.newBuilder();
		lb.setState(ls);
		lb.setNodeState(nState);

		lb.setQuery(query);
		lb.setAction(action);
		lb.setMsgId(nodeId);
		lb.setOriginNodeId(nodeId);
		lb.setTerm(state.getTerm());
		lb.setPrevTerm(state.getPrevTerm());
		lb.setDestId(destId);
		wb.setLeader(lb);
		return wb.build();
	
	}
	

	public static WorkMessage createLeaderElectionMessage(ServerState state, int nodeId, LeaderQuery query, 
			NodeState nState, LeaderState ls, int destId) {
		System.out.println("Utils : sending leader election to..2:"+destId);
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(nodeId);
		hb.setDestination(-1);
		hb.setTime(System.currentTimeMillis());
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(1111111);
		LeaderStatus.Builder lb = LeaderStatus.newBuilder();
		lb.setState(ls);
		lb.setNodeState(nState);

		lb.setQuery(query);
		if(query.equals(LeaderQuery.THELEADERIS)){
			
		}
		lb.setMsgId(nodeId);
		lb.setOriginNodeId(nodeId);
		lb.setTerm(state.getTerm());
		lb.setPrevTerm(state.getPrevTerm());
		lb.setDestId(destId);
		wb.setLeader(lb);
		return wb.build();
	
	}
	
	public static boolean isReachableByPing(String host) {
	    try{
	            String cmd = "";
	            if(System.getProperty("os.name").startsWith("Windows")) {   
	                    // For Windows
	                    cmd = "ping -n 1 " + host;
	            } else {
	                    // For Linux and OSX
	                    cmd = "ping -c 1 " + host;
	            }

	            Process myProcess = Runtime.getRuntime().exec(cmd);
	            myProcess.waitFor();

	            if(myProcess.exitValue() == 0) {

	                    return true;
	            } else {

	                    return false;
	            }

	    } catch( Exception e ) {

	            e.printStackTrace();
	            return false;
	    }
	} 
	
	public static ArrayList<String> pingAll(){
		ArrayList<String> hostList  = new ArrayList<String>();
				for (int i=50;i<56;i++){
		       String subnet = "192.168.1.";
		       String host = subnet+String.valueOf(i);
		       if(isReachableByPing(host))
		       {	   
		    	   hostList.add(host);
		        System.out.println(host+"is reachable");}
		       else
			   System.out.println(host+String.valueOf(i)+"is not reachable");
		}
		return hostList;
		
	}
	
	
	
	
	//}
	
//	 public static RoutingConf modifyConf(RoutingConf conf){
//		 ArrayList<RoutingEntry> route = new ArrayList<RoutingEntry>();
//		 RoutingEntry e1 = new RoutingEntry(); 
//		 for (int i=51;i<60;i++){
//	 
//	       String host="192.168.1" + ".";
//	       if(isReachableByPing(host+String.valueOf(i)))
//	    	   System.out.println(host+String.valueOf(i)+"is reachable");
//	       e1.setHost();
//			e1.setId(i);
//			e1.setPort(var_port);
//			route.add(e1);
//	       else
//		       System.out.println(host+String.valueOf(i)+"is not reachable");
//	       
//
//	}
//	 }
//	

}
