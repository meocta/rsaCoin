package shared;

import java.util.Vector;

import data.CBlock;
import data.CGenericArray;

public class CNetToMinBlocks
{
	private CGenericArray< CBlock > fBlocks = null;	
	private boolean fBlockAdded = false;	
	//CMiner waits on this object. CNetwork write to it
	static private CNetToMinBlocks fIncomingBlk = null;
	
	//todo: synchronization needed for fast block transfers, where the processing power is too low 
	// producer consumer model
	private CNetToMinBlocks()
	{
		fBlocks = new CGenericArray< CBlock >();
	}

	static public CNetToMinBlocks mGetInstance()
	{
		if( null == fIncomingBlk ){
			fIncomingBlk = new CNetToMinBlocks();
		}
		return fIncomingBlk;
	}

	/*
	 * only the reader should block on this object
	 */
	synchronized public Vector< CBlock > mGetBlocks()
	{
		while( false == fBlockAdded ){
			try{
				wait();
			}catch( InterruptedException e ){
				e.printStackTrace();
			}
		}
		fBlockAdded = false;
		return fBlocks.mGetArray();
	}
		
	synchronized public void mAddBlock( CBlock block )
	{
		fBlocks.mAddElement( block );
		fBlockAdded = true;
		notify();
	}
	
}
