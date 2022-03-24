package cryptography;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CHelper
{
	static public byte[] mGetByteFromSerial( Serializable serialObj ){
		byte[] byteStream = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();		
		try( ObjectOutputStream oos = new ObjectOutputStream( baos ) ){
			oos.writeObject( serialObj );
			oos.flush();
			byteStream = baos.toByteArray();
		}
		catch( IOException e ){
			e.printStackTrace();
		}		
		return byteStream;
	}
	
	static public void mPrintByteArray( byte[] array ){
		for( int i = 0; i < array.length; i++ ){
			Byte b = array[ i ];
			System.out.print(  b.intValue()  + " " );
		}
		System.out.println("CHelper.mPrintByteArray()");
	}
}
