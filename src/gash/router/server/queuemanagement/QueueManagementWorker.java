package gash.router.server.queuemanagement;

import java.io.File;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import gash.router.server.ServerState;

public class QueueManagementWorker implements Runnable {

	ServerState state;

	public QueueManagementWorker(ServerState state) {
		this.state = state;
	}

			//check own queue
			//if it is less then ask for tasks to the leader
			//if leader has more tasks, it forwards them to the requesting node
			//if the leader has less tasks, it will check heart beat of every node for their queue size
			//A queue with more entries in the queue will pass on its tasks to the requesting node	
	
	public double calcCPULoad() {
		
		final int AVGSIZE=5;
		
		/*String path="F:/hyperic-sigar-1.6.4/hyperic-sigar-1.6.4/sigar-bin/lib";
		String libName="sigar-amd64-winnt.dll";
		String loadlibname = System.mapLibraryName(libName); 

		// Load the library via its absolute path
		System.load(new File(path, loadlibname).getAbsolutePath());
		*/
		System.loadLibrary("sigar-amd64-winnt");
		CpuPerc cpuperc = null;
		Sigar sigar=new Sigar();
		double loadArray[]=new double[AVGSIZE];
		double sum=0;
		try {
			
			for(int i=0;i<AVGSIZE;i++){
			cpuperc=sigar.getCpuPerc();
			double load=cpuperc.getCombined()*100;
			System.out.println("CpU PER CYCLE========="+load);
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loadArray[i]=cpuperc.getCombined();
			sum=sum+loadArray[i];
			}
			
			
			sum=sum/AVGSIZE;
			
			return sum;
			
			
		} catch (SigarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sum;

	}
		
	
		
		//System.out.println("Within Queue Management");
		/*if ((state.getTasks().numEnqueued() <= 5 )) {//|| s.getProcessed() < 2

			System.out.println("I have less tasks"); 
			// check its own queue

			if (state.getEmon().isQueueLessThanThreshold()) {
				// insert into its own queue
				ei.getChannel().writeAndFlush("kkkj");
			} else {
				if (state.isLeaderStatus()) {
					// send a heartbeat to everybody
					state.getEmon().setEnqueuedTask(state.getEmon().getEnqueuedTask());
					state.getEmon().setProcessedTask(state.getEmon().getEnqueuedTask());
				}
			}
		}else{
			System.out.println("I have more tasks");
		}*/






	@Override
	public void run() {
		//System.out.println("Im in queue Management");
		
	}
	
	
}
