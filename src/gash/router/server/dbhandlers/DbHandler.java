package gash.router.server.dbhandlers;

import java.util.Map;

public interface DbHandler {

	void addEntry(String key, String value);

	long encryptKey(String key);

	void insertData(String key, int sequenceId, String value);
	
	public Map< String, String> retrieveData(String key);
	
	public Map<String, String> removeData(String key);

}
