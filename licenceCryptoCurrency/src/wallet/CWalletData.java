package wallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import data.CTuple;
import main.CConfiguration;

public class CWalletData
{
	static private CWalletData walletData = null;	
	// CTuple - transaction hash, output number, alias, value. All representing one entry of the wallet total credit
	// list with all confirmed transactions of this node
	private Vector< CTuple > fTupleList = null;
	// total credit owned by current node
	private float fCredit		= -1;
	private File  fPersTuple	= null;
	private File  fPersCredit	= null;
	
	@SuppressWarnings("unchecked")
	private CWalletData()
	{	
		fPersTuple		= new File( CConfiguration.walletFolder, "tuple.pers" );			
		fPersCredit		= new File( CConfiguration.walletFolder, "credit.pers" );
		
		//creation of files to be done when first write to them
		if( ( 0L != fPersTuple.length() ) && ( 0L != fPersCredit.length() ) )
		{		
			try(	FileInputStream fisTuple   	= new FileInputStream( fPersTuple );
					FileInputStream fisCredit 	= new FileInputStream( fPersCredit );					
					ObjectInputStream oisTuple 	= new ObjectInputStream( fisTuple );
					ObjectInputStream oisCredit = new ObjectInputStream( fisCredit ); )
			{
				fTupleList 			= ( Vector<CTuple> )oisTuple.readObject();
				fCredit				= ( float )oisCredit.readFloat();
			} catch(Exception e) {
				e.printStackTrace();
			}		
		}
	}

	static public CWalletData mGetInstance()
	{
		if( null == walletData ) {
			walletData = new CWalletData();
		}		
		return walletData;
	}
	
	private void mUpdatePersSpendableList()
	{
		try( 	FileOutputStream fos = new FileOutputStream( fPersTuple );
				ObjectOutputStream oos = new ObjectOutputStream( fos ); )
		{
			oos.writeObject( fTupleList );			
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/* public members */
	public synchronized void mAddConfirmedTransaction( CTuple cTx )
	{
		if( null == fTupleList ) {
			fTupleList = new Vector<>();			
			try {
				fPersTuple.createNewFile();				
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}		
		fTupleList.addElement( cTx );
		mUpdatePersSpendableList();
	}
	
	/*
	 * returns the list of CTuples to form a transaction to pay sum to other peer
	 */
	public synchronized Vector< CTuple > mGetSpendableTransactionsList( float sum )
	{
		Vector< CTuple > spendableList = new Vector<>();
		for( CTuple tuple: fTupleList ){
			spendableList.addElement( tuple );
			fTupleList.remove( tuple );			
			float value = tuple.mGetValue() ;
			if( sum > value ){
				sum -= value;
			}else{
				break;
			}
		}		
		mUpdatePersSpendableList();
		mSetCredit( fCredit - sum );
		return spendableList;
	}
	
	// when setting the credit, update the persistence as well
	public synchronized void mSetCredit( float newCredit )
	{
		if( -1 == fCredit ){
			try{
				fPersCredit.createNewFile();
			}catch( IOException e ){
				e.printStackTrace();
			}
		}
		fCredit = newCredit;
		try( FileOutputStream fos = new FileOutputStream( fPersCredit );
			 ObjectOutputStream oos = new ObjectOutputStream( fos ); )
		{
			oos.writeFloat( fCredit );
		}catch( Exception e ){
			e.printStackTrace();
		}
	}
	
	public synchronized float mGetCredit()
	{
		return fCredit;
	}
}
