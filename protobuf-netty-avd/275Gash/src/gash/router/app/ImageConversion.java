package gash.router.app;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*import org.apache.commons.codec.binary.Base64;*/

public class ImageConversion {

	public byte[] imageConvert(String filename) {

		File file = new File(filename);

		try {
			// Reading a Image file from file system
			FileInputStream imageInFile = new FileInputStream(file);
			long leng=file.length();
			System.out.println(leng);
			if(leng < 2147483647){
				System.out.println(leng);
			byte imageData[] = new byte[ (int) file.length()];
			imageInFile.read(imageData);
			imageInFile.close();
			return imageData;
			}else
				return null;
			
			// Converting Image byte array into Base64 String
		/*	String imageDataString = encodeImage(imageData);
			System.out.println(imageDataString);*/
			
			/*return imageDataString;*/
					
		} catch (FileNotFoundException e) {
			System.out.println("Image not found" + e);
		} catch (IOException ioe) {
			System.out.println("Exception while reading the Image " + ioe);
		}
		return null;
	}

	/**
	 * Encodes the byte array into base64 string
	 *
	 * @param imageByteArray
	 *            - byte array
	 * @return String a {@link java.lang.String}
	 */
	//public String encodeImage(byte[] imageByteArray) {
	//	return Base64.encodeBase64URLSafeString(imageByteArray);
	//}

	/**
	 * Decodes the base64 string into byte array
	 *
	 * @param imageDataString
	 *            - a {@link java.lang.String}
	 * @return byte array
	 */
	//public byte[] decodeImage(String imageDataString) {
	//	return Base64.decodeBase64(imageDataString);
	//}

/*	public void stringDecoder(String outputFile, String imageDataString) {
		// Converting a Base64 String into Image byte array
		try {
			byte[] imageByteArray = decodeImage(imageDataString);
			System.out.println(imageByteArray);
			// Write a image byte array into file system
			FileOutputStream imageOutFile = new FileOutputStream(outputFile);

			imageOutFile.write(imageByteArray);

			imageOutFile.close();

			System.out.println("Image Successfully Manipulated!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}*/
}
