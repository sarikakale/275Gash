package gash.router.dns.server;

public class IPList {
	private static String hostIP ;
	private final static int hostPort = 5000;
	private final static int clientfacingPort = 4000; 
	public static String getHostIP() {
		return hostIP;
	}

	public static void setHostIP(String hostIP) {
		IPList.hostIP = hostIP;
	}

	public static int getHostport() {
		return hostPort;
	}

	public static int getClientfacingport() {
		return clientfacingPort;
	}

	
	
	
}
