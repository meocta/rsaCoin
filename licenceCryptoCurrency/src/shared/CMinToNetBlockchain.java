package shared;

import data.CBlock;

/*
 * used to transfer blocks from blockchain to another peer node
 * CMiner is producer and CNetwork consumer
 * 
 * Producer waits for a consumer to arrive using fConsumerActive
 * then the waiting is done on fProducerFinished
 */
public class CMinToNetBlockchain
{
	//the blockchain is sent block by block
	private CBlock fBlock = null;
	//set when the producer first writes
	private boolean fFull = false;
	//producer waits on this until a consumer arrives
	//a new consumer waits on this until the old one leaves
	private boolean fConsumerActive = false;
	static private CMinToNetBlockchain fInstance = null;
	
	private CMinToNetBlockchain()
	{		
	}
	
	static public CMinToNetBlockchain mGetInstance()
	{
		if( null == fInstance ){
			fInstance = new CMinToNetBlockchain();
		}
		return fInstance;
	}	
	
	//producer writes a null block to show there's no new block in the blockchain
	public synchronized void mWriteBlock( CBlock block )
	{
		if( true == fFull ){
			try{
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}		
		fFull = true;
		fBlock = block;
		notify();
	}
	
	public synchronized CBlock mReadBlock()
	{
		if( false == fFull ){
			try{
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}
		fFull = false;
		notify();
		return fBlock;
	}	
	
	//new consumers are waiting on this
	public synchronized void mSetConsumerActive()
	{
		if( true == fConsumerActive ){
			try{
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}
		fConsumerActive = true;
		notify();
	}

	//producer calls this when the transfer is done
	public synchronized void mUnsetConsumerActive()
	{
		fConsumerActive = false;
	}
	
	//consumer calls this when transfer is done, to wake up the other consumers
	public synchronized void mWakeUpConsumers()
	{
		notify();		
	}
	
	//producer waits for consumer
	public synchronized void mProdWaitForCons()
	{
		if( false == fConsumerActive ){
			try{
				wait();
			}catch( InterruptedException e ){
				System.err.println( e.toString() );
			}
		}
	}
}
