package miner;

import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import cryptography.CCryptoSunrsasign;
import data.CBlock;
import data.CTransaction;
import data.CTuple;
import main.CConfiguration;
import shared.CNetToMinBlocks;
import shared.CWalToMinData;

public class CCreateBlock implements Runnable
{
	static private CCreateBlock fInstance = null;

	private CBlockChain 	fBC   = null;
	private CMinerData      fData = null;
	private CNetToMinBlocks fNTMB = null;
	private CWalToMinData	fWTM = null;

	private CCreateBlock()
	{
		fBC   = CBlockChain.mGetInstance();
		fData = CMinerData.mGetInstance();
		fNTMB = CNetToMinBlocks.mGetInstance();
		fWTM  = CWalToMinData.mGetInstance();
	}

	static public CCreateBlock mGetInstance()
	{
		if( fInstance ==null ){
			fInstance = new CCreateBlock();
		}
		return fInstance;
	}

	private String mGetRandAlias()
	{
		Long randomizer = System.currentTimeMillis();
		String alias = randomizer.toString();		
		return alias;
	}

	// reward transaction is the first transaction in a block
	CTransaction mGetRewardTransaction( String alias )
	{
		float[] reward = { CConfiguration.rewardValue };
		byte[][] rewardKeyHases = { CCryptoSunrsasign.getKeyHashFromAlias( alias ) };		
		return new CTransaction( reward, null, null, null, rewardKeyHases, true );
	}

	private CBlock mCreateBlock( CTransaction rewardTx )
	{
		Vector< CTransaction > txList = fData.mGetUnboundTransactionsList();
		Vector< byte[] > txHashes = new Vector<>();
		CBlock block = null;

		if( txList.isEmpty() ){
			fBC.mNoNewTransactions();
		}else{
			// add reward tx hash in front of list
			txHashes.addElement( rewardTx.mGetTransactionDigest() );			
			for( CTransaction tx: txList ){
				txHashes.addElement( tx.mGetTransactionDigest() );
			}			
			try{
				// add the reward tx in the front of the list
				txList.insertElementAt( rewardTx, 0 );
				// this is the only place where a block is created
				block = new CBlock( fBC.mGetCurrentBlock().mGetHeaderHash(), txHashes, txList.toArray( new CTransaction[ txList.size() ] ) );
			}catch ( NoSuchAlgorithmException e){
				e.printStackTrace();
			}
		}		
		return block;
	}

	private void mAcceptThisBlock( CBlock block )
	{
		fNTMB.mAddBlock( block );
	}

	@Override
	public void run()
	{
		while( true ){
			fBC.mStartingBlockCreation();
			
			String alias = mGetRandAlias();			
			CTransaction rewardTx = mGetRewardTransaction( alias );		
			
			CBlock block = mCreateBlock( rewardTx );
			
			// if this node received a block while creating its own block, abandon the later
			if( fBC.mFinishingBlockCreation() && null != block )
			{
				mAcceptThisBlock( block );				
				// save reward in confirmed tx list
				int out = 0;
				CTuple ctx = new CTuple( rewardTx.mGetTransactionDigest(), out, alias, CConfiguration.rewardValue );
				fWTM.mAddConfirmedTransaction( ctx );
			}else{
				//Reject this Block
			}
		}
	}

	public Thread mStartThread()
	{
		Thread thread = new Thread( fInstance );		
		thread.start();		
		return thread;
	}
}
