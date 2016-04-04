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
package gash.router.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import gash.router.app.ImageConversion;
import pipe.common.Common.Header;
import routing.Pipe.Chunk;
import routing.Pipe.CommandMessage;
import routing.Pipe.ReadResponse;

/**
 * front-end (proxy) to our service - functional-based
 * 
 * @author gash
 * 
 */
public class MessageClient {
	// track requests
	private long curID = 0;
	protected static Logger logger = LoggerFactory.getLogger("connect");

	public MessageClient(String host, int port) {
		init(host, port);
	}

	private void init(String host, int port) {
		CommConnection.initConnection(host, port);
			}

	public void addListener(CommListener listener) {
		CommConnection.getInstance().addListener(listener);
	}

	public void ping() {
		// construct the message to send
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(999);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setPing(true);

		try {
			// direct no queue
			// CommConnection.getInstance().write(rb.build());

			// using queue
			CommConnection.getInstance().enqueue(rb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	public void upload(String filepath) {
		// divide the data into chunks and put into the queue
		
		try {
			File file = new File(filepath);
			Filechunk fileChunk = new Filechunk();
			String builder1 = new String();
			int i = 0;
			ImageConversion imageConversion = new ImageConversion();

			byte imageData[] = null;
			/*
			 * FileInputStream imageInFile = new FileInputStream(file); long
			 * leng=file.length(); System.out.println(leng); if(leng <
			 * 2147483647){ System.out.println(leng); imageData = new byte[
			 * (int) file.length()]; imageInFile.read(imageData);
			 * imageInFile.close(); }
			 */
			// ByteString byteStr = ByteString.copyFrom(imageData);

			// Constructing the message to be sent
			/*
			 * Header.Builder hb = Header.newBuilder(); hb.setNodeId(999);
			 * hb.setDestination(-1); hb.setTime(System.currentTimeMillis());
			 * 
			 * ChunkedFile chunkedFile=new ChunkedFile(file );
			 * 
			 * CommandMessage.Builder rb = CommandMessage.newBuilder();
			 * rb.setHeader(hb); rb.setAction(routing.Pipe.Action.UPLOAD);
			 * rb.setUploadData(byteStr); rb.setFilename(filename);
			 * rb.setTotalSize(file.length()); rb.setPort(5000); Chunk.Builder
			 * chunk = Chunk.newBuilder(); chunk.setChunkId(i++);
			 * chunk.setChunkSize(byteStr.size()); chunk.setNumberOfChunks(1);
			 * rb.setChunk(chunk);
			 * rb.setHost(InetAddress.getLocalHost().getHostAddress());
			 * rb.setMessageId(12345);
			 * 
			 * 
			 * ChannelFuture ch=
			 * ClientConnection.getInstance().connect().writeAndFlush(rb.build()
			 * );
			 */

			Header.Builder hb = Header.newBuilder();
			hb.setNodeId(999);
			hb.setDestination(-1);
			hb.setTime(System.currentTimeMillis());
			byte byteArray[] = imageConversion.imageConvert(filepath);
			
			String[] bits = filepath.split("/");
			String filename = bits[bits.length-1];
			
			
			if (byteArray == null) {
				logger.error("File exceeds 2 GB");
				return;
			} else {

				ByteString byteStr = ByteString.copyFrom(byteArray);

				if (byteStr != null) {
					if (byteStr.size() < 60000000) {

						CommandMessage.Builder rb = CommandMessage.newBuilder();
						rb.setHeader(hb);
						rb.setAction(routing.Pipe.Action.UPLOAD);
						rb.setUploadData(byteStr);
						rb.setFilename(filename);
						rb.setTotalSize(file.length());
						rb.setPort(5000);
						Chunk.Builder chunk = Chunk.newBuilder();
						chunk.setChunkId(i++);
						chunk.setChunkSize(byteStr.size());
						chunk.setNumberOfChunks(1);
						rb.setChunk(chunk);
						rb.setHost(InetAddress.getLocalHost().getHostAddress());
						rb.setMessageId(12345);

						CommConnection.getInstance().enqueue(rb.build());
					} else {
						ArrayList<byte[]> bytearray = fileChunk.fileChunking(filepath);

						if (bytearray.size() != 0) {
							for (int k = 0; k < bytearray.size(); k++) {

								ByteString bs = ByteString.copyFrom(bytearray.get(k));
								if (bs != null) {

									CommandMessage.Builder rb = CommandMessage.newBuilder();
									rb.setHeader(hb);
									rb.setAction(routing.Pipe.Action.UPLOAD);
									rb.setUploadData(bs);
									rb.setFilename(filename);
									rb.setTotalSize(file.length());
									rb.setPort(5000);
									Chunk.Builder chunk = Chunk.newBuilder();
									chunk.setChunkId(i++);
									chunk.setChunkSize(bs.size());
									chunk.setNumberOfChunks(bytearray.size() + 1);
									System.out.println("CHUNK NUMBER=========" + bytearray.size());
									rb.setChunk(chunk);
									rb.setHost(InetAddress.getLocalHost().getHostAddress());
									rb.setMessageId(12345);
									if (k % 5 == 0)
										Thread.sleep(1000);
									CommConnection.getInstance().enqueue(rb.build());
								} else {
									System.out.println("No byte String obtained");
									return;
								}
							}
						} else {
							System.out.println("No byteArray Received ");
							return;
						}

					}
				} else {
					System.out.println("No byte String obtained");
					return;
				}

				// change the name of the file
				// builder = imageConversion.imageConvert(filename);
				// } // ByteString byteMessage=ByteString.copyFrom(builder);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to open file '" + filepath + "'");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error reading file '" + filepath + "'");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void read(String filename) {
		// obtain the data from the servers
		ImageConversion imageConversion = new ImageConversion();
		try {
			//ByteString byteStr = ByteString.copyFrom(imageConversion.imageConvert(filename));
			Header.Builder hb = Header.newBuilder();
			hb.setNodeId(999);
			hb.setDestination(-1);
			hb.setTime(-1);

			CommandMessage.Builder rb = CommandMessage.newBuilder();
			rb.setHeader(hb);
			rb.setAction(routing.Pipe.Action.READ);
			ReadResponse.Builder read = ReadResponse.newBuilder();
			read.setReadData(ByteString.copyFromUtf8(filename));
			rb.setReadResponse(read);
			rb.setFilename(filename);
			rb.setTotalSize(filename.length());
			rb.setPort(5000);
		/*	Chunk.Builder chunk = Chunk.newBuilder();
			chunk.setChunkId(1);
			chunk.setChunkSize(byteStr.size());
			chunk.setNumberOfChunks(byteStr.size() / (30 * 1048576));
			rb.setChunk(chunk);*/
			rb.setHost(InetAddress.getLocalHost().getHostAddress());
			rb.setMessageId(121545);
			CommConnection.getInstance().enqueue(rb.build());

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	public void release() {
		CommConnection.getInstance().release();
	}

	/**
	 * Since the service/server is asychronous we need a unique ID to associate
	 * our requests with the server's reply
	 * 
	 * @return
	 */
	private synchronized long nextId() {
		return ++curID;
	}
}
