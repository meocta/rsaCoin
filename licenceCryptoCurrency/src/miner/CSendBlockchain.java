package miner;

import data.CBlock;
import shared.CMinToNetBlockchain;

class CSendBlockchain implements Runnable
{
	static private CSendBlockchain fInstance = null;
	
	private CBlockChain         fBC    = null;
	private CMinToNetBlockchain fMTNBC = null;
	private CMinerData			fData  = null;
	
	private CSendBlockchain()
	{
		fBC = CBlockChain.mGetInstance();
		fMTNBC = CMinToNetBlockchain.mGetInstance();
		fData  = CMinerData.mGetInstance();
	}
	
	static public CSendBlockchain mGetInstance()
	{
		if( fInstance ==null ){
			fInstance = new CSendBlockchain();
		}
		return fInstance;
	}
	
	@Override
	public void run()
	{
		while( true ){
			//wait for consumer to arrive
			fMTNBC.mProdWaitForCons();
			
			//todo: first block should be embedded
			//the blockchain can increase continually while we read blocks
			for( int i = 1; i <= fData.mGetBlocksNumber(); i++ ){
				CBlock block = fBC.mGetBlockAtIndex( i );
				fMTNBC.mWriteBlock( block );
			}
			fMTNBC.mUnsetConsumerActive();
		}		
	}

	public Thread mStartThread()
	{
		CSendBlockchain blockchain = mGetInstance();
		Thread thread = new Thread( blockchain, "SendBlockchain_thread" );		
		thread.start();
		System.out.println("SendBlockchain_thread created");
		return thread;
	}
}
