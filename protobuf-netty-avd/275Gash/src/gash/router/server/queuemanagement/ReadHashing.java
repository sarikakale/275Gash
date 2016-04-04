package gash.router.server.queuemanagement;

import java.util.Iterator;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerState;
import gash.router.server.edges.EdgeList;
import gash.router.server.queuemanagement.ReadHashing.RoundRobin;
import pipe.common.Common.Header;

public class ReadHashing {
	protected static Logger logger = LoggerFactory.getLogger("message server");
	public int roundRobin(ServerState state) {
		// TODO Auto-generated method stub
		Set<Integer> set=state.getEmon().getOutBoundEdges().getEdgesMap().keySet();
		RoundRobin robin=new RoundRobin(set);
		int id=robin.next();
		logger.info("IDDDDD  Round Robin====" + id);
		return id;
	}
	class RoundRobin {
		private Iterator<Integer> it;
		private Set<Integer> set;
		  
		public RoundRobin(Set<Integer> set) {
		this.set = set;
		it = set.iterator();
		}
		 
		public int next() {
		// if we get to the end, start again
		if (!it.hasNext()) {
		it = set.iterator();
		}
			 
		return it.next();
		}
		}

	
}
