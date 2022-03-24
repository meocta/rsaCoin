package miner;

import java.util.Map;
import java.util.Vector;

import data.CNetworkObject;
import data.CTransaction;
import shared.CMinToNetObject;
import shared.CNetToMinTransactions;
import states.EObjectType;

/*
 * processes the incoming transactions 
 * 1. validates the transactions
 * 2. updates the list of new transactions not in block
 */
class CProcessTransactions implements Runnable
{
	static private CProcessTransactions fInstance = null;
	
	private CMinToNetObject			fMNObject	= null;
	private CNetToMinTransactions	fTxList		= null;
	private CMinerData				fData		= null;
	private CBlockChain				fBC			= null;	
	
	private CProcessTransactions()
	{
		fMNObject = CMinToNetObject.mGetInstance();
		fTxList   = CNetToMinTransactions.mGetInstance();
		fData     = CMinerData.mGetInstance();
		fBC		  = CBlockChain.mGetInstance();	
	}
	
	static public CProcessTransactions mGetInstance()
	{
		if( fInstance ==null ){
			fInstance = new CProcessTransactions();
		}
		return fInstance;
	}
	
	/*
	 * if tx is in the unbound list or it is ill formed, do nothing further
	 * else, add it to the unbound list and send it to other peers
	 */
	private boolean mVerifyTransaction( CTransaction tx )
	{
		//if transaction is not in the unbound list
		if( -1 == fData.mGetUnboundTxIndex( tx ) ){
			//verify that tx construction is all right
			if( tx.mVerifyTransaction() ){
				//check for the inputs of this tx, they must be unspent
				Map< byte[], Integer > inputs = tx.mGetInputs();
				
				for( Map.Entry< byte[], Integer > entry : inputs.entrySet() ){
					if( false == fData.mSearchUnspentOutput( entry.getKey(), entry.getValue() )) {
						return false;
					}
				}
				//todo: add key -> hash key check
				//transaction passed all verification tests, is good to keep
				for( Map.Entry< byte[], Integer > entry : inputs.entrySet() ){
					//remove all the entries in the list with unspent transactions
					if( false == fData.mRemoveUnspentOutput( entry.getKey(), entry.getValue() )) {
						System.err.println( "transaction not found! can't get here!" );
					}
				}
			}else{
				return false;
			}			
		}else{
			return false;
		}		
		return true;
	}
	
	@Override
	public void run()
	{
		while( true ){			
			//thread waits here until data is produced in fTxList object
			Vector< CTransaction > txList = fTxList.mGetTransactions();
			
			for( CTransaction tx : txList ){
				if( true == mVerifyTransaction( tx ) ){
					//add tx to unbound tx list
					Vector< CTransaction > txs = new Vector<>( 1 );
					txs.add( tx );
					fData.mAddTransactionList( txs );
					fBC.mNewTransactions();
					//add tx's outputs to unspent tx list
					Vector< Integer > outputs = new Vector<>( tx.mGetOutputsNumber() );
					for( int i = 0; i < tx.mGetOutputsNumber(); i++ ){
						outputs.addElement( i );
					}
					// todo: add key hashes for the checkup
					fData.mAddUnspentTransaction( tx.mGetTransactionDigest(), outputs );	
					//send tx to other peers
					CNetworkObject netObj = new CNetworkObject( tx, EObjectType.eTransaction );
					fMNObject.mWrite( netObj );
				}
			}
		}
	}

	public Thread mStartThread()
	{
		CProcessTransactions procTxs = mGetInstance();
		Thread thread = new Thread( procTxs );
		thread.start();
		return thread;
	}
}
