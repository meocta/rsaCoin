package shared;

import java.util.HashMap;
import java.util.Map;

import data.CNetworkObject;

/*
 * this class is used for exchanging transactions and blocks between CMiner and CNetwork
 */
public class CMinToNetObject
{
	static CMinToNetObject outObject = null;
	private CNetworkObject fNetObj = null;
	//allows all the output connections to copy the object
	private int fConsumerCount = 0;
	//synchronization map
	private Map< Thread, Boolean > fDataIsRead = null;
	
	/*
	 * if the object is a CBlock then fType must be eBlock
	 * else, if the object is a CTransaction, fType must be a eTransaction 
	 */
	private CMinToNetObject()
	{
		fDataIsRead = new HashMap<>();
	}
	
	static public CMinToNetObject mGetInstance()
	{
		if( null == outObject ){
			outObject = new CMinToNetObject();
		}
		return outObject;
	}
	
	public synchronized void mRegisterConsumerThread( Thread consumer )
	{
		fDataIsRead.put( consumer , true );
	}
	
	public synchronized void mStartReading()
	{
		if( true == fDataIsRead.get( Thread.currentThread() ) ){
			try{
				//wait until producer is filling in the data
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}
	}
	
	public CNetworkObject mGetNetObj()
	{
		return fNetObj;
	}
	
	public synchronized void mFinishReading()
	{
		fDataIsRead.replace( Thread.currentThread(), true );
		fConsumerCount--;
		if( 0 == fConsumerCount ){
			//weak up producer. no problem if consumers will be awaken as well, they will go back to sleep
			notifyAll();
		}
	}
	
	public synchronized void mWrite( CNetworkObject obj )
	{
		//wait until consumers have registered and read the data 
		if( ( 0 < fConsumerCount ) || ( 0 == fDataIsRead.size() ) ){
			try{
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}
		fNetObj = obj;		
		fConsumerCount = fDataIsRead.size();
		
		for( Map.Entry< Thread, Boolean > entry : fDataIsRead.entrySet() ){
			entry.setValue( false );
		}
		//wake up all consumers
		notifyAll();
	}
}
