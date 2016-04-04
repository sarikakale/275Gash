package gash.router.client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Source code to split a file in to chunks using java nio.
 * 
 * YYYY-MM-DD 2014-05-06 mshannon - created.
 */
public class Filechunk {
	public static void main(String[] args) throws IOException {
		final long CHUNKSIZE =  1048576; // 128 Megabytes file chunks
		final int BUFFERSZE = 2 * 1048576; // 256 Megabyte memory buffer for
											// reading source file

		long splitSize = CHUNKSIZE;
		int bufferSize = BUFFERSZE;

		// String source = args[0];
		String source = "F:/CMPE275/demo.txt";

		// String output = args[1];
		String output = "F:/CMPE275/demo2.txt";

		FileChannel sourceChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();

			ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
			
			long totalBytesRead = 0; // total bytes read from channel
			long totalBytesWritten = 0; // total bytes written to output
			System.out.println("File Size====="+sourceChannel.size());
			double numberOfChunks = Math.ceil(sourceChannel.size() / (double) splitSize);

			System.out.println(numberOfChunks + " " + " scs-----" + sourceChannel.size());
			int padSize = (int) Math.floor(Math.log10(numberOfChunks) + 1);
			String outputFileFormat = "%s.%0" + padSize + "d";

			FileChannel outputChannel = null; // output channel (split file) we
												// are currently writing
			long outputChunkNumber = 0; // the split file / chunk number
			long outputChunkBytesWritten = 0; // number of bytes written to
												// chunk so far

			try {
				for (int bytesRead = sourceChannel.read(buffer); bytesRead != -1; bytesRead = sourceChannel
						.read(buffer)) {
					totalBytesRead += bytesRead;

					System.out.println(String.format("Read %d bytes from channel; total bytes read %d/%d ", bytesRead,
							totalBytesRead, sourceChannel.size()));

					buffer.flip(); // convert the buffer from writing data to
									// buffer from disk to reading mode

					int bytesWrittenFromBuffer = 0; // number of bytes written
													// from buffer

					while (buffer.hasRemaining()) {
						if (outputChannel == null) {
							outputChunkNumber++;
							outputChunkBytesWritten = 0;
							// put enqueueing here
							String outputName = String.format(outputFileFormat, output, outputChunkNumber);
							System.out.println(String.format("Creating new output channel %s", outputName));
							outputChannel = new FileOutputStream(outputName).getChannel();
						}

						long chunkBytesFree = (splitSize - outputChunkBytesWritten); // maxmimum
																						// free
																						// space
																						// in
																						// chunk
						int bytesToWrite = (int) Math.min(buffer.remaining(), chunkBytesFree); // maximum
																								// bytes
																								// that
																								// should
																								// be
																								// read
																								// from
																								// current
																								// byte
																								// buffer

						System.out.println(String.format(
								"Byte buffer has %d remaining bytes; chunk has %d bytes free; writing up to %d bytes to chunk",
								buffer.remaining(), chunkBytesFree, bytesToWrite));

						buffer.limit(bytesWrittenFromBuffer + bytesToWrite); // set
																				// limit
																				// in
																				// buffer
																				// up
																				// to
																				// where
																				// bytes
																				// can
																				// be
																				// read

						System.out.println("Buffer: " + buffer);
						int bytesWritten = outputChannel.write(buffer);

						outputChunkBytesWritten += bytesWritten;
						bytesWrittenFromBuffer += bytesWritten;
						totalBytesWritten += bytesWritten;

						System.out.println(String.format(
								"Wrote %d to chunk; %d bytes written to chunk so far; %d bytes written from buffer so far; %d bytes written in total",
								bytesWritten, outputChunkBytesWritten, bytesWrittenFromBuffer, totalBytesWritten));

						buffer.limit(bytesRead); // reset limit

						if (totalBytesWritten == sourceChannel.size()) {
							System.out.println("Finished writing last chunk");

							closeChannel(outputChannel);
							outputChannel = null;

							break;
						} else if (outputChunkBytesWritten == splitSize) {
							System.out.println("Chunk at capacity; closing()");

							closeChannel(outputChannel);
							outputChannel = null;
						}
					}

					buffer.clear();
				}
			} finally {
				closeChannel(outputChannel);
			}
		} finally {
			closeChannel(sourceChannel);
		}

	}

	public ArrayList<byte[]> fileChunking(String fileName) throws IOException {
		/*
		 * long splitSize = 128 * 1048576; // 128 Megabytes file chunks int
		 * bufferSize = 256 * 1048576; // 256 Megabyte memory buffer for reading
		 * source file
		 */

		final long CHUNKSIZE = 1048576 ; // 128 Megabytes file chunks
		final int BUFFERSZE =  2 * 1048576; // 256 Megabyte memory buffer for
											// reading source file

		long splitSize = CHUNKSIZE;
		int bufferSize = BUFFERSZE;
		ByteBuffer[] buffers;
		// String source = args[0];
		String source = fileName;

		// String output = args[1];
		String output = "F:/CMPE275/demo2.txt";
		ArrayList<byte[]> byteArray;
		FileChannel sourceChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();

			ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

			long totalBytesRead = 0; // total bytes read from channel
			long totalBytesWritten = 0; // total bytes written to output

			double numberOfChunks = Math.ceil(sourceChannel.size() / (double) splitSize);
			int padSize = (int) Math.floor(Math.log10(numberOfChunks) + 1);
			String outputFileFormat = "%s.%0" + padSize + "d";

			// setting buffers length
			buffers = new ByteBuffer[(int) numberOfChunks];
			byteArray=new ArrayList<>((int)numberOfChunks);
			
			FileChannel outputChannel = null; // output channel (split file) we
												// are currently writing
			long outputChunkNumber = 0; // the split file / chunk number
			long outputChunkBytesWritten = 0; // number of bytes written to
												// chunk so far

			try {

				for (int bytesRead = sourceChannel.read(buffer); bytesRead != -1; 
						bytesRead = sourceChannel.read(buffer)) {
					totalBytesRead += bytesRead;

					System.out.println(String.format("Read %d bytes from channel; total bytes read %d/%d ", bytesRead,
							totalBytesRead, sourceChannel.size()));

					buffer.flip(); // convert the buffer from writing data to
									// buffer from disk to reading mode

					int bytesWrittenFromBuffer = 0; // number of bytes written
													// from buffer

					while (buffer.hasRemaining()) {
						if (outputChannel == null) {
							outputChunkNumber++;
							outputChunkBytesWritten = 0;
							// put enqueueing here
							String outputName = String.format(outputFileFormat, output, outputChunkNumber);
							System.out.println(String.format("Creating new output channel %s", outputName));
							outputChannel = new FileOutputStream(outputName).getChannel();
						}

						long chunkBytesFree = (splitSize - outputChunkBytesWritten); // maxmimum
																						// free
																						// space
																						// in
																						// chunk
						int bytesToWrite = (int) Math.min(buffer.remaining(), chunkBytesFree); // maximum
																								// bytes
																								// that
																								// should
																								// be
																								// read
																								// from
																								// current
																								// byte
																								// buffer

						System.out.println(String.format(
								"Byte buffer has %d remaining bytes; chunk has %d bytes free; writing up to %d bytes to chunk",
								buffer.remaining(), chunkBytesFree, bytesToWrite));

						buffer.limit(bytesWrittenFromBuffer + bytesToWrite); // set
																				// limit
																				// in
																				// buffer
																				// up
																				// to
																				// where
																				// bytes
																				// can
																				// be
																				// read

						System.out.println("Buffer: " + buffer);
						int bytesWritten = outputChannel.write(buffer);
						buffer.flip();
						byte b[]=new byte[buffer.remaining()];
						//buffer.get(b);
						buffer.get(b);
						buffers[(int) outputChunkNumber - 1] = buffer;
						
						byteArray.add(b);
						outputChunkBytesWritten += bytesWritten;
						bytesWrittenFromBuffer += bytesWritten;
						totalBytesWritten += bytesWritten;

						System.out.println(String.format(
								"Wrote %d to chunk; %d bytes written to chunk so far; %d bytes written from buffer so far; %d bytes written in total",
								bytesWritten, outputChunkBytesWritten, bytesWrittenFromBuffer, totalBytesWritten));

						buffer.limit(bytesRead); // reset limit

						if (totalBytesWritten == sourceChannel.size()) {
							System.out.println("Finished writing last chunk");

							closeChannel(outputChannel);
							outputChannel = null;

							break;

						} else if (outputChunkBytesWritten == splitSize) {
							System.out.println("Chunk at capacity; closing()");

							closeChannel(outputChannel);
							outputChannel = null;
						}
					}

					buffer.clear();
					//
					
				}

				return byteArray;	
			} finally {
				
				closeChannel(outputChannel);
			}
		} finally {
			closeChannel(sourceChannel);
		}
		

	}

	private static void closeChannel(FileChannel channel) {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignore) {

			}
		}
	}
}
