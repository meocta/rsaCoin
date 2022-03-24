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

	private CMinerData 			 fData           = null;
	
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
		
		fData = CMinerData.mGetInstance();		
		fData.mSetBlocksNumber( mGetBCSize() );
	}	
	
	static public CMiner mGetInstance()
	{
		if( fInstance ==null ){
			fInstance = new CMiner();
		}
		return fInstance;
	}
	//todo: move to CMinerData?
	private int mGetBCSize()
	{
		File bcFolder = new File( CConfiguration.blockchainFolder );
		//count the number of files within the folder. no other files than blocks are there
		int bcSize = bcFolder.listFiles().length;
		if( 0 < bcSize ){
			fData.mSetBCState( EBlockChainState.eHalf );
		}else{
			fData.mSetBCState( EBlockChainState.eEmpty );
		}
		return bcSize;
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
