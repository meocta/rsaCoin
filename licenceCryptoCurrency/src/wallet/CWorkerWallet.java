package wallet;

import data.CTuple;
import shared.CWalToMinData;

/*
 * gets the input from miner and adds it to wallet data
 */
public class CWorkerWallet implements Runnable
{
	static private CWorkerWallet fInstance = null;
	private CWalToMinData fWTMD = null;
	private CWalletData   fData = null;
	
	private CWorkerWallet()
	{
		fWTMD = CWalToMinData.mGetInstance();
		fData = CWalletData.mGetInstance();
	}
	
	static public CWorkerWallet mGetInstance()
	{
		if( null == fInstance ) {
			fInstance = new CWorkerWallet();
		}		
		return fInstance;
	}
	
	public Thread mStartThread()
	{
		Thread worker = new Thread( this );		
		worker.start();		
		return worker;
	}

	@Override
	public void run()
	{
		while( true ) {
			// worker waits here for an address to be confirmed
			fWTMD.mIsAddressConfirmed();			
			CTuple cTx = fWTMD.mGetConfirmedTransaction();			
			fData.mAddConfirmedTransaction( cTx );			
			float newCredit = cTx.mGetValue() + fData.mGetCredit();
			fData.mSetCredit( newCredit );
		}
	}
}
