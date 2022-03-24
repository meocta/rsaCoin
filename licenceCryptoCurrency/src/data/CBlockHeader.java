package data;

import java.util.Vector;

public class CBlockHeader extends CSerializableSuper 
{
	private static final long serialVersionUID = CSerializableSuper.serialVersionUID;
	private static final int transactionsLimit = 100; // maximum number of transactions in a block
	
	private byte[] fPreviousBlockHash = null;
	@SuppressWarnings("unused")
	private long fNoonce;
	private long fTimestamp;
	private Vector< byte[] > fTransactionHashes;
	
	public CBlockHeader( byte[] previousBlockHash, Vector< byte[] > transactionHashes )
	{
		fPreviousBlockHash = previousBlockHash;
		fTransactionHashes = transactionHashes;
		
		fNoonce = 0;
		fTimestamp = System.currentTimeMillis() * 1000;
	}
	
	/* 
	 * verifies that timestamp is no larger with more than an hour than current time
	 * and no smaller than a day
	 */
	public boolean mVerifyTime()
	{
		long currentTime = System.currentTimeMillis() * 1000;
		if( ( fTimestamp < currentTime + 3600 ) && ( fTimestamp > currentTime - 86400 ) ) {
			return true;
		} else {
			System.out.println( "timestamp not in acceptable range: " + fTimestamp + ", current time: " + currentTime );
		}
		return false;
	}
	
	/*
	 * verifies if the number of transactions is less than transactionsLimit
	 */
	public boolean mVerifyTransactionsNumber()
	{
		if( fTransactionHashes.size() < transactionsLimit ){
			return true;
		} else {
			System.out.println( " number of allowed transactions excedeed: " + fTransactionHashes.size() );
		}
		return false;
	}
	
	/*
	 * increments the number to be used in the hash changes
	 */
	public void mIncrementNoonce()
	{
		++fNoonce;
	}
	
	public byte[] mGetParentHash()
	{
		return fPreviousBlockHash;
	}
}
