package gash.router.server;

import java.net.UnknownHostException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.dbhandlers.DbHandler;
import gash.router.server.dbhandlers.Mongo;
import gash.router.server.dbhandlers.RedisHandler;
import gash.router.server.tasks.TaskList;
import io.netty.channel.Channel;
import pipe.work.Work.Task;
import pipe.work.Work.WorkMessage;
import routing.Pipe;
import routing.Pipe.CommandMessage;

public class DataProcessingThread implements Runnable {
	protected static Logger logger = LoggerFactory.getLogger("Data processing");
	private TaskList taskList;
	private boolean forever = true;
	private static int i = 0;
	private static int counter = 0;
	private static int msgId = 0;
	private DbHandler handler = new RedisHandler();
	private ServerState state;

	public DataProcessingThread(TaskList taskList, ServerState state) {
		this.taskList = taskList;
		this.state = state;
	}

	@Override
	public void run() {

		logger.info("DATA PROCESSING************");
		while (true) {

			if (!forever && taskList.numEnqueued() == 0) {
				break;
			}

			if (taskList != null && taskList.numEnqueued() != 0) {

				pullTasksFromQueue(taskList);
			}

		}
	}

	public void pullTasksFromQueue(TaskList taskList) {

		logger.info("PULLING TASKS ");
		logger.info("TASKLIST+++++++++" + taskList.numEnqueued());
		Task task = taskList.dequeue();

		CommandMessage msg = task.getMessage();

		if ((msg.getAction().equals(Pipe.Action.READ))) {

			processData(msg);
		} else {

			if ((msg.getAction().equals(Pipe.Action.UPLOAD)) && msg.getChunk().getNumberOfChunks() == 1) {
				processData(msg);
			} else if ((!ChunkTracker.checkMissing(msg.getMessageId())) && (msg.getAction().equals(Pipe.Action.UPLOAD))
					&& msg.getChunk().getNumberOfChunks() != 1) {

				processData(msg);

			} else {
				
					rolllback(msg);
			}
		}
	}

	public boolean verfiyData(CommandMessage msg) {

		if (ChunkTracker.checkMissing(msg.getMessageId())) {
			return false;
		} else
			return true;

	}

	public void rolllback(CommandMessage msg) {

		// get all message chunks from the db of same msg id and delete them

		logger.error("All chunks not received**&*&&******&*&*&*&*&*&&*&*&*&*&*&*&*&*&*& ROLLBACK");
		String key = msg.getFilename();
		handler.removeData(key);
		
	}

	public synchronized void processData(CommandMessage msg) {

		try {
			Mongo m = new Mongo();
			
			if (msg != null) {
				msg = msg;
			} else {

				System.out.println("No Task Defined");
				return;
			}
			if (msg != null) {
				if (msg.hasUploadData() && msg.getAction().equals(Pipe.Action.UPLOAD)) {

					// Redis insertion
					String key = msg.getFilename();

					handler.insertData(key, msg.getChunk().getChunkId(), msg.getUploadData().toString());

					if (state.isLeader) {
						MessageServer.StartWorkCommunication.getInstance().dataReplicate(msg);
					}

					// Mongo Insertion

					try {

						m.insertData(key, msg.getChunk().getChunkId(), msg.getUploadData().toString());

					}

					catch (Exception e) {
						throw e;
					} finally {
						m.closeConnection();
					}

				} else {
					if (msg.getAction().equals(Pipe.Action.READ)) {
						System.out.println("Reading");
						System.out.println("File name: " + msg.getFilename());

						// Jedis Read

						Map<String, String> dataMap;
						try {
							String key = msg.getFilename();
							
							dataMap = handler.retrieveData(key);
							
							Map<String, String> dataMongo = m.retrieveData(key);
							if(dataMap==null){
								sendToClient(dataMongo, msg);
							}
							sendToClient(dataMap, msg);

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {

							m.closeConnection();
						}

					}
				}
			} else {
				System.out.println("No Message To serve");
				return;
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendToClient(Map<String, String> dataMap, CommandMessage msg) {
		logger.info("SEND TO THE CLIENT BACK" + dataMap.size());
		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			System.out.println("Key " + dataMap.get(entry.getKey()));
			System.out.println("Value " + dataMap.get(entry.getValue()));
			Channel ch = state.getReadCh();
			WorkMessage wm = state.getEmon().createTaskMessage(msg, entry, dataMap.size());
			ch.writeAndFlush(wm);
		}

	}

}
