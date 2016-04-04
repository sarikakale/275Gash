package gash.router.server.filemanagement;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.protobuf.ByteString;

import routing.Pipe.CommandMessage;

public class FileChunkManagement{
	
	
	private byte[] fileBytes;
	
	public static ArrayList<ByteString> buffer;
	
	public static ArrayList<ByteString> getBuffer() {
		return buffer;
	}
	public static void setBuffer(ArrayList<ByteString> buffer) {
		FileChunkManagement.buffer = buffer;
	}
	public byte[] getFileBytes() {
		return fileBytes;
	}
	public void setFileBytes(ByteBuffer buffer) {
		this.fileBytes = fileBytes;
	}
	
	public void setByteBuffPos(int totSize){
		
		buffer=new ArrayList<>();
	}
	
	public void fileAdd(ByteString bs, CommandMessage msg){
		
		
		buffer.add(bs);
		
		
	}
	
	public void fileMerge(){
		int i=0;
		for(ByteString b:buffer){
		System.out.println("Buffer==="+b+" "+i++);
		}
	}
	

}
