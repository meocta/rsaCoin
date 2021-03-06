package data;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Vector;

import cryptography.CCryptoSunrsasign;
import cryptography.CHelper;
import miner.CMinerData;
import states.EBlockChainState;

public class CBlock extends CSerializableSuper 
{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;	
	private static final int  algSize = 512;	
	private static final int  hashTarget = 3; // represents the number of zeroes the block hash has to start with
	
	//todo: add block number in blockchain
	private		CBlockHeader	fBlockHeader		= null;
	private 	CTransaction[]	fTransactionList	= null;
	
	public CBlock( byte[] previousBlockHash, Vector< byte[] > transactionHashes, CTransaction[] transactionList )
	{
		fTransactionList = transactionList;
		fBlockHeader = new CBlockHeader( previousBlockHash, transactionHashes );	
		
		//search for a noonce so that the header hash meets the target
		while( true ){
			byte[] serializedHeader = CHelper.mGetByteFromSerial( fBlockHeader );
			try{
				byte[] headerHash = CCryptoSunrsasign.mMessageDigest( algSize, serializedHeader );
				boolean  checkTarget = false;
				for( int i = 0; i < hashTarget; i++){
					if( 0 == headerHash[ i ] ){
						checkTarget = true;
					} else {
						checkTarget = false;
						break;
					}
				}
				// for the header hash to meet the target, the first hashTarget positions need to be zero
				if( checkTarget == true ){
					System.out.println( "block header found" );
					break;
				} else {
					fBlockHeader.mIncrementNoonce();
				}				
			} catch( NoSuchAlgorithmException e ) {
				e.printStackTrace();
				break;
			} catch( Exception e ) {
				e.printStackTrace();;
			}
		}
	}
	
	/*
	 * verifies the number of transactions in the block, the creation time, the header hash validity and each transaction validity
	 */
	public boolean mVerifyBlock()
	{
		boolean status = false;
		if( fBlockHeader.mVerifyTransactionsNumber() &&
			mVerifyHeaderHash() &&
			mVerifyTransactions() )
		{
			status = true;
		}
		if(CMinerData.mGetInstance().mGetBCState() == EBlockChainState.eFull &&
			fBlockHeader.mVerifyTime() == false )
		{
			status = false;
		}
		return status;
	}
	
	/* private members */
	
	/*
	 * verifies transactions validity
	 */
	private boolean mVerifyTransactions()
	{
		//reward transaction is checked later
		for( int index = 1; index < fTransactionList.length; index++){
			//todo: add verification for hash list to match the transaction list
			if( ! fTransactionList[ index ].mVerifyTransaction() ){
				System.out.println( "transaction verification failed at: " + index );
				return false;
			}
		}
		return true;
	}
	
	/*
	 * verifies header's hash validity
	 */
	private boolean mVerifyHeaderHash() {
		byte[] serializedHeader = CHelper.mGetByteFromSerial( fBlockHeader );
		try {
			byte[] headerHash = CCryptoSunrsasign.mMessageDigest( algSize, serializedHeader );
			int checkTarget = 0;
			for( int i = 0; i < hashTarget; i++) {
				checkTarget += headerHash[i];
			}
			// for the header hash to meet the target, the first hashTarget positions need to be zero
			if( checkTarget == 0 ) {
				System.out.println( "block header ok" );
				return true;
			}
		} catch( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		}		
		return false;
	}
	
	public byte[] mGetHeaderHash() throws NoSuchAlgorithmException
	{
		byte[] serializedHeader = CHelper.mGetByteFromSerial( fBlockHeader );
		return CCryptoSunrsasign.mMessageDigest( algSize, serializedHeader);
	}
	
	public CTransaction mGetTransactionAtIndex( int index )
	{
		return fTransactionList[ index ];
	}
	
	public int mGetTransactionListSize()
	{
		return fTransactionList.length;
	}
	
	public boolean mEquals( CBlock block )
	{
		try {
			return Arrays.equals( block.mGetHeaderHash(), mGetHeaderHash() );		
		} catch( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean mIsChildOf( byte[] parentHash )
	{
		return Arrays.equals( parentHash, fBlockHeader.mGetParentHash() );
	}
}
