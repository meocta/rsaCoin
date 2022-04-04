package miner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import data.CBlock;
import data.CTuple;
import main.CConfiguration;

public class CBlockChain
{
	static private CBlockChain fInstance = null;	
	// CCreateBlock will wait on this until this condition is false
	// it will also stop the block creation when the flag is true
	private boolean fBlockArrived = false;
	private boolean fTransactionArrived = false;	
	private CMinerData fData = null;
	//last block in the blockchain
	private CBlock fCurrentBlock = null;
	private File fNoOfBlocks = null;
	
	private CBlockChain()
	{
		fData = CMinerData.mGetInstance();
	}
	
	static public CBlockChain mGetInstance()
	{
		if( null == fInstance ){
			fInstance = new CBlockChain();
		}		
		return fInstance;
	}
	
	public CBlock mGetCurrentBlock()
	{
		return fCurrentBlock;
	}
	
	public void mSetCurrentBlock(CBlock current)
	{
		fCurrentBlock = current;
		fData.mIncrementBlocksNumber();

		try( FileOutputStream fos   	= new FileOutputStream( fNoOfBlocks );
			 ObjectOutputStream oos 	= new ObjectOutputStream( fos ); )
		{
			oos.writeInt( fData.mGetBlocksNumber() );
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * write block to file.
	 * the name of the file is the height of the block in the blockchain.
	 * e.g.: height of 115 -> 115.block
	 */
	public synchronized void mAddBlock( CBlock block )
	{
		mSetCurrentBlock(block);
		
		try{ 
			File blockFile = new File( CConfiguration.blockchainFolder + fData.mGetBlocksNumber() + ".block" );			
			blockFile.createNewFile();
		
			try(	FileOutputStream fos = new FileOutputStream( blockFile );
					ObjectOutputStream oos = new ObjectOutputStream( fos ) )
			{				
				oos.writeObject( block );
			}catch(Exception e){
				e.printStackTrace();;
			}
		}catch( NullPointerException | IOException e ){
			e.printStackTrace();
		}
	}
	
	public CBlock mGetBlockAtIndex( int blockNumber )
	{
		if( blockNumber > fData.mGetBlocksNumber() || blockNumber <= 0 ){
			System.err.println( "block index out of range" );
		}else{
			try{
				File blockFile = new File( CConfiguration.blockchainFolder + blockNumber + ".block" );	
				
				if( blockFile.exists() ){
					try(	FileInputStream fis = new FileInputStream( blockFile );
							ObjectInputStream ois = new ObjectInputStream( fis ) )
					{
						return ( CBlock )ois.readObject();
					}catch(Exception e){
						e.printStackTrace();
					}
				}else{
					System.err.println( "there's no such block on the disc" );
				}
			}catch( NullPointerException e ){
				System.err.println( e.toString() );
			}
		}		
		return null;
	}
	
	/*
	 * called by CCreateBlock thread before starting to create a block
	 */
	public synchronized void mStartingBlockCreation()
	{
		while( true == fBlockArrived || false == fTransactionArrived ){
			try{
				wait();
			}catch( InterruptedException e ){
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * called by CCreateBlock thread before ending the block creation and sending it to peers
	 * if true, block is to be dropped, otherwise keep it
	 */
	public synchronized boolean mFinishingBlockCreation()
	{	
		return fBlockArrived;
	}
	
	/*
	 * called when the new block received from peers is verified
	 * or a new transaction arrives
	 */
	public synchronized void mAllowBlockCreation()
	{
		fBlockArrived = false;
		//wake up CCreateBlock thread
		notify();
	}
	
	/*
	 * called when a new block is received from peers
	 */
	public synchronized void mCancelBlockCreation()
	{
		fBlockArrived = true;
	}
	
	/*
	 * set upon arriving of a new transaction
	 */
	public synchronized void mNewTransactions()
	{
		fTransactionArrived = true;
		notify();
	}
	
	/*
	 * unset upon consuming all the transactions in the list
	 */
	public synchronized void mNoNewTransactions()
	{
		fTransactionArrived = false;
	}
}
