package shared;

import java.util.Vector;

import data.CGenericArray;
import data.CTransaction;

/*
 * shared objects
 * fIncomingTx - is populated by the socket connected to the network, CIncomingConnection
 * 			   - its entries are consumed by the thread processing the transactions from CMiner node
 * 
 */
public class CNetToMinTransactions
{
	private CGenericArray< CTransaction > fTransactions = null;
	private boolean fTransactionAdded = false;
	static private CNetToMinTransactions fIncomingTx = null;
	
	private CNetToMinTransactions()
	{
		fTransactions = new CGenericArray< CTransaction >();
	}

	static public CNetToMinTransactions mGetInstance()
	{
		if( null == fIncomingTx ){
			fIncomingTx = new CNetToMinTransactions();
		}
		return fIncomingTx;
	}

	/*
	 * only the reader should block on this object
	 */
	synchronized public Vector< CTransaction > mGetTransactions()
	{
		while( false == fTransactionAdded ){
			try{
				wait();
			}catch( InterruptedException e ){
				e.printStackTrace();
			}
		}
		fTransactionAdded = false;
		return fTransactions.mGetArray();
	}
	
	synchronized public void mAddTransaction( CTransaction transaction )
	{
		fTransactions.mAddElement( transaction );
		fTransactionAdded = true;
		notify();
	}	
}
