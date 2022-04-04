package miner;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Vector;

import cryptography.CCryptoSunrsasign;
import data.CBlock;
import data.CCreditAddress;
import data.CNetworkObject;
import data.CTransaction;
import data.CTuple;
import main.CConfiguration;
import shared.CMinToNetObject;
import shared.CNetToMinBlocks;
import shared.CWalToMinData;
import states.EObjectType;

/*
 * processes incoming blocks from CNetToMinBlocks
 * 1. checks if the block is valid
 * 2. checks if it contains a transaction with an output to one of the addresses wallet is waiting
 * 3. updates the list of unspent transactions
 */
class CProcessBlocks implements Runnable
{
	static private CProcessBlocks fInstance = null;
	
	private CNetToMinBlocks fBlkList	= null;
	private CMinerData 		fData		= null;
	private CMinToNetObject fMNObject	= null;
	private CBlockChain 	fBlockChain	= null;
	private CWalToMinData	fWalToMin	= null;
	
	private CProcessBlocks()
	{
		fBlkList    = CNetToMinBlocks.mGetInstance();
		fData       = CMinerData.mGetInstance();
		fMNObject   = CMinToNetObject.mGetInstance();
		fBlockChain = CBlockChain.mGetInstance();
		fWalToMin	= CWalToMinData.mGetInstance();
		
		//todo: create list with unspent transactions
	}
	
	static public CProcessBlocks mGetInstance()
	{
		if( fInstance ==null ) {
			fInstance = new CProcessBlocks();
		}
		return fInstance;
	}
	
	private void mWriteBlockToDisc( CBlock block )
	{
		fBlockChain.mAddBlock( block );
	}
	
	private void mCheckForWalletAddress( CTransaction tx )
	{
		if( fWalToMin.mIsAddressAdded() ){
			Vector< CCreditAddress > addrList = fWalToMin.mGetAddressesList();
			
			for( CCreditAddress addr: addrList ){
				String alias = addr.mGetAlias();
				byte[] keyHash = CCryptoSunrsasign.getKeyHashFromAlias( alias );
				int out = tx.mKeyHashPayedValue( keyHash, addr.mGetValue() );
				
				if( 0 <= out ){
					int index = addrList.indexOf( addr );
					fWalToMin.mRemoveCreditAdressAtIndex( index );
					
					CTuple ctx = new CTuple( tx.mGetTransactionDigest(), out, alias, addr.mGetValue() );
					fWalToMin.mAddConfirmedTransaction( ctx );
				}
			}
		}			
	}
	
	/*
	 * verifies the first transaction in a block
	 */
	private boolean mVerifyRewardTransaction( CBlock block )
	{		
		CTransaction tx = block.mGetTransactionAtIndex( 0 );
		float txVal = tx.mGetTotalOutputValue();
		return ( 0 == Float.compare( CConfiguration.rewardValue, tx.mGetTotalOutputValue() ) );
	}
	
	/*
	 * verifies the transaction list within this block
	 * if all are ok, then remove them from unbound list 
	 *                and add their outputs to unspent
	 * else, reject the block
	 */	
	private boolean mVerifyTxList( CBlock block )
	{
		int nTxs = block.mGetTransactionListSize();
		Vector< Integer > unboundIndexes = new Vector<>();
		
		if( (false == mVerifyRewardTransaction( block )) ||
			(1 == nTxs && fData.mGetBlocksNumber()>0 ))
		{
			return false;
		}
		// check from the second transaction in the list
		for( int i = 1; i < nTxs; i++ ){
			CTransaction tx = block.mGetTransactionAtIndex( i );				
			int index = fData.mGetUnboundTxIndex( tx );
			//code is similar to CProcessTransactions
			if( -1 == index ){
				//transaction was not found in the unbound list
				//verify that tx construction is all right
				if( tx.mVerifyTransaction() ){
					//check for the inputs of this tx, they must be unspent
					Map< byte[], Integer > inputs = tx.mGetInputs();
					for( Map.Entry< byte[], Integer > entry : inputs.entrySet() ){
						if( false == fData.mSearchUnspentOutput( entry.getKey(), entry.getValue())){
							return false;
						}
					}					
					//transaction passed all verification tests, is good to keep
					for( Map.Entry< byte[], Integer > entry : inputs.entrySet() ){
						//remove all the entries in the list with unspent transactions
						if( false == fData.mRemoveUnspentOutput( entry.getKey(), entry.getValue() ) ){
							System.err.println( "tx not found in unspent list! this can't be!" );
							//break;
						}
					}
					//todo: might be a race condition with CProcesTransactions
					//add tx to unbound tx list, in case the block will be rejected
					Vector< CTransaction > txs = new Vector<>( 1 );
					txs.add( tx );
					fData.mAddTransactionList( txs );
					unboundIndexes.add( fData.mGetUnboundTxListSize() - 1 );
					//add tx's outputs to unspent tx list
					Vector< Integer > outputs = new Vector<>( tx.mGetOutputsNumber() );
					for( int j = 0; j < tx.mGetOutputsNumber(); j++ ){
						outputs.addElement( j );
					}
					fData.mAddUnspentTransaction( tx.mGetTransactionDigest(), outputs );
				} else {
					return false;
				}
			} else {
				//transaction found in unbound tx list, save index to delete the entry later
				unboundIndexes.add( index );				
			}
			// save the data to wallet if transaction matches to expected
			mCheckForWalletAddress( tx );
		}
		//remove txs from unbound list since they will be bound to this block
		for( int index: unboundIndexes ){
			fData.mRemoveUnboundTransaction( index );
		}		
		return true;
	}
	
	private boolean mVerifyBlock( CBlock block )
	{
		
		//check the internal structure of this block
		if( false == block.mVerifyBlock()) {
			return false;
		}
		CBlock currentBlock = fBlockChain.mGetCurrentBlock();
		if( null != currentBlock ) {
			//check for duplicates, if blocks are equal then this is a duplicate, do nothing with it further
			if( true == currentBlock.mEquals( block )) {
				return false;
			}
			//check if hash pointer points to the blockchain
			try{
				if( false == block.mIsChildOf( currentBlock.mGetHeaderHash())) {
					return false;
				}
			}catch( NoSuchAlgorithmException e ){
				e.printStackTrace();
				return false;
			}
		}
		//check the list of transactions if they comply with all the rules
		if( false == mVerifyTxList( block ) ) {
			return false;
		}		
		return true;
	}
	
	@Override
	public void run()
	{
		while( true ){			
			//thread waits here until data is produced in fTxList object
			Vector< CBlock > blkList = fBlkList.mGetBlocks();
			//cancel block creation that is in progress
			fBlockChain.mCancelBlockCreation();
			for( CBlock block : blkList ){
				if( true == mVerifyBlock( block ) ){
					mWriteBlockToDisc( block );
					//write block so CNetwork can read it and send it away
					CNetworkObject obj = new CNetworkObject( block, EObjectType.eBlock ); 
					fMNObject.mWrite( obj );
				}else{
					//reject block
				}
			}
			//allow block creation to start
			fBlockChain.mAllowBlockCreation();
		}
	}
	
	public Thread mStartThread()
	{
		CProcessBlocks procBlk = mGetInstance();
		Thread thread = new Thread( procBlk, "ProcessBlocks_thread" );		
		thread.start();
		System.out.println("ProcesBlocks_thread created");
		return thread;
	}
}
