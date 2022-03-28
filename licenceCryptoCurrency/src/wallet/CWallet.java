package wallet;

import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.Vector;

import cryptography.CCryptoSunrsasign;
import data.CCreditAddress;
import data.CTransaction;
import data.CTuple;
import shared.CNetToMinTransactions;
import shared.CWalToMinData;

public class CWallet implements Runnable
{
	static private CWallet walletSingletonInstance = null;
	
	private CNetToMinTransactions fNTMT   = null;
	private CWalToMinData 		  fWTMD	  = null;
	private CWalletData 		  fData   = null;
	private CWorkerWallet 		  fWorker = null;
	
	private CWallet()
	{
		fNTMT 	= CNetToMinTransactions.mGetInstance();
		fWTMD 	= CWalToMinData.mGetInstance();
		fData 	= CWalletData.mGetInstance();
		fWorker = CWorkerWallet.mGetInstance();
	}
	
	static public CWallet mGetInstance()
	{
		if( null == walletSingletonInstance ){
			walletSingletonInstance = new CWallet();
		}
		return walletSingletonInstance;
	}

	private CTransaction mCreateTransaction( String addr, float sum )
	{
		CTransaction tx = null;
		
		if( fData.mGetCredit() > sum ){
			Vector< CTuple > txList = fData.mGetSpendableTransactionsList( sum );
			int 	listSize 	= txList.size();
			float 	rest 		= 0;
			float 	totalValue 	= 0;
			
			String[] aliases  = new String[ listSize ];
			int[] 	 outputs  = new int[ listSize ];
			byte[][] txHashes = new byte[ listSize ][ CCryptoSunrsasign.transactionHashSize ];
			
			for( int index = 0; index < listSize; index++ )
			{
				CTuple spendableTx = txList.elementAt( index );
				totalValue += spendableTx.mGetValue();
				aliases[ index ] = spendableTx.mGetAlias();
				outputs[ index ] = spendableTx.mGetOutputNumber();
				txHashes[ index ] = spendableTx.mGetTxHash();
			}
			// residual value to be given back to the owner of this transaction
			rest = totalValue - sum;			
			float[] values = { sum, rest };			
			// only 2 outputs for now
			byte[][] keysHashes = new byte[ 2 ][ CCryptoSunrsasign.keyHashSize ];
			// first output pays to the other node
			keysHashes[ 0 ] = Base64.getDecoder().decode(addr.getBytes()) ;
			// second pays to self the rest 
			keysHashes[ 1 ] = txList.lastElement().mGetHashKey();	
			
			tx = new CTransaction(values, aliases, txHashes, outputs, keysHashes, false);	
		}else{
			System.out.println( " not enough credit for the payment: " + fData.mGetCredit() );
		}		
		return tx;
	}
	
	private void mPayToAddress( float sum, String addr)
	{
		CTransaction tx = mCreateTransaction( addr, sum );		
		// send to CTransactionInput, to be added into known transactions list, and sent to other nodes afterwards
		fNTMT.mAddTransaction( tx );
	}
	
	/*
	 * creates an address and prints it to console
	 * along with its alias, used for checking afterwards
	 */
	private void mAddressToBePayedTo( float sum, String alias )
	{
		byte[] keyHash = CCryptoSunrsasign.getKeyHashFromAlias( alias );
		// print the pub key to which the sum will be payed
		System.out.println( " following is the public key hash " );
		System.out.println( Base64.getEncoder().encodeToString( keyHash ) );
		// add address to list of expected payments so miner would check upon it
		CCreditAddress addr = new CCreditAddress( alias, sum );
		fWTMD.mAddAddress( addr );		
	}
	
	private void mWalletMenu()
	{
		Scanner inputScanner = new Scanner( System.in );
		
		boolean repeat = true;		
		while( repeat ){
			System.out.println( " command:" );
			String command = inputScanner.nextLine();
			float sum = 0;
			switch(command){
				case "credit":
					System.out.println( " total credit is : " + fData.mGetCredit() );
					break;
				case "pay":
					System.out.println( "write in one line the receiving address i.e. public key hash" );
					String address = inputScanner.nextLine();

					System.out.println( "write the sum payed to this address" );
					sum = inputScanner.nextFloat();
					
					mPayToAddress( sum, address );
					break;
				case "expect":
					System.out.println( "write the sum to be received" );
					sum = inputScanner.nextFloat();
					
					System.out.println( "write the alias associated with this address" );
					String alias = inputScanner.nextLine(); 
					
					System.out.println( "following is the address to receive the payment:" );
					mAddressToBePayedTo( sum, alias );
					break;
				case "exit":
					System.out.println( "exiting the application" );
					repeat = false;
					//todo: add clean up code
					// exit the JVM
					System.exit( 0 );
					break;
				case "help":
					System.out.println( " credit - display the available sum of currency \n" +
							" pay - make a payment to the address you will provide \n" +
							" expect - axpect a payment to an address provided to you \n" +
							" exit - exit the application \n" +
							" help - display the menu options " );
					break;
				default:					
					System.out.println( " \"help\" will list the menu " );					
			}
		}
		inputScanner.close();
	}

	@Override
	public void run()
	{
		// start CWorkerWallet
		fWorker.mStartThread();
		mWalletMenu();		
	}
}
