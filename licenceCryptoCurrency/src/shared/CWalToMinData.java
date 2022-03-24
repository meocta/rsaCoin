package shared;

import java.util.Vector;

import data.CCreditAddress;
import data.CTuple;

public class CWalToMinData 
{
	static private CWalToMinData fInstance = null;
	
	// miner to write in this list data that will be consumed by wallet
	// todo: replace with queue
	private Vector< CTuple > fConfirmedTransactions = null;
	private Vector< CCreditAddress > fCreditAddresses = null;	
	// wallet will check on this flag to know when a payment was confirmed
	private boolean fAddressConfirmed = false;
	// miner will check on this flag to know when to look for a specific transaction
	private boolean fAddressAdded = false;
	
	
	private CWalToMinData()
	{
		fConfirmedTransactions = new Vector<>();
		fCreditAddresses = new Vector<>();
	}
	
	static public CWalToMinData mGetInstance()
	{
		if( null == fInstance ){
			fInstance = new CWalToMinData();
		}		
		return fInstance;
	}
	
	//wallet worker thread to sleep on this until address confirmation arrives
	public synchronized void mIsAddressConfirmed()
	{
		if( ! fAddressConfirmed ){
			try{
				wait();
			}catch( InterruptedException e ){
				e.printStackTrace();
			}
		}
	}
	
	// miner will call this to know it has to check for the transaction
	public synchronized boolean mIsAddressAdded()
	{
		return fAddressAdded;
	}
	
	//miner ads the transaction to be used by wallet
	public synchronized void mAddConfirmedTransaction( CTuple cTx )
	{
		fConfirmedTransactions.addElement( cTx );
		fAddressConfirmed = true;
		notify();
	}
	
	public synchronized CTuple mGetConfirmedTransaction()
	{
		CTuple cTx = fConfirmedTransactions.firstElement();
		fConfirmedTransactions.removeElementAt( 0 );
		
		if( fConfirmedTransactions.isEmpty() ){
			fAddressConfirmed = false;
		}		
		return cTx;
	}
	
	//wallet ads the transaction it expects
	public synchronized void mAddAddress( CCreditAddress addr )
	{
		fCreditAddresses.addElement( addr );
		fAddressAdded = true;
	}
	
	public synchronized Vector< CCreditAddress > mGetAddressesList()
	{
		return fCreditAddresses;
	}
	
	public synchronized void mRemoveCreditAdressAtIndex( int index )
	{
		fCreditAddresses.remove( index );
		
		if( fCreditAddresses.isEmpty() ){
			fAddressAdded = false;
		}
	}	
}
