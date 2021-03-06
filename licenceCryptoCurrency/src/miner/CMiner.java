package miner;

import java.io.File;

import main.CConfiguration;
import states.EBlockChainState;

/*
 * blocks are to be stored as files within a folder called Blockchain in user's home folder
 * files are named as numbers, representing the height of the block in the blockchain
 */
public class CMiner implements Runnable
{
	static private CMiner fInstance = null;
	
	private CCreateBlock 		 fBlockCreator   = null;
	private CProcessBlocks 		 fBlockProcessor = null;
	private CProcessTransactions fTxProcessor    = null;
	private CSendBlockchain 	 fBlockSender    = null;

	private CMiner()
	{
		//create workers instances
		fBlockProcessor = CProcessBlocks.mGetInstance();
		fTxProcessor    = CProcessTransactions.mGetInstance();
		fBlockSender    = CSendBlockchain.mGetInstance();
		fBlockCreator   = CCreateBlock.mGetInstance();
	}	
	
	static public CMiner mGetInstance()
	{
		if( fInstance ==null ){
			fInstance = new CMiner();
		}
		return fInstance;
	}

	@Override
	public void run()
	{		
		//start workers
		fBlockProcessor.mStartThread();
		fTxProcessor.mStartThread();
		fBlockSender.mStartThread();
		fBlockCreator.mStartThread();		
	}
}
